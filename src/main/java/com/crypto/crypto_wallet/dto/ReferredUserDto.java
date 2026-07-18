package com.crypto.crypto_wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferredUserDto {
    private UserResponse user;
    private BigDecimal rewardEarned;
    private String rewardCoin;
}
