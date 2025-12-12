package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.Candle;
import io.github.hyperliquid.sdk.model.info.CandleInterval;
import io.github.hyperliquid.sdk.model.subscription.TradesSubscription;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;
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
public class ExampleWebsocketBTC {

    public static void main(String[] args) {
        // Note: WebSocket subscription does not require private key, only subscribes to public market data
        HyperliquidClient client = HyperliquidClient.builder()
                //.testNetUrl()  // 使用测试网
                .build();
        Info info = client.getInfo();
        // ==================== 4. Subscribe to BTC real-time trades ====================
        // Subscribe to BTC individual trades====================
        TradesSubscription btcTrades = TradesSubscription.of("BTC");
        info.subscribe(btcTrades, msg -> System.out.println("BTC 成交: " + msg));

        // Subscribe to ETH individual trades
        TradesSubscription ethTrades = TradesSubscription.of("ETH");
        info.subscribe(ethTrades, msg -> System.out.println("ETH 成交: " + msg));
    }

    @Test
    public void candleSnapshotByCount() throws JsonProcessingException {
        HyperliquidClient client = HyperliquidClient.builder()
                //.testNetUrl()
                //.addPrivateKey(TESTNET_PRIVATE_KEY)
                .build();
        Info info = client.getInfo();
        List<Candle> candles = info.candleSnapshotByCount("BTC", CandleInterval.MINUTE_15, 1000);
        System.out.println(JSONUtil.writeValueAsString(candles));
    }

    @Test
    public void orderUpdates() throws JsonProcessingException {
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                //.addPrivateKey(TESTNET_PRIVATE_KEY)
                .build();
        JsonNode sub = JSONUtil.convertValue(java.util.Map.of("type", "orderUpdates", "user", "0x...."), JsonNode.class);
        client.getInfo().subscribe(sub, System.out::println);

        /*  获取所有订阅
        *
        * 订阅: orderUpdates
          订阅内容: {"user":"0x....","type":"orderUpdates"}
        * */
        Map<String, List<WebsocketManager.ActiveSubscription>> subscriptions = client.getInfo().getWsManager().getSubscriptions();
        subscriptions.forEach((k, v) -> {
            System.out.println("订阅: " + k);
            for (WebsocketManager.ActiveSubscription activeSubscription : v) {
                System.out.println("订阅内容: " + activeSubscription.getSubscription());
            }
        });

        //等待
        try {
            Thread.sleep(6000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
