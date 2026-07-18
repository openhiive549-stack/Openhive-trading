package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(Long userId);
}
