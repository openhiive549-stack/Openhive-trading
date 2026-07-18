package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.StakeRequest;
import com.crypto.crypto_wallet.dto.StakeResponse;
import java.util.List;

public interface StakingService {
    StakeResponse stake(Long userId, StakeRequest request);
    List<StakeResponse> getMyStakes(Long userId);
    StakeResponse cancelStake(Long userId, Long stakeId);
}
