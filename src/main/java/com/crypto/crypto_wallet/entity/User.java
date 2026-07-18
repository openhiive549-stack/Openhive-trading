package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String referralCode;

    private String referredBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Builder.Default
    private String vipLevel = "VIP Level 1";

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Tracks how much has been withdrawn today for limit enforcement */
    @Builder.Default
    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal dailyWithdrawalUsed = BigDecimal.ZERO;

    /** Date when dailyWithdrawalUsed was last reset */
    private LocalDate withdrawalLimitResetDate;

    /**
     * When true, the user's binary option trades can win normally.
     * When false (default for all users), every binary option trade is forced to LOST.
     * Only an admin can set this to true per user.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean binaryOptionWinAllowed = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Wallet> wallets;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TradeOrder> tradeOrders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Stake> stakes;
}
