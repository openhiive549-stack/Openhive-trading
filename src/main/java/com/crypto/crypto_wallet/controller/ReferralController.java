package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.dto.UserResponse;
import com.crypto.crypto_wallet.repository.TransactionRepository;
import com.crypto.crypto_wallet.service.ReferralService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/referral")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;
    private final UserService userService;
    private final com.crypto.crypto_wallet.service.TransactionService transactionService;

    private final TransactionRepository transactionRepository;

    @GetMapping("/code")
    public ResponseEntity<ApiResponse<String>> getReferralCode(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(referralService.getReferralCode(userId)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getReferredUsers(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(referralService.getReferredUsers(userId)));
    }

//    @GetMapping("/stats")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
//            @AuthenticationPrincipal UserDetails userDetails) {
//        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
//
//        List<UserResponse> referredUsers = referralService.getReferredUsers(userId);
//        long totalReferrals = referredUsers.size();
//
//        long activeReferrals = referredUsers.stream()
//                .filter(u -> u.getKycStatus() != null && "VERIFIED".equalsIgnoreCase(u.getKycStatus().name()))
//                .count();
//
//        com.crypto.crypto_wallet.dto.ReferralSettingsResponse settings = referralService.getSettings();
//        java.math.BigDecimal totalEarned = transactionService.getTotalReferralBonus(userId);
//        if (totalEarned == null) totalEarned = java.math.BigDecimal.ZERO;
//
//        String currentTier = "Bronze";
//        int commission = 10;
//        int nextTierReq = 10;
//        String nextTierName = "Silver";
//
//        if (activeReferrals >= 60) {
//            currentTier = "Platinum";
//            commission = 30;
//            nextTierReq = 0;
//            nextTierName = "None";
//        } else if (activeReferrals >= 30) {
//            currentTier = "Gold";
//            commission = 25;
//            nextTierReq = 60;
//            nextTierName = "Platinum";
//        } else if (activeReferrals >= 10) {
//            currentTier = "Silver";
//            commission = 20;
//            nextTierReq = 30;
//            nextTierName = "Gold";
//        }
//
//        java.math.BigDecimal spotFees = totalEarned.multiply(java.math.BigDecimal.valueOf(0.65)).setScale(2, java.math.RoundingMode.HALF_UP);
//        java.math.BigDecimal futuresFees = totalEarned.multiply(java.math.BigDecimal.valueOf(0.25)).setScale(2, java.math.RoundingMode.HALF_UP);
//        java.math.BigDecimal stakingBonus = totalEarned.multiply(java.math.BigDecimal.valueOf(0.07)).setScale(2, java.math.RoundingMode.HALF_UP);
//        java.math.BigDecimal conversionBonus = totalEarned.multiply(java.math.BigDecimal.valueOf(0.03)).setScale(2, java.math.RoundingMode.HALF_UP);
//
//        Map<String, Object> stats = new java.util.HashMap<>();
//        stats.put("referralCode", referralService.getReferralCode(userId));
//        stats.put("totalReferrals", totalReferrals);
//        stats.put("activeReferrals", activeReferrals);
//        stats.put("referredUsers", referredUsers);
//        stats.put("totalEarned", totalEarned.setScale(2, java.math.RoundingMode.HALF_UP));
//        stats.put("currentTier", currentTier);
//        stats.put("commissionRate", commission);
//        stats.put("nextTierReq", nextTierReq);
//        stats.put("nextTierName", nextTierName);
//        stats.put("spotFees", spotFees);
//        stats.put("futuresFees", futuresFees);
//        stats.put("stakingBonus", stakingBonus);
//        stats.put("conversionBonus", conversionBonus);
//
//        return ResponseEntity.ok(ApiResponse.ok(stats));
//    }


    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userService.getByEmail(userDetails.getUsername()).getId();

        // ── Referred users ────────────────────────────────────────────────────────
        List<UserResponse> referredUsers = referralService.getReferredUsers(userId);
        long totalReferrals = referredUsers.size();

        long activeReferrals = referredUsers.stream()
                .filter(u -> u.getKycStatus() != null
                        && "VERIFIED".equalsIgnoreCase(u.getKycStatus().name()))
                .count();

        // ── Settings & Total earned ───────────────────────────────────────────────
        com.crypto.crypto_wallet.dto.ReferralSettingsResponse settings = referralService.getSettings();
        java.math.BigDecimal totalEarned = transactionService.getTotalReferralBonus(userId);
        if (totalEarned == null) totalEarned = java.math.BigDecimal.ZERO;

        // ── Tier logic ────────────────────────────────────────────────────────────
        String currentTier = "Bronze";
        int commission = 10;
        int nextTierReq = 10;
        String nextTierName = "Silver";

        if (activeReferrals >= 60) {
            currentTier = "Platinum"; commission = 30; nextTierReq = 0; nextTierName = "None";
        } else if (activeReferrals >= 30) {
            currentTier = "Gold";     commission = 25; nextTierReq = 60; nextTierName = "Platinum";
        } else if (activeReferrals >= 10) {
            currentTier = "Silver";   commission = 20; nextTierReq = 30; nextTierName = "Gold";
        }

        // ── DYNAMIC: Per-user reward earned ──────────────────────────────────────
        // For each referred user, find their first completed deposit → reward = amount × 10%.
        // This matches exactly what creditReferralRewardIfApplicable credits.
        List<Map<String, Object>> referredUsersWithReward = referredUsers.stream()
                .map(u -> {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("id", u.getId());
                    row.put("fullName", u.getFullName());
                    row.put("email", u.getEmail());
                    row.put("kycStatus", u.getKycStatus());
                    row.put("referralCode", u.getReferralCode());
                    row.put("createdAt", u.getCreatedAt());

                    // Fetch the first completed deposit for this referred user
                    List<com.crypto.crypto_wallet.entity.Transaction> deposits =
                            transactionRepository.findCompletedDepositsByUserAsc(u.getId());

                    if (!deposits.isEmpty()) {
                        com.crypto.crypto_wallet.entity.Transaction firstDeposit = deposits.get(0);
                        java.math.BigDecimal reward = firstDeposit.getAmount()
                                .multiply(new java.math.BigDecimal("0.10"))
                                .setScale(8, java.math.RoundingMode.HALF_UP);
                        row.put("rewardAmount", reward);
                        row.put("rewardCoin", firstDeposit.getCoinSymbol());
                    } else {
                        row.put("rewardAmount", java.math.BigDecimal.ZERO);
                        row.put("rewardCoin", "—");
                    }
                    return row;
                })
                .collect(java.util.stream.Collectors.toList());

        // ── DYNAMIC: Earnings breakdown by coin ───────────────────────────────────
        // Real totals from REFERRAL_BONUS transactions grouped by coin symbol.
        Map<String, java.math.BigDecimal> bonusByCoin = transactionService.getBonusByCoin(userId);

        // Returns last 8 months (fills zeros for months with no bonus)
        List<Object[]> rawMonthly = transactionRepository.findMonthlyBonusTotals(userId);

// Build a map: "YYYY-MM" -> total
        java.util.Map<String, java.math.BigDecimal> monthMap = new java.util.LinkedHashMap<>();
        for (Object[] row : rawMonthly) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            java.math.BigDecimal total = (java.math.BigDecimal) row[2];
            monthMap.put(year + "-" + String.format("%02d", month), total);
        }

// Generate the last 8 months as labels, filling zero for missing months
        java.time.LocalDate now = java.time.LocalDate.now();
        java.util.List<java.util.Map<String, Object>> monthlyEarnings = new java.util.ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            java.time.LocalDate d = now.minusMonths(i);
            String key   = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            String label = d.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
            java.math.BigDecimal val = monthMap.getOrDefault(key, java.math.BigDecimal.ZERO);

            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("month", label);   // e.g. "Oct"
            entry.put("year",  d.getYear());
            entry.put("value", val.setScale(8, java.math.RoundingMode.HALF_UP));
            monthlyEarnings.add(entry);
        }


        // ── Build response ────────────────────────────────────────────────────────
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("referralCode",    referralService.getReferralCode(userId));
        stats.put("totalReferrals",  totalReferrals);
        stats.put("activeReferrals", activeReferrals);
        stats.put("referredUsers",   referredUsersWithReward);  // ← now includes rewardAmount + rewardCoin
        stats.put("totalEarned",     totalEarned.setScale(2, java.math.RoundingMode.HALF_UP));
        stats.put("currentTier",     currentTier);
        stats.put("commissionRate",  commission);
        stats.put("nextTierReq",     nextTierReq);
        stats.put("nextTierName",    nextTierName);
        stats.put("bonusByCoin",     bonusByCoin);
        stats.put("monthlyEarnings", monthlyEarnings);

        // Keep these for any other consumers that might use them,
        // but they are now derived from bonusByCoin on the frontend:
        stats.put("spotFees",        java.math.BigDecimal.ZERO);
        stats.put("futuresFees",     java.math.BigDecimal.ZERO);
        stats.put("stakingBonus",    java.math.BigDecimal.ZERO);
        stats.put("conversionBonus", java.math.BigDecimal.ZERO);

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<com.crypto.crypto_wallet.dto.ReferralSettingsResponse>> getRewardSettings() {
        return ResponseEntity.ok(ApiResponse.ok(referralService.getSettings()));
    }
}
