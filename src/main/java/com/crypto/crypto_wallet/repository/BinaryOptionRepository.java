package com.crypto.crypto_wallet.repository;

import com.crypto.crypto_wallet.entity.BinaryOptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BinaryOptionRepository extends JpaRepository<BinaryOptionOrder, Long> {
    List<BinaryOptionOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
