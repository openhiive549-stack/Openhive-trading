package com.crypto.crypto_wallet.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private UserResponse user;
    private BigDecimal totalPortfolioUsd;
    private List<WalletResponse> wallets;
    private List<TransactionResponse> recentTransactions;
    private List<TradeResponse> recentTrades;
}
