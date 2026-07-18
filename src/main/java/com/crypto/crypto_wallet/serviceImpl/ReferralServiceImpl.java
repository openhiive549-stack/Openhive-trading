package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.ReferralSettingsResponse;
import com.crypto.crypto_wallet.dto.UserResponse;
import com.crypto.crypto_wallet.entity.ReferralSettings;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.ReferralSettingsRepository;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.service.ReferralService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final ReferralSettingsRepository referralSettingsRepository;

    @Override
    public String getReferralCode(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getReferralCode();
    }

    @Override
    public List<UserResponse> getReferredUsers(Long userId) {
        String referralCode = getReferralCode(userId);
        return userRepository.findAll().stream()
                .filter(u -> referralCode != null && referralCode.equals(u.getReferredBy()))
                .map(userService::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getReferralCount(Long userId) {
        return getReferredUsers(userId).size();
    }

    @Override
    public ReferralSettingsResponse getSettings() {
        ReferralSettings settings = referralSettingsRepository.findAll()
                .stream().findFirst()
                .orElseGet(() -> {
                    // Auto-create default settings if none exist
                    ReferralSettings def = ReferralSettings.builder().build();
                    return referralSettingsRepository.save(def);
                });
        return toResponse(settings);
    }

    @Override
    @Transactional
    public ReferralSettingsResponse updateSettings(BigDecimal rewardAmount, String rewardCoinSymbol, boolean enabled) {
        ReferralSettings settings = referralSettingsRepository.findAll()
                .stream().findFirst()
                .orElseGet(() -> referralSettingsRepository.save(ReferralSettings.builder().build()));

        settings.setRewardAmount(rewardAmount);
        settings.setRewardCoinSymbol(rewardCoinSymbol != null ? rewardCoinSymbol.toUpperCase() : "USDT");
        settings.setEnabled(enabled);
        settings.setUpdatedAt(LocalDateTime.now());
        return toResponse(referralSettingsRepository.save(settings));
    }

    private ReferralSettingsResponse toResponse(ReferralSettings s) {
        return ReferralSettingsResponse.builder()
                .id(s.getId())
                .rewardAmount(s.getRewardAmount())
                .rewardCoinSymbol(s.getRewardCoinSymbol())
                .enabled(s.isEnabled())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
