package com.crypto.crypto_wallet.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
public class BinaryOptionResponse {
    private Long id;
    private String pair;
    private String direction;        // CALL | PUT
    private BigDecimal stake;
    private BigDecimal entryPrice;
    private BigDecimal exitPrice;
    private BigDecimal payoutRate;
    private BigDecimal payout;       // total returned on WIN
    private BigDecimal profit;       // net profit on WIN (payout - stake)
    private String status;           // PENDING | WON | LOST
    private Integer expirySeconds;
    private ZonedDateTime expiresAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime settledAt;
}
