package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.OrderSide;
import com.crypto.crypto_wallet.entity.OrderType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TradeRequest {
    private String pair;        // e.g. BTC/USDT
    private OrderSide side;     // BUY / SELL
    private OrderType orderType; // MARKET / LIMIT
    private BigDecimal amount;
    private BigDecimal price;   // required for LIMIT orders
}
