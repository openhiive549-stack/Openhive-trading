package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.*;
import com.crypto.crypto_wallet.entity.DepositAddress;
import com.crypto.crypto_wallet.entity.Wallet;
import com.crypto.crypto_wallet.entity.User;
import com.crypto.crypto_wallet.entity.Transaction;
import com.crypto.crypto_wallet.entity.TransactionType;
import com.crypto.crypto_wallet.entity.TransactionStatus;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.DepositAddressRepository;
import com.crypto.crypto_wallet.repository.WalletRepository;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.repository.TransactionRepository;
import com.crypto.crypto_wallet.service.CryptoPriceService;
import com.crypto.crypto_wallet.service.KycService;
import com.crypto.crypto_wallet.service.ReferralService;
import com.crypto.crypto_wallet.service.TransactionService;
import com.crypto.crypto_wallet.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final KycService kycService;
    private final PasswordEncoder passwordEncoder;
    private final ReferralService referralService;
    private final DepositAddressRepository depositAddressRepository;
    private final WalletRepository walletRepository;
    private final CryptoPriceService cryptoPriceService;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    // ─────────────────────────────────────────
    //  BALANCE ADJUSTMENT & BONUS
    // ─────────────────────────────────────────

    @PostMapping("/users/{id}/adjust-balance")
    public ResponseEntity<ApiResponse<Void>> adjustBalance(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {

        String coinSymbol = payload.get("coinSymbol").toString().toUpperCase().trim();
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String reason = payload.containsKey("reason") ? payload.get("reason").toString() : "Admin manual adjustment";

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserIdAndCoinSymbol(id, coinSymbol)
                .orElseGet(() -> Wallet.builder()
                        .user(user)
                        .coinSymbol(coinSymbol)
                        .balance(BigDecimal.ZERO)
                        .build());

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Record a transaction for audit trail and history
        Transaction tx = Transaction.builder()
                .user(user)
                .type(TransactionType.DEPOSIT)
                .coinSymbol(coinSymbol)
                .amount(amount)
                .txHash("MANUAL_CREDIT: " + reason)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        return ResponseEntity.ok(ApiResponse.ok("Balance adjusted successfully", null));
    }

    // ─────────────────────────────────────────
    //  USER MANAGEMENT
    // ─────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched all users", userService.getAllUsers()));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User created successfully", userService.createUser(request, passwordEncoder)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", userService.updateUser(id, request)));
    }

    /** Returns all wallets for a user with live USD values calculated from the server-side price cache. */
    @GetMapping("/users/{id}/wallets")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserWallets(@PathVariable Long id) {
        List<Wallet> wallets = walletRepository.findByUserId(id);
        JsonNode priceData = cryptoPriceService.getMarketData();

        // Build a quick sym -> price lookup map from cached CoinGecko data
        Map<String, Double> prices = new HashMap<>();
        if (priceData != null && priceData.isArray()) {
            priceData.forEach(coin -> {
                String sym = coin.has("symbol") ? coin.get("symbol").asText("").toUpperCase() : "";
                double price = coin.has("current_price") ? coin.get("current_price").asDouble(0) : 0;
                if (!sym.isEmpty()) prices.put(sym, price);
            });
        }

        List<Map<String, Object>> result = new ArrayList<>();
        double totalUsd = 0;
        for (Wallet w : wallets) {
            String sym = w.getCoinSymbol().trim().toUpperCase();
            double balance = w.getBalance().doubleValue();
            double priceUsd = prices.getOrDefault(sym, 0.0);
            double usdValue = balance * priceUsd;
            totalUsd += usdValue;

            Map<String, Object> row = new HashMap<>();
            row.put("coinSymbol", sym);
            row.put("balance", w.getBalance());
            row.put("priceUsd", priceUsd);
            row.put("usdValue", usdValue);
            result.add(row);
        }

        // Append a totals summary row
        Map<String, Object> summary = new HashMap<>();
        summary.put("coinSymbol", "TOTAL");
        summary.put("balance", null);
        summary.put("priceUsd", null);
        summary.put("usdValue", totalUsd);
        result.add(summary);

        return ResponseEntity.ok(ApiResponse.ok("User wallets fetched", result));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User status toggled", userService.toggleUserStatus(id)));
    }

    @PutMapping("/users/{id}/toggle-binary-win")
    public ResponseEntity<ApiResponse<UserResponse>> toggleBinaryOptionWin(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Binary option win flag toggled", userService.toggleBinaryOptionWin(id)));
    }

    // ─────────────────────────────────────────
    //  DEPOSIT MANAGEMENT
    // ─────────────────────────────────────────

    @GetMapping("/deposits")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getPendingDeposits() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched pending deposits", transactionService.getPendingDeposits()));
    }

    @PostMapping("/deposits/{id}/approve")
    public ResponseEntity<ApiResponse<TransactionResponse>> approveDeposit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Deposit approved", transactionService.approveDeposit(id)));
    }

    @PostMapping("/deposits/{id}/reject")
    public ResponseEntity<ApiResponse<TransactionResponse>> rejectDeposit(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String reason = payload.getOrDefault("reason", "");
        return ResponseEntity.ok(ApiResponse.ok("Deposit rejected", transactionService.rejectDeposit(id, reason)));
    }

    // ─────────────────────────────────────────
    //  WITHDRAWAL MANAGEMENT
    // ─────────────────────────────────────────

    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getPendingWithdrawals() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched pending withdrawals", transactionService.getPendingWithdrawals()));
    }

    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<ApiResponse<TransactionResponse>> approveWithdrawal(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal approved", transactionService.approveWithdrawal(id)));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<ApiResponse<TransactionResponse>> rejectWithdrawal(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String reason = payload.getOrDefault("reason", "");
        if (reason.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Rejection reason is required"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal rejected", transactionService.rejectWithdrawal(id, reason)));
    }

    // ─────────────────────────────────────────
    //  KYC MANAGEMENT
    // ─────────────────────────────────────────

    @GetMapping("/kyc")
    public ResponseEntity<ApiResponse<List<KycResponse>>> getPendingKyc() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched pending KYC documents", kycService.getPendingKyc()));
    }

    @PostMapping("/kyc/{id}/approve")
    public ResponseEntity<ApiResponse<KycResponse>> approveKyc(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("KYC approved", kycService.approveKyc(id)));
    }

    @PostMapping("/kyc/{id}/reject")
    public ResponseEntity<ApiResponse<KycResponse>> rejectKyc(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        return ResponseEntity.ok(ApiResponse.ok("KYC rejected", kycService.rejectKyc(id, reason)));
    }

    // ─────────────────────────────────────────
    //  WALLET ADDRESS MANAGEMENT
    // ─────────────────────────────────────────

    @GetMapping("/deposit-addresses")
    public ResponseEntity<ApiResponse<List<DepositAddressResponse>>> getAllDepositAddresses() {
        List<DepositAddressResponse> list = depositAddressRepository.findAll()
                .stream().map(this::toAddressResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Fetched all deposit addresses", list));
    }

    @PostMapping("/deposit-addresses")
    public ResponseEntity<ApiResponse<DepositAddressResponse>> upsertDepositAddress(
            @RequestBody DepositAddressRequest request) {
        // Upsert: find by coinSymbol or create new
        DepositAddress addr = depositAddressRepository.findAll().stream()
                .filter(a -> a.getCoinSymbol().equalsIgnoreCase(request.getCoinSymbol()))
                .findFirst()
                .orElse(DepositAddress.builder()
                        .coinSymbol(request.getCoinSymbol().toUpperCase())
                        .build());

        addr.setCoinName(request.getCoinName());
        addr.setNetwork(request.getNetwork());
        addr.setAddress(request.getAddress());
        if (request.getActive() != null) addr.setActive(request.getActive());
        addr.setUpdatedAt(LocalDateTime.now());
        DepositAddress saved = depositAddressRepository.save(addr);
        return ResponseEntity.ok(ApiResponse.ok("Deposit address saved", toAddressResponse(saved)));
    }

    @PutMapping("/deposit-addresses/{id}")
    public ResponseEntity<ApiResponse<DepositAddressResponse>> updateDepositAddress(
            @PathVariable Long id,
            @RequestBody DepositAddressRequest request) {
        DepositAddress addr = depositAddressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit address not found"));
        if (request.getCoinName() != null) addr.setCoinName(request.getCoinName());
        if (request.getNetwork() != null) addr.setNetwork(request.getNetwork());
        if (request.getAddress() != null) addr.setAddress(request.getAddress());
        if (request.getActive() != null) addr.setActive(request.getActive());
        addr.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.ok("Deposit address updated", toAddressResponse(depositAddressRepository.save(addr))));
    }

    @DeleteMapping("/deposit-addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepositAddress(@PathVariable Long id) {
        DepositAddress addr = depositAddressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit address not found"));
        addr.setActive(false);
        addr.setUpdatedAt(LocalDateTime.now());
        depositAddressRepository.save(addr);
        return ResponseEntity.ok(ApiResponse.ok("Deposit address deactivated", null));
    }

    // ─────────────────────────────────────────
    //  REFERRAL SETTINGS
    // ─────────────────────────────────────────

    @GetMapping("/referral-settings")
    public ResponseEntity<ApiResponse<ReferralSettingsResponse>> getReferralSettings() {
        return ResponseEntity.ok(ApiResponse.ok("Referral settings", referralService.getSettings()));
    }

    @PostMapping("/referral-settings")
    public ResponseEntity<ApiResponse<ReferralSettingsResponse>> updateReferralSettings(
            @RequestBody Map<String, Object> payload) {
        BigDecimal amount = new BigDecimal(payload.getOrDefault("rewardAmount", "0").toString());
        String coin = payload.getOrDefault("rewardCoinSymbol", "USDT").toString();
        boolean enabled = Boolean.parseBoolean(payload.getOrDefault("enabled", "true").toString());
        return ResponseEntity.ok(ApiResponse.ok("Referral settings updated",
                referralService.updateSettings(amount, coin, enabled)));
    }

    // ─────────────────────────────────────────
    //  HELPER
    // ─────────────────────────────────────────

    private DepositAddressResponse toAddressResponse(DepositAddress da) {
        return DepositAddressResponse.builder()
                .id(da.getId())
                .coinSymbol(da.getCoinSymbol())
                .coinName(da.getCoinName())
                .network(da.getNetwork())
                .address(da.getAddress())
                .active(da.isActive())
                .build();
    }
}
