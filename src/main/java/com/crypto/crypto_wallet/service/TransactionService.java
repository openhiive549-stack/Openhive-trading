package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.DepositRequest;
import com.crypto.crypto_wallet.dto.TransactionResponse;
import com.crypto.crypto_wallet.dto.WithdrawRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TransactionService {
    TransactionResponse deposit(Long userId, DepositRequest request);
    TransactionResponse withdraw(Long userId, WithdrawRequest request);
    List<TransactionResponse> getTransactions(Long userId);

    // Admin: deposits
    TransactionResponse approveDeposit(Long txId);
    List<TransactionResponse> getPendingDeposits();
    TransactionResponse rejectDeposit(Long txId, String reason);

    // Admin: withdrawals
    List<TransactionResponse> getPendingWithdrawals();
    TransactionResponse approveWithdrawal(Long txId);
    TransactionResponse rejectWithdrawal(Long txId, String reason);

    // Withdrawal limit
    java.math.BigDecimal getDailyWithdrawalLimit(Long userId);
    java.math.BigDecimal getDailyWithdrawalRemaining(Long userId);

    // Referral Bonus
    java.math.BigDecimal getTotalReferralBonus(Long userId);

    @Transactional(readOnly = true)
    Map<String, BigDecimal> getBonusByCoin(Long userId);
}
