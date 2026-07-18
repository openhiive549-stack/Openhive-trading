package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.dto.WalletResponse;
import com.crypto.crypto_wallet.service.UserService;
import com.crypto.crypto_wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getWallets(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(walletService.getUserWallets(userId)));
    }

    @GetMapping("/{coinSymbol}")
    public ResponseEntity<ApiResponse<WalletResponse>> getOrCreateWallet(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String coinSymbol) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(walletService.getOrCreateWallet(userId, coinSymbol)));
    }
}
