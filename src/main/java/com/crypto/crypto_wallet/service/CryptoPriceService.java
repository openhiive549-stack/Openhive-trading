package com.crypto.crypto_wallet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server-side proxy for CoinGecko market data.
 *
 * Strategy (production-ready, zero extra dependencies):
 *  - Caches the raw CoinGecko JSON array for CACHE_TTL_SECONDS (default 5 min).
 *  - All browser requests are served from cache → only 1 upstream call per TTL window,
 *    regardless of how many users/tabs are open.
 *  - On 429 / network error the previous cached value is returned (stale-while-revalidate).
 *  - A ReentrantLock prevents thundering-herd: only one thread fetches while others wait.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoPriceService {

    private static final String COINGECKO_URL =
            "https://api.coingecko.com/api/v3/coins/markets" +
            "?vs_currency=usd" +
            "&ids=bitcoin,ethereum,solana,binancecoin,ripple,cardano,avalanche-2,dogecoin,tether" +
            "&order=market_cap_desc" +
            "&per_page=50&page=1" +
            "&price_change_percentage=24h" +
            "&sparkline=true";

    private static final String COINGECKO_GLOBAL_URL =
            "https://api.coingecko.com/api/v3/global";

    /** How long (seconds) to keep cached data before fetching again. */
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

    /** Timeout for the upstream HTTP call. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(15);

    private final ObjectMapper objectMapper;

    // ── Cache state ──────────────────────────────────────────────────────────
    private final AtomicReference<JsonNode>  cachedData        = new AtomicReference<>(null);
    private final AtomicReference<Instant>   cachedAt          = new AtomicReference<>(Instant.EPOCH);
    private final ReentrantLock              fetchLock         = new ReentrantLock();

    private final AtomicReference<JsonNode>  cachedGlobal      = new AtomicReference<>(null);
    private final AtomicReference<Instant>   cachedGlobalAt    = new AtomicReference<>(Instant.EPOCH);
    private final ReentrantLock              fetchGlobalLock   = new ReentrantLock();

    // Reusable HTTP client (thread-safe, connection-pooling built-in)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the cached market data JSON array.
     * If the cache is stale it refreshes first (with lock); on upstream error
     * it falls back to stale data so the UI never shows empty prices.
     *
     * @return JsonNode (array) or null if no data has ever been fetched.
     */
    public JsonNode getMarketData() {
        if (isFresh()) {
            return cachedData.get();
        }

        // Only one thread calls upstream; others wait then use the fresh value.
        fetchLock.lock();
        try {
            // Double-check after acquiring lock (another thread may have just refreshed)
            if (isFresh()) {
                return cachedData.get();
            }
            return fetchAndCache();
        } finally {
            fetchLock.unlock();
        }
    }

    /**
     * Returns the CoinGecko /global response (server-cached for 5 min).
     */
    public JsonNode getGlobalData() {
        if (isGlobalFresh()) {
            return cachedGlobal.get();
        }
        fetchGlobalLock.lock();
        try {
            if (isGlobalFresh()) return cachedGlobal.get();
            return fetchAndCacheGlobal();
        } finally {
            fetchGlobalLock.unlock();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isFresh() {
        JsonNode data = cachedData.get();
        return data != null &&
               Duration.between(cachedAt.get(), Instant.now()).getSeconds() < CACHE_TTL_SECONDS;
    }

    private JsonNode fetchAndCache() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(COINGECKO_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    // Identify our server so CoinGecko can contact us if needed
                    .header("User-Agent", "NexusCrypto/1.0 (server-side-cache)")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                cachedData.set(node);
                cachedAt.set(Instant.now());
                log.debug("CoinGecko prices refreshed successfully ({} coins)", node.size());
                return node;
            } else {
                log.warn("CoinGecko returned HTTP {} – serving stale cache", response.statusCode());
                return cachedData.get(); // stale-while-revalidate
            }
        } catch (Exception e) {
            log.warn("CoinGecko fetch error: {} – serving stale cache", e.getMessage());
            return cachedData.get(); // stale-while-revalidate
        }
    }

    private boolean isGlobalFresh() {
        JsonNode data = cachedGlobal.get();
        return data != null &&
               Duration.between(cachedGlobalAt.get(), Instant.now()).getSeconds() < CACHE_TTL_SECONDS;
    }

    private JsonNode fetchAndCacheGlobal() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(COINGECKO_GLOBAL_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", "NexusCrypto/1.0 (server-side-cache)")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                cachedGlobal.set(node);
                cachedGlobalAt.set(Instant.now());
                log.debug("CoinGecko global stats refreshed successfully");
                return node;
            } else {
                log.warn("CoinGecko global returned HTTP {} – serving stale cache", response.statusCode());
                return cachedGlobal.get();
            }
        } catch (Exception e) {
            log.warn("CoinGecko global fetch error: {} – serving stale cache", e.getMessage());
            return cachedGlobal.get();
        }
    }
}
