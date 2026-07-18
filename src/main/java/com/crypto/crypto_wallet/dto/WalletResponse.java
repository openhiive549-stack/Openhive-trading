package com.crypto.crypto_wallet.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class WalletResponse {
    private Long id;
    private String coinSymbol;
    private BigDecimal balance;
    private String depositAddress;
}
