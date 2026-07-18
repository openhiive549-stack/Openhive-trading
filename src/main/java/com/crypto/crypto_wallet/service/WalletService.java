package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.WalletResponse;
import java.util.List;

public interface WalletService {
    List<WalletResponse> getUserWallets(Long userId);
    WalletResponse getOrCreateWallet(Long userId, String coinSymbol);
}
