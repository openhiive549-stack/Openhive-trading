package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.TransactionStatus;
import com.crypto.crypto_wallet.entity.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private TransactionType type;
    private String coinSymbol;
    private BigDecimal amount;
    private String txHash;
    private String toAddress;
    private TransactionStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
