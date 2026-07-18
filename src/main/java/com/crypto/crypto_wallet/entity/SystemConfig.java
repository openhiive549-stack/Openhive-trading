package com.crypto.crypto_wallet.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * General-purpose key/value config table.
 * Used to persist the current data-encryption toggle state (DATA_ENCRYPTED = true/false).
 */
@Entity
@Table(name = "system_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @Column(nullable = false, length = 100)
    private String configKey;

    @Column(nullable = false, length = 500)
    private String configValue;
}
