package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.dto.BinaryOptionRequest;
import com.crypto.crypto_wallet.dto.BinaryOptionResponse;
import com.crypto.crypto_wallet.service.BinaryOptionService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/binary")
@RequiredArgsConstructor
public class BinaryOptionController {

    private final BinaryOptionService binaryOptionService;
    private final UserService userService;

    /**
     * Place a new binary option trade.
     * Stake is immediately deducted from the user's USDT wallet.
     */
    @PostMapping("/place")
    public ResponseEntity<ApiResponse<BinaryOptionResponse>> placeOption(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody BinaryOptionRequest request) {

        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        BinaryOptionResponse response = binaryOptionService.placeOption(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Binary option placed", response));
    }

    /**
     * Settle a binary option at expiry.
     * Called by the frontend when the countdown reaches zero with the current live price.
     * On WIN, the payout is credited to the user's USDT wallet.
     */
    @PostMapping("/settle/{optionId}")
    public ResponseEntity<ApiResponse<BinaryOptionResponse>> settleOption(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long optionId,
            @RequestParam BigDecimal exitPrice) {

        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        BinaryOptionResponse response = binaryOptionService.settleOption(userId, optionId, exitPrice);
        return ResponseEntity.ok(ApiResponse.ok("Option settled: " + response.getStatus(), response));
    }

    /**
     * Retrieve the authenticated user's binary option trade history.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BinaryOptionResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        List<BinaryOptionResponse> history = binaryOptionService.getHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
