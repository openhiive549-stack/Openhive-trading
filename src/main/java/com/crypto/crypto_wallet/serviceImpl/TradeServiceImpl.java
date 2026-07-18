package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.TradeRequest;
import com.crypto.crypto_wallet.dto.TradeResponse;
import com.crypto.crypto_wallet.entity.*;
import com.crypto.crypto_wallet.exception.BadRequestException;
import com.crypto.crypto_wallet.exception.InsufficientBalanceException;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.TradeOrderRepository;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.repository.WalletRepository;
import com.crypto.crypto_wallet.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private final TradeOrderRepository tradeOrderRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TradeResponse placeOrder(Long userId, TradeRequest request) {
        if (request.getPair() == null || !request.getPair().contains("/")) {
            throw new BadRequestException("Invalid trading pair. Format: BASE/QUOTE (e.g. BTC/USDT)");
        }

        String[] parts   = request.getPair().trim().split("/");
        String baseCoin  = parts[0].trim().toUpperCase();
        String quoteCoin = parts[1].trim().toUpperCase();

        if (baseCoin.isEmpty() || quoteCoin.isEmpty()) {
            throw new BadRequestException("Invalid trading pair");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        if (request.getSide() == null || request.getOrderType() == null) {
            throw new BadRequestException("Side and order type are required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getOrderType() == OrderType.LIMIT) {
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Price is required for LIMIT orders");
            }
        } else if (request.getOrderType() == OrderType.MARKET) {
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Reference price is required for MARKET orders (instant settlement)");
            }
        }

        BigDecimal executionPrice = request.getPrice();
        BigDecimal quoteNotional = request.getAmount().multiply(executionPrice);

        // Deduct balance from the spending wallet
        if (request.getSide() == OrderSide.BUY) {
            Wallet quoteWallet = walletRepository.findByUserIdAndCoinSymbol(userId, quoteCoin)
                    .orElseThrow(() -> new InsufficientBalanceException("No " + quoteCoin + " wallet found"));

            if (quoteWallet.getBalance().compareTo(quoteNotional) < 0) {
                throw new InsufficientBalanceException("Insufficient " + quoteCoin + " balance");
            }
            quoteWallet.setBalance(quoteWallet.getBalance().subtract(quoteNotional));
            walletRepository.save(quoteWallet);

            Wallet baseWallet = walletRepository.findByUserIdAndCoinSymbol(userId, baseCoin)
                    .orElseGet(() -> {
                        Wallet w = Wallet.builder().user(user).coinSymbol(baseCoin).build();
                        return walletRepository.save(w);
                    });
            baseWallet.setBalance(baseWallet.getBalance().add(request.getAmount()));
            walletRepository.save(baseWallet);

        } else {
            Wallet baseWallet = walletRepository.findByUserIdAndCoinSymbol(userId, baseCoin)
                    .orElseThrow(() -> new InsufficientBalanceException("No " + baseCoin + " wallet found"));

            if (baseWallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient " + baseCoin + " balance");
            }
            baseWallet.setBalance(baseWallet.getBalance().subtract(request.getAmount()));
            walletRepository.save(baseWallet);

            Wallet quoteWallet = walletRepository.findByUserIdAndCoinSymbol(userId, quoteCoin)
                    .orElseGet(() -> {
                        Wallet w = Wallet.builder().user(user).coinSymbol(quoteCoin).build();
                        return walletRepository.save(w);
                    });
            quoteWallet.setBalance(quoteWallet.getBalance().add(quoteNotional));
            walletRepository.save(quoteWallet);
        }

        walletRepository.flush();

        BigDecimal storedLimitPrice = request.getOrderType() == OrderType.LIMIT ? request.getPrice() : null;

        TradeOrder order = TradeOrder.builder()
                .user(user)
                .pair(baseCoin + "/" + quoteCoin)
                .side(request.getSide())
                .orderType(request.getOrderType())
                .amount(request.getAmount())
                .price(storedLimitPrice)
                .executedPrice(executionPrice)
                .status(OrderStatus.FILLED)
                .executedAt(LocalDateTime.now())
                .build();

        tradeOrderRepository.save(order);
        return toResponse(order);
    }

    @Override
    public List<TradeResponse> getTradeHistory(Long userId) {
        return tradeOrderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TradeResponse cancelOrder(Long userId, Long orderId) {
        TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to this user");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only PENDING orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        tradeOrderRepository.save(order);
        return toResponse(order);
    }

    private TradeResponse toResponse(TradeOrder o) {
        return TradeResponse.builder()
                .id(o.getId())
                .pair(o.getPair())
                .side(o.getSide())
                .orderType(o.getOrderType())
                .amount(o.getAmount())
                .price(o.getPrice())
                .executedPrice(o.getExecutedPrice())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .executedAt(o.getExecutedAt())
                .build();
    }
}
