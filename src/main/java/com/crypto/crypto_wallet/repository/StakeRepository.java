package com.crypto.crypto_wallet.repository;

import com.crypto.crypto_wallet.entity.Stake;
import com.crypto.crypto_wallet.entity.StakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StakeRepository extends JpaRepository<Stake, Long> {
    List<Stake> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Stake> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, StakeStatus status);
}
