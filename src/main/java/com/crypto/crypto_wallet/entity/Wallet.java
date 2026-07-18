package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coinSymbol"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String coinSymbol;   // BTC, ETH, USDT …

    @Column(nullable = false, precision = 30, scale = 10)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    // Deposit address shown to user
    private String depositAddress;

    // Stores AES-encrypted balance string when system is in encrypted state
    @Column(length = 500)
    private String encryptedBalance;
}
