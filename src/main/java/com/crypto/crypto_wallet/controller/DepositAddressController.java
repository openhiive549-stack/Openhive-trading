package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.dto.DepositAddressResponse;
import com.crypto.crypto_wallet.entity.DepositAddress;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.DepositAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deposit-addresses")
@RequiredArgsConstructor
public class DepositAddressController {

    private final DepositAddressRepository depositAddressRepository;

    /** Public – any authenticated user can read deposit addresses */
    @GetMapping("/{coinSymbol}")
    public ResponseEntity<ApiResponse<DepositAddressResponse>> getAddress(
            @PathVariable String coinSymbol) {

        DepositAddress da = depositAddressRepository
                .findByCoinSymbolIgnoreCaseAndActiveTrue(coinSymbol)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active deposit address configured for " + coinSymbol.toUpperCase()));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(da)));
    }

    /** Public – list all active deposit addresses (for the coin selector) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepositAddressResponse>>> listAll() {
        List<DepositAddressResponse> list = depositAddressRepository
                .findAllByActiveTrueOrderByCoinSymbolAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /* ── private helper ── */
    private DepositAddressResponse toResponse(DepositAddress da) {
        return DepositAddressResponse.builder()
                .id(da.getId())
                .coinSymbol(da.getCoinSymbol())
                .coinName(da.getCoinName())
                .network(da.getNetwork())
                .address(da.getAddress())
                .active(da.isActive())
                .build();
    }
}
