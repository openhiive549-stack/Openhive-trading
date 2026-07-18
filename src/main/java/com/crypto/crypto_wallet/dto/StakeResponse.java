package com.crypto.crypto_wallet.dto;

import com.crypto.crypto_wallet.entity.StakeStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class StakeResponse {
    private Long id;
    private String coinSymbol;
    private BigDecimal amount;
    private Double apr;
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private StakeStatus status;
}
