package com.crypto.crypto_wallet.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepositAddressResponse {
    private Long id;
    private String coinSymbol;
    private String coinName;
    private String network;
    private String address;
    private boolean active;
}
