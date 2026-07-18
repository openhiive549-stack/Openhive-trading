package com.crypto.crypto_wallet.repository;

import com.crypto.crypto_wallet.entity.DepositAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DepositAddressRepository extends JpaRepository<DepositAddress, Long> {
    Optional<DepositAddress> findByCoinSymbolIgnoreCaseAndActiveTrue(String coinSymbol);
    List<DepositAddress> findAllByActiveTrueOrderByCoinSymbolAsc();
    Optional<DepositAddress> findByCoinSymbolIgnoreCase(String coinSymbol);
}
