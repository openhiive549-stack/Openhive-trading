package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.DepositRequest;
import com.crypto.crypto_wallet.dto.TransactionResponse;
import com.crypto.crypto_wallet.dto.WithdrawRequest;
import com.crypto.crypto_wallet.entity.*;
import com.crypto.crypto_wallet.exception.InsufficientBalanceException;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.*;
import com.crypto.crypto_wallet.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ReferralSettingsRepository referralSettingsRepository;

    // ─────────────────────────────────────────
    // Withdrawal limits per KYC tier (USD equiv.)
    // ─────────────────────────────────────────
    private static final BigDecimal LIMIT_UNVERIFIED = new BigDecimal("500");
    private static final BigDecimal LIMIT_VERIFIED   = new BigDecimal("50000");

    // ═══════════════════════════════════════════════════════════════
    //  DEPOSIT
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public TransactionResponse deposit(Long userId, DepositRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Save the deposit as PENDING — balance is NOT credited until admin approves
        Transaction tx = Transaction.builder()
                .user(user)
                .type(TransactionType.DEPOSIT)
                .coinSymbol(request.getCoinSymbol().toUpperCase())
                .amount(request.getAmount())
                .txHash(request.getTxHash())
                .status(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(tx);
        return toResponse(tx);
    }

    /** Admin: approve a pending deposit → credit the wallet and mark COMPLETED */
    @Override
    @Transactional
    public TransactionResponse approveDeposit(Long txId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getType() != TransactionType.DEPOSIT)
            throw new IllegalStateException("Transaction is not a deposit");
        if (tx.getStatus() == TransactionStatus.COMPLETED)
            throw new IllegalStateException("Deposit already completed");

        // Credit the user's wallet
        User user = tx.getUser();
        Wallet wallet = walletRepository.findByUserIdAndCoinSymbol(user.getId(), tx.getCoinSymbol())
                .orElseGet(() -> {
                    Wallet w = Wallet.builder()
                            .user(user)
                            .coinSymbol(tx.getCoinSymbol())
                            .build();
                    return walletRepository.save(w);
                });
        wallet.setBalance(wallet.getBalance().add(tx.getAmount()));
        walletRepository.save(wallet);

        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);

        // ── Referral reward: credit referrer on the referred user's FIRST completed deposit ──
        creditReferralRewardIfApplicable(user, tx);

        return toResponse(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getPendingDeposits() {
        return transactionRepository
                .findByTypeAndStatus(TransactionType.DEPOSIT, TransactionStatus.PENDING)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionResponse rejectDeposit(Long txId, String reason) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getType() != TransactionType.DEPOSIT)
            throw new IllegalStateException("Transaction is not a deposit");
        if (tx.getStatus() != TransactionStatus.PENDING)
            throw new IllegalStateException("Deposit is not pending");

        tx.setStatus(TransactionStatus.FAILED);
        tx.setRejectionReason(reason);
        transactionRepository.save(tx);
        return toResponse(tx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  WITHDRAWAL
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public TransactionResponse withdraw(Long userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal amount = request.getAmount();

        // ── Daily limit check ──────────────────────────────────────
        BigDecimal limit = getLimit(user);
        resetDailyCounterIfNeeded(user);

        BigDecimal used = user.getDailyWithdrawalUsed() == null ? BigDecimal.ZERO : user.getDailyWithdrawalUsed();
        if (used.add(amount).compareTo(limit) > 0) {
            BigDecimal remaining = limit.subtract(used);
            throw new InsufficientBalanceException(
                    "Daily withdrawal limit exceeded. Your limit: " + limit +
                    " | Used: " + used + " | Remaining: " + (remaining.compareTo(BigDecimal.ZERO) < 0 ? "0" : remaining));
        }

        // ── Balance check ──────────────────────────────────────────
        Wallet wallet = walletRepository.findByUserIdAndCoinSymbol(userId, request.getCoinSymbol())
                .orElseThrow(() -> new InsufficientBalanceException(
                        "No " + request.getCoinSymbol() + " wallet found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        // ── Deduct balance immediately (funds held pending admin approval) ──
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // ── Update daily counter ───────────────────────────────────
        user.setDailyWithdrawalUsed(used.add(amount));
        user.setWithdrawalLimitResetDate(LocalDate.now());
        userRepository.save(user);

        // ── Create PENDING transaction ─────────────────────────────
        Transaction tx = Transaction.builder()
                .user(user)
                .type(TransactionType.WITHDRAWAL)
                .coinSymbol(request.getCoinSymbol().toUpperCase())
                .amount(amount)
                .toAddress(request.getToAddress())
                .status(TransactionStatus.PENDING)
                .build();

        transactionRepository.save(tx);
        return toResponse(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getPendingWithdrawals() {
        return transactionRepository
                .findByTypeAndStatus(TransactionType.WITHDRAWAL, TransactionStatus.PENDING)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Admin: approve withdrawal → mark COMPLETED (funds already deducted from wallet) */
    @Override
    @Transactional
    public TransactionResponse approveWithdrawal(Long txId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getType() != TransactionType.WITHDRAWAL)
            throw new IllegalStateException("Transaction is not a withdrawal");
        if (tx.getStatus() != TransactionStatus.PENDING)
            throw new IllegalStateException("Withdrawal is not pending");

        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);
        return toResponse(tx);
    }

    /** Admin: reject withdrawal → FAILED + refund balance + save reason */
    @Override
    @Transactional
    public TransactionResponse rejectWithdrawal(Long txId, String reason) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getType() != TransactionType.WITHDRAWAL)
            throw new IllegalStateException("Transaction is not a withdrawal");
        if (tx.getStatus() != TransactionStatus.PENDING)
            throw new IllegalStateException("Withdrawal is not pending");

        // ── Refund the held amount back to the wallet ──────────────
        User user = tx.getUser();
        Wallet wallet = walletRepository.findByUserIdAndCoinSymbol(user.getId(), tx.getCoinSymbol())
                .orElseGet(() -> {
                    Wallet w = Wallet.builder().user(user).coinSymbol(tx.getCoinSymbol()).build();
                    return walletRepository.save(w);
                });
        wallet.setBalance(wallet.getBalance().add(tx.getAmount()));
        walletRepository.save(wallet);

        // ── Also roll back the daily counter ──────────────────────
        if (user.getDailyWithdrawalUsed() != null) {
            BigDecimal newUsed = user.getDailyWithdrawalUsed().subtract(tx.getAmount());
            user.setDailyWithdrawalUsed(newUsed.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newUsed);
            userRepository.save(user);
        }

        tx.setStatus(TransactionStatus.FAILED);
        tx.setRejectionReason(reason);
        transactionRepository.save(tx);
        return toResponse(tx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  GENERAL
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getDailyWithdrawalLimit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return getLimit(user);
    }

    @Override
    public BigDecimal getDailyWithdrawalRemaining(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        resetDailyCounterIfNeeded(user);
        BigDecimal used = user.getDailyWithdrawalUsed() == null ? BigDecimal.ZERO : user.getDailyWithdrawalUsed();
        BigDecimal remaining = getLimit(user).subtract(used);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalReferralBonus(Long userId) {
        return transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, TransactionType.REFERRAL_BONUS)
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private BigDecimal getLimit(User user) {
        return user.getKycStatus() == KycStatus.VERIFIED ? LIMIT_VERIFIED : LIMIT_UNVERIFIED;
    }

    private void resetDailyCounterIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        if (user.getWithdrawalLimitResetDate() == null || !user.getWithdrawalLimitResetDate().equals(today)) {
            user.setDailyWithdrawalUsed(BigDecimal.ZERO);
            user.setWithdrawalLimitResetDate(today);
            userRepository.save(user);
        }
    }

    /**
     * If this is the user's first completed DEPOSIT and they were referred,
     * credit the referrer with the configured reward.
     */
    private void creditReferralRewardIfApplicable(User user, Transaction depositTx) {
        if (user.getReferredBy() == null || user.getReferredBy().isBlank()) return;

        // Check if user passed KYC
        if (user.getKycStatus() != KycStatus.VERIFIED) return;

        // Only fire on the very first completed deposit
        long completedDeposits = transactionRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), TransactionType.DEPOSIT)
                .stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        if (completedDeposits != 1) return; // already had prior deposits (this one is the first)

        // Find the referrer by referral code
        userRepository.findAll().stream()
                .filter(u -> user.getReferredBy().equals(u.getReferralCode()))
                .findFirst()
                .ifPresent(referrer -> {
                    // Calculate 10% of the deposit amount
                    BigDecimal reward = depositTx.getAmount().multiply(new BigDecimal("0.10"));
                    
                    Wallet rWallet = walletRepository
                            .findByUserIdAndCoinSymbol(referrer.getId(), depositTx.getCoinSymbol())
                            .orElseGet(() -> {
                                Wallet w = Wallet.builder()
                                        .user(referrer)
                                        .coinSymbol(depositTx.getCoinSymbol())
                                        .build();
                                return walletRepository.save(w);
                            });
                    rWallet.setBalance(rWallet.getBalance().add(reward));
                    walletRepository.save(rWallet);
                    
                    // Create transaction record for referrer
                    Transaction bonusTx = Transaction.builder()
                            .user(referrer)
                            .type(TransactionType.REFERRAL_BONUS)
                            .coinSymbol(depositTx.getCoinSymbol())
                            .amount(reward)
                            .status(TransactionStatus.COMPLETED)
                            .build();
                    transactionRepository.save(bonusTx);
                });
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .userId(tx.getUser() != null ? tx.getUser().getId() : null)
                .userEmail(tx.getUser() != null ? tx.getUser().getEmail() : null)
                .userFullName(tx.getUser() != null ? tx.getUser().getFullName() : null)
                .type(tx.getType())
                .coinSymbol(tx.getCoinSymbol())
                .amount(tx.getAmount())
                .txHash(tx.getTxHash())
                .toAddress(tx.getToAddress())
                .status(tx.getStatus())
                .rejectionReason(tx.getRejectionReason())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getBonusByCoin(Long userId) {
        List<Object[]> rows = transactionRepository.findBonusByCoinForUser(userId);
        Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            String coin = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            result.put(coin, total);
        }
        return result;
    }
}
