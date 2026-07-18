package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String pair;           // e.g. BTC/USDT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;        // BUY / SELL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;   // MARKET / LIMIT

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal amount;

    @Column(precision = 30, scale = 10)
    private BigDecimal price;      // null for MARKET orders

    @Column(precision = 30, scale = 10)
    private BigDecimal executedPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime executedAt;
}
