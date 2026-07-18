package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.OrderSide;
import com.crypto.crypto_wallet.entity.OrderStatus;
import com.crypto.crypto_wallet.entity.OrderType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TradeResponse {
    private Long id;
    private String pair;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal amount;
    private BigDecimal price;
    private BigDecimal executedPrice;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}
