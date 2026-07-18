package com.crypto.crypto_wallet.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
