package com.crypto.crypto_wallet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaMigrationFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Ensure the 'type' and 'status' columns in 'transactions' table are large enough
            jdbcTemplate.execute("ALTER TABLE transactions MODIFY COLUMN type VARCHAR(50) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE transactions MODIFY COLUMN status VARCHAR(50) NOT NULL");
            
            // Ensure enum columns in 'users' table are large enough
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN kyc_status VARCHAR(20) NOT NULL");
            
            System.out.println("Schema migration: columns modified to VARCHAR for safety.");
        } catch (Exception e) {
            System.err.println("Schema migration failed (might already be fixed): " + e.getMessage());
        }
    }
}
