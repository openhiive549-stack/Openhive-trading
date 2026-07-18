package com.crypto.crypto_wallet.dto;

import lombok.Data;

@Data
public class DepositAddressRequest {
    private String coinSymbol;
    private String coinName;
    private String network;
    private String address;
    private Boolean active;
}
