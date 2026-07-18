package com.crypto.crypto_wallet.config;

import com.crypto.crypto_wallet.entity.User;
import com.crypto.crypto_wallet.entity.UserRole;
import com.crypto.crypto_wallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .email("admin21@gmail.com")
                    .password(passwordEncoder.encode("admin@21"))
                    .fullName("Default Admin")
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            System.out.println("Default admin user created: admin21@gmail.com / admin@21");
        }
    }
}
