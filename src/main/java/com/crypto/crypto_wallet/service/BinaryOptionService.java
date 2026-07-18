package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.BinaryOptionRequest;
import com.crypto.crypto_wallet.dto.BinaryOptionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BinaryOptionService {
    BinaryOptionResponse placeOption(Long userId, BinaryOptionRequest request);
    BinaryOptionResponse settleOption(Long userId, Long optionId, BigDecimal exitPrice);
    List<BinaryOptionResponse> getHistory(Long userId);
}
