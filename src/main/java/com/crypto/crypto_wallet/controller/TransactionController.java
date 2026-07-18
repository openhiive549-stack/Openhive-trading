package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.*;
import com.crypto.crypto_wallet.service.TransactionService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DepositRequest request) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok("Deposit submitted, pending admin approval",
                transactionService.deposit(userId, request)));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody WithdrawRequest request) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal submitted, pending admin approval",
                transactionService.withdraw(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(transactionService.getTransactions(userId)));
    }

    /** Returns the user's daily withdrawal limit and remaining amount */
    @GetMapping("/withdrawal-limit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWithdrawalLimit(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        BigDecimal limit = transactionService.getDailyWithdrawalLimit(userId);
        BigDecimal remaining = transactionService.getDailyWithdrawalRemaining(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "dailyLimit", limit,
                "remaining", remaining,
                "used", limit.subtract(remaining)
        )));
    }

    /** Admin: approve a pending deposit → credits the wallet */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{txId}/approve")
    public ResponseEntity<ApiResponse<TransactionResponse>> approve(@PathVariable Long txId) {
        return ResponseEntity.ok(ApiResponse.ok("Deposit approved",
                transactionService.approveDeposit(txId)));
    }
}
