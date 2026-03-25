package io.github.hyperliquid.sdk.examples.info;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.*;

import java.util.List;
import java.util.Map;

/**
 * Info API Overview Example: Demonstrates how to query market data and user
 * state.
 * <p>
 * This example focuses on several commonly used Info methods:
 * <ul>
 * <li>allMids(): get mid prices of all coins</li>
 * <li>meta(): load perpetual market metadata</li>
 * <li>l2Book(): get L2 order book snapshot</li>
 * <li>candleSnapshotByCount(): get recent candles</li>
 * <li>userState() and userRateLimit(): query basic account information</li>
 * </ul>
 * All calls use HTTP only and do not require private keys.
 * </p>
 */
public class InfoOverviewExample {

    /**
     * Entry point for Info API examples.
     * <p>
     * Note:
     * <ul>
     * <li>Public market data (allMids, meta, l2Book, candle) does NOT require any
     * key.</li>
     * <li>User endpoints (userState, userRateLimit) only require a wallet address
     * string,
     * which can be passed via the PRIMARY_WALLET_ADDRESS environment variable.</li>
     * </ul>
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        // ==================== 1. Initialize client and Info on testnet
        // ====================
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl() // Use testnet API endpoint
                .build();

        Info info = client.getInfo();

        System.out.println("=== Hyperliquid Info API Overview Example ===\n");

        // ==================== 2. Query mid prices for all coins ====================
        System.out.println("--- 1) allMids(): global mid prices ---");
        Map<String, String> mids = info.allMids();
        if (mids != null && !mids.isEmpty()) {
            int count = 0;
            for (Map.Entry<String, String> entry : mids.entrySet()) {
                System.out.printf("Mid %s: %s%n", entry.getKey(), entry.getValue());
                if (++count >= 3) {
                    break;
                }
            }
        }

        // ==================== 3. Query perpetual market metadata ====================
        System.out.println("\n--- 2) meta(): perpetual market metadata ---");
        Meta meta = info.meta();
        if (meta != null && meta.getUniverse() != null && !meta.getUniverse().isEmpty()) {
            Meta.Universe first = meta.getUniverse().getFirst();
            System.out.println("First perp market:");
            System.out.println("  Name         : " + first.getName());
            System.out.println("  Max leverage : " + first.getMaxLeverage());
            System.out.println("  Size decimals: " + first.getSzDecimals());
        }

        // ==================== 4. Query L2 order book snapshot for ETH
        // ====================
        System.out.println("\n--- 3) l2Book(\"ETH\"): L2 order book snapshot ---");
        L2Book book = info.l2Book("ETH");
        if (book != null && book.getLevels() != null && book.getLevels().size() >= 2) {
            List<L2Book.Levels> bids = book.getLevels().get(0);
            List<L2Book.Levels> asks = book.getLevels().get(1);
            if (bids != null && !bids.isEmpty() && asks != null && !asks.isEmpty()) {
                L2Book.Levels bestBid = bids.get(0);
                L2Book.Levels bestAsk = asks.get(0);
                System.out.printf("Best bid: %s x %s%n", bestBid.getPx(), bestBid.getSz());
                System.out.printf("Best ask: %s x %s%n", bestAsk.getPx(), bestAsk.getSz());
            }
        }

        // ==================== 5. Query recent candles for BTC (last 10 one-minute
        // candles) ====================
        System.out.println("\n--- 4) candleSnapshotByCount(\"BTC\", 1m, 10): recent candles ---");
        List<Candle> candles = info.candleSnapshotByCount("BTC", CandleInterval.MINUTE_1, 10);
        if (candles != null && !candles.isEmpty()) {
            Candle last = candles.getLast();
            System.out.println("Last candle:");
            System.out.println("  Symbol  : " + last.getSymbol());
            System.out.println("  Interval: " + last.getInterval());
            System.out.println("  Open    : " + last.getOpenPrice());
            System.out.println("  High    : " + last.getHighPrice());
            System.out.println("  Low     : " + last.getLowPrice());
            System.out.println("  Close   : " + last.getClosePrice());
            System.out.println("  Volume  : " + last.getVolume());
        }

        // ==================== 6. Optional: query user state and rate limit if wallet
        // address is provided ====================
        System.out.println("\n--- 5) userState() and userRateLimit(): account-related data (optional) ---");
        String address = System.getenv("PRIMARY_WALLET_ADDRESS");
        if (address == null || address.isBlank()) {
            System.out.println("PRIMARY_WALLET_ADDRESS is not set, skip user-related queries.");
        } else {
            // Info user APIs only need the address string, no private key is required
            address = address.toLowerCase();

            ClearinghouseState state = info.userState(address);
            if (state != null && state.getMarginSummary() != null) {
                System.out.println("Account info:");
                System.out.println("  Account value    : " + state.getMarginSummary().getAccountValue());
                System.out.println("  Total margin used: " + state.getMarginSummary().getTotalMarginUsed());
            }

            UserRateLimit limit = info.userRateLimit(address);
            if (limit != null) {
                System.out.println("User rate limit:");
                System.out.println("  Cumulative volume: " + limit.getCumVlm());
                System.out.println("  Requests used    : " + limit.getnRequestsUsed());
                System.out.println("  Requests cap     : " + limit.getnRequestsCap());
            }
        }

        System.out.println("\n=== InfoOverviewExample execution completed ===");
    }
}
