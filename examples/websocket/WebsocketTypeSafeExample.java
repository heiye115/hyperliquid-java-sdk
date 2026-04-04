package io.github.hyperliquid.sdk.examples.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.subscription.*;
import io.github.hyperliquid.sdk.utils.JSONUtil;

import java.util.Map;
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
public class WebsocketTypeSafeExample {

    public static void main(String[] args) throws InterruptedException {
        // ==================== 1. Initialize Client ====================
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()  // Use testnet
                .build();

        Info info = client.getInfo();
        String userAddress = System.getenv("HL_USER_ADDRESS");
        if (userAddress == null || userAddress.isBlank()) {
            userAddress = "0x0000000000000000000000000000000000000000";
        }

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

        System.out.println("--- Subscribe to userEvents and orderUpdates ---");
        UserEventsSubscription userEvents = UserEventsSubscription.create("0x0000000000000000000000000000000000000000");
        info.subscribe(userEvents, msg -> System.out.println("[userEvents] " + msg.path("channel").asText()));

        OrderUpdatesSubscription orderUpdates = OrderUpdatesSubscription.of(userAddress);
        info.subscribe(orderUpdates, msg -> System.out.println("[orderUpdates] " + msg.path("channel").asText()));

        System.out.println("--- Subscribe to user channels with JsonNode ---");
        info.subscribe(JSONUtil.convertValue(
                Map.of("type", "userFills", "user", userAddress), JsonNode.class
        ), msg -> System.out.println("[userFills] " + msg.path("channel").asText()));

        info.subscribe(JSONUtil.convertValue(
                Map.of("type", "userFundings", "user", userAddress), JsonNode.class
        ), msg -> System.out.println("[userFundings] " + msg.path("channel").asText()));

        info.subscribe(JSONUtil.convertValue(
                Map.of("type", "userNonFundingLedgerUpdates", "user", userAddress), JsonNode.class
        ), msg -> System.out.println("[userNonFundingLedgerUpdates] " + msg.path("channel").asText()));

        info.subscribe(JSONUtil.convertValue(
                Map.of("type", "webData2", "user", userAddress), JsonNode.class
        ), msg -> System.out.println("[webData2] " + msg.path("channel").asText()));

        info.subscribe(JSONUtil.convertValue(
                Map.of("type", "activeAssetCtx", "coin", "BTC"), JsonNode.class
        ), msg -> System.out.println("[activeAssetCtx-perp] " + msg.path("channel").asText()));

        info.subscribe(JSONUtil.convertValue(
                Map.of("type", "activeAssetCtx", "coin", "@1"), JsonNode.class
        ), msg -> System.out.println("[activeAssetCtx-spot] " + msg.path("channel").asText()));

        info.subscribe(JSONUtil.convertValue(
                Map.of("type", "activeAssetData", "user", userAddress, "coin", "BTC"), JsonNode.class
        ), msg -> System.out.println("[activeAssetData] " + msg.path("channel").asText()));

        System.out.println("--- Subscribe to BTC Trades with Two Callbacks ---");
        TradesSubscription btcTrades = TradesSubscription.of("BTC");
        var btcTradesHandleA = info.subscribeWithHandle(btcTrades, msg ->
                System.out.println("[BTC Trades Callback-A] tick received"));
        var btcTradesHandleB = info.subscribeWithHandle(btcTrades, msg ->
                System.out.println("[BTC Trades Callback-B] tick received"));

        System.out.println("\nReceiving real-time market data for 20 seconds...\n");
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(20, TimeUnit.SECONDS);

        System.out.println("Unsubscribe BTC trades callback A by handle...");
        boolean unsubscribedA = info.unsubscribe(btcTradesHandleA);
        System.out.println("Callback A unsubscribed: " + unsubscribedA);

        System.out.println("\nReceiving with callback B still active for 20 seconds...\n");
        latch.await(20, TimeUnit.SECONDS);

        System.out.println("Unsubscribe BTC trades callback B by subscription id...");
        boolean unsubscribedB = info.unsubscribe(btcTradesHandleB.getSubscriptionId());
        System.out.println("Callback B unsubscribed: " + unsubscribedB);

        System.out.println("\nReceiving remaining subscriptions for final 20 seconds...\n");
        latch.await(20, TimeUnit.SECONDS);

        // ==================== 8. Gracefully Close WebSocket ====================
        System.out.println("\nClosing WebSocket connection...");
        info.closeWs();
        System.out.println("WebSocket closed, program exiting.");
    }
}
