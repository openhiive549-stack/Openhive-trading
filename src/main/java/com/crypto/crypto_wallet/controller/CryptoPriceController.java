package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.service.CryptoPriceService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Backend proxy endpoint for live crypto market prices.
 *
 * All frontend pages call GET /api/crypto/prices instead of calling
 * CoinGecko directly, so:
 *  - Only this server hits CoinGecko (once per 5 min, shared across all users).
 *  - No more 429 rate-limit errors for end users.
 *  - HTTP Cache-Control header is set so browsers also cache for 60 s.
 */
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoPriceController {

    private final CryptoPriceService cryptoPriceService;

    /**
     * Returns the CoinGecko /coins/markets response (server-cached for 5 min).
     *
     * Permitted to authenticated users only (price data is not sensitive,
     * but the security config already restricts /api/** to authenticated sessions).
     */
    @GetMapping("/prices")
    public ResponseEntity<ApiResponse<JsonNode>> getPrices() {
        JsonNode data = cryptoPriceService.getMarketData();

        if (data == null) {
            return ResponseEntity.ok(ApiResponse.error("Price data temporarily unavailable"));
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).mustRevalidate())
                .body(ApiResponse.ok(data));
    }

    /**
     * Returns the CoinGecko /global stats (server-cached for 5 min).
     * Used by market.html for market cap, BTC dominance, active coins, etc.
     */
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<JsonNode>> getGlobal() {
        JsonNode data = cryptoPriceService.getGlobalData();

        if (data == null) {
            return ResponseEntity.ok(ApiResponse.error("Global stats temporarily unavailable"));
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).mustRevalidate())
                .body(ApiResponse.ok(data));
    }
}
