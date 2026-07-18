package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "binary_option_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BinaryOptionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String pair;              // e.g. BTC/USDT

    @Column(nullable = false)
    private String direction;         // CALL | PUT

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal stake;         // amount wagered in USDT

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal entryPrice;    // locked price at open

    @Column(precision = 30, scale = 10)
    private BigDecimal exitPrice;     // price at expiry

    @Column(nullable = false)
    private Integer expirySeconds;    // e.g. 60

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // absolute expiry timestamp

    @Column(nullable = false, precision = 6, scale = 4)
    @Builder.Default
    private BigDecimal payoutRate = new BigDecimal("0.85"); // 85% profit rate

    @Column(precision = 30, scale = 10)
    private BigDecimal payout;        // total returned on WIN (stake * 1.85)

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING | WON | LOST

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime settledAt;
}
