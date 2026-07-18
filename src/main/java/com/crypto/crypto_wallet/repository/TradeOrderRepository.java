package com.crypto.crypto_wallet.repository;

import com.crypto.crypto_wallet.entity.TradeOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<TradeOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
