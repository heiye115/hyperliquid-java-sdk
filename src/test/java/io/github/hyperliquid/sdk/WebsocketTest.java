package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.Candle;
import io.github.hyperliquid.sdk.model.info.CandleInterval;
import io.github.hyperliquid.sdk.model.subscription.ActiveSubscription;
import io.github.hyperliquid.sdk.model.subscription.CandleSubscription;
import io.github.hyperliquid.sdk.model.subscription.L2BookSubscription;
import io.github.hyperliquid.sdk.model.subscription.OrderUpdatesSubscription;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * WebSocket subscription BTC market data example
 * <p>
 * Demonstrated features:
 * 1. Subscribe to BTC L2 order book (real-time market depth data)
 * 2. Subscribe to BTC real-time trades
 * 3. Subscribe to BTC candlestick data (1-minute period)
 * 4. Add connection status listener
 * 5. Gracefully close WebSocket connection
 * </p>
 */
public class WebsocketTest {

    public static void main(String[] args) throws JsonProcessingException {
        // Note: WebSocket subscription does not require private key, only subscribes to
        // public market data
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl() // Use testnet
                .build();
        Info info = client.getInfo();
        // ==================== 4. Subscribe to BTC real-time trades
        // ====================
        // Subscribe to BTC individual trades====================
        info.subscribe(CandleSubscription.of("BTC", "1m"), msg -> System.out.println("CandleSubscription BTC 1m data: " + msg));
        info.subscribe(L2BookSubscription.of("BTC"), msg -> System.out.println("L2BookSubscription BTC data: " + msg));

    }

    @Test
    public void candleSnapshotByCount() throws JsonProcessingException {
        HyperliquidClient client = HyperliquidClient.builder()
                // .testNetUrl()
                // .addPrivateKey(TESTNET_PRIVATE_KEY)
                .build();
        Info info = client.getInfo();
        List<Candle> candles = info.candleSnapshotByCount("BTC", CandleInterval.MINUTE_15, 1000);
        System.out.println(JSONUtil.writeValueAsString(candles));
    }

    @Test
    public void orderUpdates() throws JsonProcessingException {
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                // .addPrivateKey(TESTNET_PRIVATE_KEY)
                .build();
        OrderUpdatesSubscription orderUpdatesSubscription = OrderUpdatesSubscription.of("0x....");
        client.getInfo().subscribe(orderUpdatesSubscription, System.out::println);

        /*
         * Retrieve all active subscriptions
         *
         * Subscription identifier: orderUpdates
         * Subscription payload example: {"user":"0x....","type":"orderUpdates"}
         */
        Map<String, ActiveSubscription> subscriptions = client.getInfo().getWsManager()
                .getSubscriptions();
        subscriptions.forEach((k, v) -> {
            System.out.println("Subscription identifier: " + k + " - " + v.getSubscription());
        });

        // Alternative lookup: identifier "orderUpdates" with payloads like
        // {"type":"orderUpdates","user":"0x...."}
        ActiveSubscription orderUpdates = client.getInfo().getWsManager()
                .getSubscriptionsByIdentifier("orderUpdates");
        System.out.println("Subscription payload: " + orderUpdates.getSubscription());

        // Wait for messages to arrive on the WebSocket stream
        try {
            Thread.sleep(6000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void candleSubscription() {
        HyperliquidClient client = HyperliquidClient.builder().build();
        Info info = client.getInfo();
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

        CandleSubscription ethCandle = CandleSubscription.of("ETH", "1m");

        info.subscribe(ethCandle, msg -> {
            JsonNode data = msg.get("data");
            if (data != null) {
                String open = data.path("o").asText();
                String high = data.path("h").asText();
                String low = data.path("l").asText();
                String close = data.path("c").asText();
                String volume = data.path("v").asText();
                System.out.printf("[Candle] ETH 1m - O: %s, H: %s, L: %s, C: %s, V: %s%n",
                        open, high, low, close, volume);
            }
        });

        // Wait for 10 minutes
        try {
            Thread.sleep(600000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
