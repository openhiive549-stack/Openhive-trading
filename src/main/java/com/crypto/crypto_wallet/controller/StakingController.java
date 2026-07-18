package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.dto.StakeRequest;
import com.crypto.crypto_wallet.dto.StakeResponse;
import com.crypto.crypto_wallet.service.StakingService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staking")
@RequiredArgsConstructor
public class StakingController {

    private final StakingService stakingService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<StakeResponse>> stake(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StakeRequest request) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok("Staking successful",
                stakingService.stake(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StakeResponse>>> getMyStakes(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(stakingService.getMyStakes(userId)));
    }

    @DeleteMapping("/{stakeId}")
    public ResponseEntity<ApiResponse<StakeResponse>> cancelStake(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long stakeId) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok("Stake cancelled",
                stakingService.cancelStake(userId, stakeId)));
    }
}
