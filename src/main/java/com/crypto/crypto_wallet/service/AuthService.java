package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.LoginRequest;
import com.crypto.crypto_wallet.dto.RegisterRequest;
import com.crypto.crypto_wallet.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    UserResponse login(LoginRequest request);
    void logout();
    UserResponse getCurrentUser();
}
