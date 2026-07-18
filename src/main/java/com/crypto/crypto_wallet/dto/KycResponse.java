package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.DocumentType;
import com.crypto.crypto_wallet.entity.KycStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class KycResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String country;
    private String address;
    private DocumentType documentType;
    private String documentNumber;
    private KycStatus status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private String frontImagePath;
    private String backImagePath;
    private String selfieImagePath;
}
