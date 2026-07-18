package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.entity.SystemConfig;
import com.crypto.crypto_wallet.entity.User;
import com.crypto.crypto_wallet.entity.Wallet;
import com.crypto.crypto_wallet.repository.SystemConfigRepository;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterKeyService {

    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AesEncryptionService aesEncryptionService;

    private static final String ENCRYPTION_FLAG_KEY = "DATA_ENCRYPTED";

    public boolean isEncrypted() {
        return systemConfigRepository.findById(ENCRYPTION_FLAG_KEY)
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(false);
    }

    @Transactional
    public String toggleEncryption() {
        boolean currentEncrypted = isEncrypted();
        List<User> users = userRepository.findAll();
        List<Wallet> wallets = walletRepository.findAll();

        if (!currentEncrypted) {
            log.info("Starting database AES-256 encryption cycle on {} users and {} wallets...", users.size(), wallets.size());

            // 1. Encrypt Users
            for (User user : users) {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    user.setEmail(aesEncryptionService.encrypt(user.getEmail()));
                }
                if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                    user.setFullName(aesEncryptionService.encrypt(user.getFullName()));
                }
                if (user.getReferralCode() != null && !user.getReferralCode().isEmpty()) {
                    user.setReferralCode(aesEncryptionService.encrypt(user.getReferralCode()));
                }
                if (user.getReferredBy() != null && !user.getReferredBy().isEmpty()) {
                    user.setReferredBy(aesEncryptionService.encrypt(user.getReferredBy()));
                }
            }

            // 2. Encrypt Wallets
            for (Wallet wallet : wallets) {
                if (wallet.getDepositAddress() != null && !wallet.getDepositAddress().isEmpty()) {
                    wallet.setDepositAddress(aesEncryptionService.encrypt(wallet.getDepositAddress()));
                }
                // Store plaintext balance in encryptedBalance field, set balance to 0
                BigDecimal currentBalance = wallet.getBalance();
                if (currentBalance == null) {
                    currentBalance = BigDecimal.ZERO;
                }
                wallet.setEncryptedBalance(aesEncryptionService.encrypt(currentBalance.toPlainString()));
                wallet.setBalance(BigDecimal.ZERO);
            }

            // Save all changes
            userRepository.saveAll(users);
            walletRepository.saveAll(wallets);

            // Update configuration flag
            SystemConfig config = new SystemConfig(ENCRYPTION_FLAG_KEY, "true");
            systemConfigRepository.save(config);

            log.info("Database encryption successfully completed.");
            return "ENCRYPTED";
        } else {
            log.info("Starting database AES-256 decryption cycle on {} users and {} wallets...", users.size(), wallets.size());

            // 1. Decrypt Users
            for (User user : users) {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    user.setEmail(aesEncryptionService.decrypt(user.getEmail()));
                }
                if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                    user.setFullName(aesEncryptionService.decrypt(user.getFullName()));
                }
                if (user.getReferralCode() != null && !user.getReferralCode().isEmpty()) {
                    user.setReferralCode(aesEncryptionService.decrypt(user.getReferralCode()));
                }
                if (user.getReferredBy() != null && !user.getReferredBy().isEmpty()) {
                    user.setReferredBy(aesEncryptionService.decrypt(user.getReferredBy()));
                }
            }

            // 2. Decrypt Wallets
            for (Wallet wallet : wallets) {
                if (wallet.getDepositAddress() != null && !wallet.getDepositAddress().isEmpty()) {
                    wallet.setDepositAddress(aesEncryptionService.decrypt(wallet.getDepositAddress()));
                }
                if (wallet.getEncryptedBalance() != null && !wallet.getEncryptedBalance().isEmpty()) {
                    String decryptedBalStr = aesEncryptionService.decrypt(wallet.getEncryptedBalance());
                    wallet.setBalance(new BigDecimal(decryptedBalStr));
                    wallet.setEncryptedBalance(null);
                }
            }

            // Save all changes
            userRepository.saveAll(users);
            walletRepository.saveAll(wallets);

            // Update configuration flag
            SystemConfig config = new SystemConfig(ENCRYPTION_FLAG_KEY, "false");
            systemConfigRepository.save(config);

            log.info("Database decryption successfully completed.");
            return "DECRYPTED";
        }
    }
}
