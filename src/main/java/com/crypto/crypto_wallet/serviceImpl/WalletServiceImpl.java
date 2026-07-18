package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.WalletResponse;
import com.crypto.crypto_wallet.entity.User;
import com.crypto.crypto_wallet.entity.Wallet;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.repository.WalletRepository;
import com.crypto.crypto_wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Override
    public List<WalletResponse> getUserWallets(Long userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WalletResponse getOrCreateWallet(Long userId, String coinSymbol) {
        String sym = coinSymbol == null ? "" : coinSymbol.trim().toUpperCase();
        return walletRepository.findByUserIdAndCoinSymbol(userId, sym)
                .map(this::toResponse)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Wallet wallet = Wallet.builder()
                            .user(user)
                            .coinSymbol(sym)
                            .depositAddress(generateDepositAddress(sym))
                            .build();
                    walletRepository.save(wallet);
                    return toResponse(wallet);
                });
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .coinSymbol(wallet.getCoinSymbol())
                .balance(wallet.getBalance())
                .depositAddress(wallet.getDepositAddress())
                .build();
    }

    private String generateDepositAddress(String coinSymbol) {
        // Simulated address — replace with real on-chain generation in production
        String prefix = switch (coinSymbol.toUpperCase()) {
            case "BTC"  -> "1";
            case "ETH", "USDT", "BNB" -> "0x";
            case "SOL"  -> "";
            default     -> "";
        };
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 34);
    }
}
