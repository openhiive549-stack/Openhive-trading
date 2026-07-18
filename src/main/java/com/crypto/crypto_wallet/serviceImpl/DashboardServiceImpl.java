package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.*;
import com.crypto.crypto_wallet.repository.TradeOrderRepository;
import com.crypto.crypto_wallet.repository.TransactionRepository;
import com.crypto.crypto_wallet.service.DashboardService;
import com.crypto.crypto_wallet.service.UserService;
import com.crypto.crypto_wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserService userService;
    private final WalletService walletService;
    private final TradeOrderRepository tradeOrderRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public DashboardResponse getDashboard(Long userId) {
        UserResponse user       = userService.getUserById(userId);
        List<WalletResponse> wallets = walletService.getUserWallets(userId);

        // The backend does not know live prices, so we set this to ZERO.
        // The frontend will calculate the real USD value using live CoinGecko prices.
        BigDecimal totalPortfolio = BigDecimal.ZERO;

        // Last 5 transactions
        List<TransactionResponse> recentTx = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream().limit(5)
                .map(tx -> TransactionResponse.builder()
                        .id(tx.getId())
                        .type(tx.getType())
                        .coinSymbol(tx.getCoinSymbol())
                        .amount(tx.getAmount())
                        .txHash(tx.getTxHash())
                        .toAddress(tx.getToAddress())
                        .status(tx.getStatus())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // Last 5 trades
        List<TradeResponse> recentTrades = tradeOrderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .stream()
                .map(o -> TradeResponse.builder()
                        .id(o.getId())
                        .pair(o.getPair())
                        .side(o.getSide())
                        .orderType(o.getOrderType())
                        .amount(o.getAmount())
                        .price(o.getPrice())
                        .executedPrice(o.getExecutedPrice())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .executedAt(o.getExecutedAt())
                        .build())
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .user(user)
                .totalPortfolioUsd(totalPortfolio)
                .wallets(wallets)
                .recentTransactions(recentTx)
                .recentTrades(recentTrades)
                .build();
    }
}
