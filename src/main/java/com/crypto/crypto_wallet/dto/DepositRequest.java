package com.crypto.crypto_wallet.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {
    private String coinSymbol;
    private BigDecimal amount;
    private String txHash;   // on-chain transaction hash
}
