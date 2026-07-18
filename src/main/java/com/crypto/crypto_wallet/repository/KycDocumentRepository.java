package com.crypto.crypto_wallet.repository;

import com.crypto.crypto_wallet.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    Optional<KycDocument> findByUserId(Long userId);
}
