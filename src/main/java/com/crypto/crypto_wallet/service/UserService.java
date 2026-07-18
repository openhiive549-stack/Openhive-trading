package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.UserResponse;
import com.crypto.crypto_wallet.entity.User;

public interface UserService {
    UserResponse getUserById(Long id);
    UserResponse updateProfile(Long id, String fullName);
    User getByEmail(String email);
    UserResponse toResponse(User user);
    java.util.List<UserResponse> getAllUsers();
    UserResponse createUser(com.crypto.crypto_wallet.dto.CreateUserRequest request, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder);
    UserResponse updateUser(Long id, com.crypto.crypto_wallet.dto.UpdateUserRequest request);
    UserResponse toggleUserStatus(Long id);
    UserResponse toggleBinaryOptionWin(Long id);
    void changePassword(Long id, com.crypto.crypto_wallet.dto.ChangePasswordRequest request, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder);
}
