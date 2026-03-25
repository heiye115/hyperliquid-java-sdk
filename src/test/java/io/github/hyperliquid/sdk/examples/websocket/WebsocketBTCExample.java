package io.github.hyperliquid.sdk.examples.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket BTC Market Data Subscription Example
 * <p>
 * Features:
 * 1. Subscribe to BTC L2 order book (real-time depth data)
 * 2. Subscribe to BTC real-time trades
 * 3. Subscribe to BTC candle data (1-minute interval)
 * 4. Add connection state listener
 * 5. Gracefully close WebSocket connection
 * </p>
 */
public class WebsocketBTCExample {

    public static void main(String[] args) throws InterruptedException {
        // ==================== 1. Initialize Client ====================
        // Note: WebSocket subscription doesn't require private key, only public market data
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()  // Use testnet
                .build();

        Info info = client.getInfo();

        System.out.println("=== Hyperliquid WebSocket BTC Market Data Subscription Example ===\n");

        // ==================== 2. Add Connection Listener (Optional) ====================
        // Note: Connection listener is COMPLETELY OPTIONAL, WebSocket works without it
        // Add listener only for observing connection state, useful for debugging and logging
        info.addConnectionListener(new WebsocketManager.ConnectionListener() {
            @Override
            public void onConnecting(String url) {
                System.out.println("[Connecting] WebSocket connecting: " + url);
            }

            @Override
            public void onConnected(String url) {
                System.out.println("[Connected] WebSocket connected successfully: " + url);
            }

            @Override
            public void onDisconnected(String url, int code, String reason, Throwable cause) {
                System.out.println("[Disconnected] WebSocket disconnected - Code: " + code + ", Reason: " + reason);
                if (cause != null) {
                    System.out.println("[Error] Disconnect cause: " + cause.getMessage());
                }
            }

            @Override
            public void onReconnecting(String url, int attempt, long nextDelayMs) {
                System.out.println("[Reconnecting] Attempt " + attempt + ", delay " + nextDelayMs + " ms");
            }

            @Override
            public void onReconnectFailed(String url, int attempted, Throwable lastError) {
                System.out.println("[Reconnect Failed] Attempted " + attempted + " times");
            }

            @Override
            public void onNetworkUnavailable(String url) {
                System.out.println("[Network Unavailable] Network probe failed");
            }

            @Override
            public void onNetworkAvailable(String url) {
                System.out.println("[Network Available] Network restored");
            }
        });

        // ==================== 3. Subscribe to BTC L2 Order Book ====================
        // WebSocket connection is automatically established, just subscribe directly
        System.out.println("\n--- Subscribe to BTC L2 Order Book ---");
        JsonNode l2BookSub = JSONUtil.convertValue(
                Map.of("type", "l2Book", "coin", "BTC"),
                JsonNode.class
        );

        info.subscribe(l2BookSub, msg -> {
            // Parse order book data
            JsonNode data = msg.get("data");
            if (data != null && data.has("levels")) {
                JsonNode levels = data.get("levels");
                if (levels.isArray() && levels.size() >= 2) {
                    JsonNode bids = levels.get(0); // Bids
                    JsonNode asks = levels.get(1); // Asks
                    if (bids.isArray() && !bids.isEmpty() && asks.isArray() && !asks.isEmpty()) {
                        String bestBid = bids.get(0).get("px").asText();
                        String bestAsk = asks.get(0).get("px").asText();
                        System.out.printf("[L2 Book] BTC Best Bid: %s, Best Ask: %s%n", bestBid, bestAsk);
                    }
                }
            }
        });

        // ==================== 4. Subscribe to BTC Real-time Trades ====================
        System.out.println("--- Subscribe to BTC Real-time Trades ---");
        JsonNode tradesSub = JSONUtil.convertValue(
                Map.of("type", "trades", "coin", "BTC"),
                JsonNode.class
        );

        info.subscribe(tradesSub, msg -> {
            // Parse trade data
            JsonNode data = msg.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode trade : data) {
                    String price = trade.path("px").asText();
                    String size = trade.path("sz").asText();
                    String side = trade.path("side").asText();
                    long time = trade.path("time").asLong();
                    System.out.printf("[Trade] BTC %s - Price: %s, Size: %s, Time: %d%n",
                            side, price, size, time);
                }
            }
        });

        // ==================== 5. Subscribe to BTC 1-Minute Candle ====================
        System.out.println("--- Subscribe to BTC 1-Minute Candle ---");
        JsonNode candleSub = JSONUtil.convertValue(
                Map.of("type", "candle", "coin", "BTC", "interval", "1m"),
                JsonNode.class
        );

        info.subscribe(candleSub, msg -> {
            // Parse candle data
            JsonNode data = msg.get("data");
            if (data != null) {
                String open = data.path("o").asText();
                String high = data.path("h").asText();
                String low = data.path("l").asText();
                String close = data.path("c").asText();
                String volume = data.path("v").asText();
                long timestamp = data.path("t").asLong();
                System.out.printf("[Candle] BTC 1m - O: %s, H: %s, L: %s, C: %s, V: %s, T: %d%n",
                        open, high, low, close, volume, timestamp);
            }
        });

        // ==================== 6. Subscribe to BTC Best Bid/Offer (BBO) ====================
        System.out.println("--- Subscribe to BTC Best Bid/Offer ---");
        JsonNode bboSub = JSONUtil.convertValue(
                Map.of("type", "bbo", "coin", "BTC"),
                JsonNode.class
        );

        info.subscribe(bboSub, msg -> {
            JsonNode data = msg.get("data");
            if (data != null) {
                String bidPrice = data.path("bid").path("px").asText();
                String bidSize = data.path("bid").path("sz").asText();
                String askPrice = data.path("ask").path("px").asText();
                String askSize = data.path("ask").path("sz").asText();
                System.out.printf("[BBO] BTC Bid: %s@%s, Ask: %s@%s%n",
                        bidPrice, bidSize, askPrice, askSize);
            }
        });

        // ==================== 7. Keep Running to Receive Messages ====================
        System.out.println("\nReceiving BTC real-time market data, will exit after 60 seconds...\n");

        // Use CountDownLatch to wait for 60 seconds
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(60, TimeUnit.SECONDS);

        // ==================== 8. Gracefully Close WebSocket ====================
        System.out.println("\nClosing WebSocket connection...");
        info.closeWs();
        System.out.println("WebSocket closed, program exiting.");
    }
}

