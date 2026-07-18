package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.DocumentType;
import lombok.Data;

@Data
public class KycRequest {
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String country;
    private String address;
    private DocumentType documentType;
    private String documentNumber;
}
