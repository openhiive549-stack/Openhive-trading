package com.crypto.crypto_wallet.repository;

import com.crypto.crypto_wallet.entity.Transaction;
import com.crypto.crypto_wallet.entity.TransactionStatus;
import com.crypto.crypto_wallet.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TransactionType type);
    List<Transaction> findByTypeAndStatus(TransactionType type, TransactionStatus status);
    List<Transaction> findAllByOrderByCreatedAtDesc();
    boolean existsByUserIdAndTypeAndStatus(Long userId, TransactionType type, TransactionStatus status);

    // NEW ─── bonus coins breakdown for referrer (for Earnings Breakdown section)
    @Query("SELECT t.coinSymbol, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = com.crypto.crypto_wallet.entity.TransactionType.REFERRAL_BONUS " +
            "AND t.status = com.crypto.crypto_wallet.entity.TransactionStatus.COMPLETED " +
            "GROUP BY t.coinSymbol " +
            "ORDER BY SUM(t.amount) DESC")
    List<Object[]> findBonusByCoinForUser(@Param("userId") Long userId);

    // NEW ─── first completed deposit for a specific user (to compute expected reward)
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = com.crypto.crypto_wallet.entity.TransactionType.DEPOSIT " +
            "AND t.status = com.crypto.crypto_wallet.entity.TransactionStatus.COMPLETED " +
            "ORDER BY t.createdAt ASC")
    List<Transaction> findCompletedDepositsByUserAsc(@Param("userId") Long userId);

    @Query("SELECT FUNCTION('YEAR', t.createdAt), FUNCTION('MONTH', t.createdAt), SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.type = com.crypto.crypto_wallet.entity.TransactionType.REFERRAL_BONUS " +
            "AND t.status = com.crypto.crypto_wallet.entity.TransactionStatus.COMPLETED " +
            "GROUP BY FUNCTION('YEAR', t.createdAt), FUNCTION('MONTH', t.createdAt) " +
            "ORDER BY FUNCTION('YEAR', t.createdAt) ASC, FUNCTION('MONTH', t.createdAt) ASC")
    List<Object[]> findMonthlyBonusTotals(@Param("userId") Long userId);




}
