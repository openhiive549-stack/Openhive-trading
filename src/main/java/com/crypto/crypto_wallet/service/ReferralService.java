package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.ReferralSettingsResponse;
import com.crypto.crypto_wallet.dto.UserResponse;
import java.math.BigDecimal;
import java.util.List;

public interface ReferralService {
    String getReferralCode(Long userId);
    List<UserResponse> getReferredUsers(Long userId);
    long getReferralCount(Long userId);

    // Referral settings (admin)
    ReferralSettingsResponse getSettings();
    ReferralSettingsResponse updateSettings(BigDecimal rewardAmount, String rewardCoinSymbol, boolean enabled);
}
