package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. BTC, ETH, SOL */
    @Column(nullable = false, unique = true)
    private String coinSymbol;

    /** Human-readable coin name */
    private String coinName;

    /** The network/chain (e.g. ERC-20, TRC-20, BEP-20) */
    private String network;

    /** The actual wallet address admin has configured */
    @Column(nullable = false)
    private String address;

    /** Admin can disable an address without deleting it */
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
