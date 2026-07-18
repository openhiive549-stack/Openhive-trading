package com.crypto.crypto_wallet.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StakeRequest {
    private String poolId;      // matches STAKING_POOLS id from UI
    private String coinSymbol;
    private BigDecimal amount;
    private Integer durationDays;
    private Double apr;
}
