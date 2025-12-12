package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.Candle;
import io.github.hyperliquid.sdk.model.info.CandleInterval;
import io.github.hyperliquid.sdk.model.subscription.TradesSubscription;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
