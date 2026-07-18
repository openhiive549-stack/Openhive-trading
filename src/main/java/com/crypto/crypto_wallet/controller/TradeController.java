package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.dto.TradeRequest;
import com.crypto.crypto_wallet.dto.TradeResponse;
import com.crypto.crypto_wallet.service.TradeService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradeResponse>> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TradeRequest request) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok("Order placed", tradeService.placeOrder(userId, request)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(tradeService.getTradeHistory(userId)));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<TradeResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", tradeService.cancelOrder(userId, orderId)));
    }
}
