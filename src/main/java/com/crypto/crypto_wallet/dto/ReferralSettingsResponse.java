package com.crypto.crypto_wallet.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReferralSettingsResponse {
    private Long id;
    private BigDecimal rewardAmount;
    private String rewardCoinSymbol;
    private boolean enabled;
    private LocalDateTime updatedAt;
}
