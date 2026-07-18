package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.KycStatus;
import com.crypto.crypto_wallet.entity.UserRole;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String referralCode;
    private UserRole role;
    private KycStatus kycStatus;
    private String vipLevel;
    private boolean enabled;
    private boolean binaryOptionWinAllowed;
    private LocalDateTime createdAt;
}
