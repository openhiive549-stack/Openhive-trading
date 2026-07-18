package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.TradeRequest;
import com.crypto.crypto_wallet.dto.TradeResponse;
import java.util.List;

public interface TradeService {
    TradeResponse placeOrder(Long userId, TradeRequest request);
    List<TradeResponse> getTradeHistory(Long userId);
    TradeResponse cancelOrder(Long userId, Long orderId);
}
