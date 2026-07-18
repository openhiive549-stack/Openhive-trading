package com.crypto.crypto_wallet.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String referralCode; // optional
}
