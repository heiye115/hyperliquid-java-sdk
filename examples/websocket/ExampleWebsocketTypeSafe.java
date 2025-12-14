import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.subscription.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket Type-Safe Subscription Example
 * <p>
 * Features:
 * 1. Use type-safe Subscription entity classes instead of JsonNode
 * 2. Subscribe to multiple types of market data (L2 order book, trades, candles, BBO, all mids)
 * 3. Enjoy compile-time type checking and better code readability
 * </p>
 */
public class ExampleWebsocketTypeSafe {

    public static void main(String[] args) throws InterruptedException {
        // ==================== 1. Initialize Client ====================
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()  // Use testnet
                .build();

        Info info = client.getInfo();

        System.out.println("=== Hyperliquid WebSocket Type-Safe Subscription Example ===\n");

        // ==================== 2. Subscribe to BTC L2 Order Book ====================
        System.out.println("--- Subscribe to BTC L2 Order Book ---");
        L2BookSubscription btcL2Book = L2BookSubscription.of("BTC");

        info.subscribe(btcL2Book, msg -> {
            JsonNode data = msg.get("data");
            if (data != null && data.has("levels")) {
                JsonNode levels = data.get("levels");
                if (levels.isArray() && levels.size() >= 2) {
                    JsonNode bids = levels.get(0);
                    JsonNode asks = levels.get(1);
                    if (bids.isArray() && !bids.isEmpty() && asks.isArray() && !asks.isEmpty()) {
                        String bestBid = bids.get(0).get("px").asText();
                        String bestAsk = asks.get(0).get("px").asText();
                        System.out.printf("[L2 Book] BTC Best Bid: %s, Best Ask: %s%n", bestBid, bestAsk);
                    }
                }
            }
        });

        // ==================== 3. Subscribe to ETH Trades ====================
        System.out.println("--- Subscribe to ETH Trades ---");
        TradesSubscription ethTrades = TradesSubscription.of("ETH");

        info.subscribe(ethTrades, msg -> {
            JsonNode data = msg.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode trade : data) {
                    String coin = trade.path("coin").asText();
                    String price = trade.path("px").asText();
                    String size = trade.path("sz").asText();
                    String side = trade.path("side").asText();
                    System.out.printf("[Trades] %s %s - Price: %s, Size: %s%n",
                            coin, side.equals("A") ? "Sell" : "Buy", price, size);
                }
            }
        });

        // ==================== 4. Subscribe to BTC 1-Minute Candle ====================
        System.out.println("--- Subscribe to BTC 1-Minute Candle ---");
        CandleSubscription btcCandle = CandleSubscription.of("BTC", "1m");

        info.subscribe(btcCandle, msg -> {
            JsonNode data = msg.get("data");
            if (data != null) {
                String open = data.path("o").asText();
                String high = data.path("h").asText();
                String low = data.path("l").asText();
                String close = data.path("c").asText();
                String volume = data.path("v").asText();
                System.out.printf("[Candle] BTC 1m - O: %s, H: %s, L: %s, C: %s, V: %s%n",
                        open, high, low, close, volume);
            }
        });

        // ==================== 5. Subscribe to ETH Best Bid/Offer ====================
        System.out.println("--- Subscribe to ETH Best Bid/Offer ---");
        BboSubscription ethBbo = BboSubscription.of("ETH");

        info.subscribe(ethBbo, msg -> {
            JsonNode data = msg.get("data");
            if (data != null) {
                String bidPrice = data.path("bid").path("px").asText();
                String bidSize = data.path("bid").path("sz").asText();
                String askPrice = data.path("ask").path("px").asText();
                String askSize = data.path("ask").path("sz").asText();
                System.out.printf("[BBO] ETH Bid: %s@%s, Ask: %s@%s%n",
                        bidPrice, bidSize, askPrice, askSize);
            }
        });

        // ==================== 6. Subscribe to All Coins Mid Prices ====================
        System.out.println("--- Subscribe to All Coins Mid Prices ---");
        AllMidsSubscription allMids = AllMidsSubscription.create();

        info.subscribe(allMids, msg -> {
            JsonNode data = msg.get("data");
            if (data != null && data.has("mids")) {
                JsonNode mids = data.get("mids");
                if (mids != null && mids.isObject()) {
                    // Only print first 3 coins to avoid too much output
                    int count = 0;
                    var iter = mids.fields();
                    System.out.print("[Mids] ");
                    while (iter.hasNext() && count < 3) {
                        var entry = iter.next();
                        System.out.printf("%s: %s  ", entry.getKey(), entry.getValue().asText());
                        count++;
                    }
                    System.out.println("...");
                }
            }
        });

        // ==================== 7. Keep Running to Receive Messages ====================
        System.out.println("\nReceiving real-time market data, will exit after 60 seconds...\n");

        // Use CountDownLatch to wait for 60 seconds
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(60, TimeUnit.SECONDS);

        // ==================== 8. Gracefully Close WebSocket ====================
        System.out.println("\nClosing WebSocket connection...");
        info.closeWs();
        System.out.println("WebSocket closed, program exiting.");
    }
}
