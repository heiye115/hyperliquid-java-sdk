package io.github.hyperliquid.sdk.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.subscription.TradesSubscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket Multi-Coin Trades Subscription Example
 * <p>
 * Features:
 * 1. Subscribe to BTC and ETH trades simultaneously
 * 2. Each coin uses independent callback function to handle messages
 * 3. Demonstrates Hyperliquid's official multi-subscription capability
 * </p>
 */
public class WebsocketMultiTradesExample {

    public static void main(String[] args) throws InterruptedException {
        // ==================== 1. Initialize Client ====================
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()  // Use testnet
                .build();

        Info info = client.getInfo();

        System.out.println("=== Hyperliquid WebSocket Multi-Coin Trades Subscription Example ===\n");

        // ==================== 2. Subscribe to BTC Trades (Using type-safe entity) ====================
        System.out.println("--- Subscribe to BTC Trades ---");
        TradesSubscription btcTrades = TradesSubscription.of("BTC");

        var btcHandle = info.subscribeWithHandle(btcTrades, msg -> {
            // Parse BTC trade data
            JsonNode data = msg.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode trade : data) {
                    String coin = trade.path("coin").asText();
                    String price = trade.path("px").asText();
                    String size = trade.path("sz").asText();
                    String side = trade.path("side").asText();
                    long time = trade.path("time").asLong();

                    System.out.printf("[BTC Trade] %s %s - Price: %s, Size: %s, Time: %d%n",
                            coin, side.equals("A") ? "Sell" : "Buy", price, size, time);
                }
            }
        });

        // ==================== 3. Subscribe to ETH Trades (Using type-safe entity) ====================
        System.out.println("--- Subscribe to ETH Trades ---");
        TradesSubscription ethTrades = TradesSubscription.of("ETH");

        var ethHandle = info.subscribeWithHandle(ethTrades, msg -> {
            // Parse ETH trade data
            JsonNode data = msg.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode trade : data) {
                    String coin = trade.path("coin").asText();
                    String price = trade.path("px").asText();
                    String size = trade.path("sz").asText();
                    String side = trade.path("side").asText();
                    long time = trade.path("time").asLong();

                    System.out.printf("[ETH Trade] %s %s - Price: %s, Size: %s, Time: %d%n",
                            coin, side.equals("A") ? "Sell" : "Buy", price, size, time);
                }
            }
        });

        // ==================== 4. Optional: Subscribe to More Coins ====================
        // You can continue adding subscriptions for more coins, for example:
        /*
        TradesSubscription solTrades = TradesSubscription.of("SOL");
        info.subscribe(solTrades, msg -> {
            // Handle SOL trade data
        });
        */

        System.out.println("\nReceiving BTC and ETH trades for 20 seconds...\n");
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(20, TimeUnit.SECONDS);

        System.out.println("Unsubscribe BTC callback by handle...");
        boolean unsubscribedByHandle = info.unsubscribe(btcHandle);
        System.out.println("BTC unsubscribed by handle: " + unsubscribedByHandle);

        System.out.println("\nReceiving ETH trades only for 20 seconds...\n");
        latch.await(20, TimeUnit.SECONDS);

        System.out.println("Unsubscribe ETH callback by subscription id...");
        boolean unsubscribedById = info.unsubscribe(ethHandle.getSubscriptionId());
        System.out.println("ETH unsubscribed by id: " + unsubscribedById);

        System.out.println("\nNo trade subscriptions for final 20 seconds...\n");
        latch.await(20, TimeUnit.SECONDS);

        // ==================== 6. Gracefully Close WebSocket ====================
        System.out.println("\nClosing WebSocket connection...");
        info.closeWs();
        System.out.println("WebSocket closed, program exiting.");
    }
}
