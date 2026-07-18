package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.UserRole;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private UserRole role;
}
