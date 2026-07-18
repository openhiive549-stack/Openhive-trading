package com.crypto.crypto_wallet.repository;

import com.crypto.crypto_wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId ORDER BY LOWER(w.coinSymbol)")
    List<Wallet> findByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND UPPER(TRIM(w.coinSymbol)) = UPPER(TRIM(:coinSymbol))")
    Optional<Wallet> findByUserIdAndCoinSymbol(@Param("userId") Long userId, @Param("coinSymbol") String coinSymbol);
}
