package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Singleton configuration row for referral reward settings.
 * Admin sets the reward amount/coin via the admin dashboard.
 * Only one row should exist (id = 1 by convention).
 */
@Entity
@Table(name = "referral_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reward amount credited to the referrer when a referred user's first deposit is approved */
    @Builder.Default
    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal rewardAmount = BigDecimal.ZERO;

    /** Coin in which the reward is paid (e.g. USDT) */
    @Builder.Default
    @Column(nullable = false)
    private String rewardCoinSymbol = "USDT";

    /** Whether referral rewards are currently active */
    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
