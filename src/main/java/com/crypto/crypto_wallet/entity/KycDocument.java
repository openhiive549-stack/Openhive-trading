package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String country;
    private String address;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;   // PASSPORT / ID_CARD / DRIVERS_LICENSE

    private String documentNumber;

    // File paths (stored server-side)
    private String frontImagePath;
    private String backImagePath;
    private String selfieImagePath;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KycStatus status = KycStatus.SUBMITTED;

    private String rejectionReason;

    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;
}
