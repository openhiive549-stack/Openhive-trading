package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.UserRole;
import lombok.Data;

@Data
public class CreateUserRequest {
    private String fullName;
    private String email;
    private String password;
    private UserRole role;
}
