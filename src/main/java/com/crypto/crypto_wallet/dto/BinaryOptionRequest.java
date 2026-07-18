package com.crypto.crypto_wallet.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BinaryOptionRequest {
    private String pair;           // e.g. BTC/USDT
    private String direction;      // CALL | PUT
    private BigDecimal stake;      // USDT amount to wager
    private BigDecimal entryPrice; // current market price at trade open
    private Integer expirySeconds; // 30, 60, 120, 300, 900
    private BigDecimal payoutRate; // e.g. 0.85 for 85%
}
