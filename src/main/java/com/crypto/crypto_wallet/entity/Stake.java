package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stakes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String coinSymbol;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal amount;

    @Column(nullable = false)
    private Double apr;

    @Column(nullable = false)
    private Integer durationDays;

    @Builder.Default
    private LocalDate startDate = LocalDate.now();

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StakeStatus status = StakeStatus.ACTIVE;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
