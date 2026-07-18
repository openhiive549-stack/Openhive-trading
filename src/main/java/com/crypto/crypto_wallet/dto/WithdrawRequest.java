package com.crypto.crypto_wallet.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawRequest {
    private String coinSymbol;
    private BigDecimal amount;
    private String toAddress;
}
