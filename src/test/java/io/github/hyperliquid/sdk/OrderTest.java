package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test for order placement and closing logic.
 */
public class OrderTest {

    /**
     * Private key (testnet)
     */
    String privateKey = "your_private_key_here";

    /**
     * Unified client (testnet + enable debug logging)
     */
    HyperliquidClient client = HyperliquidClient.builder()
            .testNetUrl()
            .addPrivateKey(privateKey)
            .build();

    /**
     * Market order placement
     **/
    @Test
    public void testMarketOrder() throws JsonProcessingException {
        OrderRequest req = OrderRequest.Open.market("ETH", true, "0.02");
        Order order = client.getExchange().order(req);
        System.out.println(JSONUtil.writeValueAsString(order));
    }

    /**
     * Market close position
     **/
    @Test
    public void testMarketCloseOrder() {
        OrderRequest req = OrderRequest.Close.market("ETH", "0.01", Cloid.auto());
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Close all positions at market price
     **/
    @Test
    public void testMarketCloseAllOrder() {
        Order order = client.getExchange().closePositionMarket("ETH");
        System.out.println(order);
    }

    /**
     * Limit order placement
     **/
    @Test
    public void testLimitOrder() {
        OrderRequest req = OrderRequest.Open.limit(Tif.GTC, "ETH", true, "0.01", "1800.0");
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Limit close position
     **/
    @Test
    public void testLimitCloseOrder() {
        OrderRequest req = OrderRequest.Close.limit("ETH", "0.01", "2000.0", Cloid.auto());
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Close all positions at limit price
     **/
    @Test
    public void testLimitCloseAllOrder() {
        Order order = client.getExchange().closePositionLimit(Tif.GTC, "ETH", "4000.0", Cloid.auto());
        System.out.println(order);
    }

    @Test
    public void testCancel() {
        JsonNode node = client.getExchange().cancel("ETH", 42988070692L);
        System.out.println(node.toPrettyString());
    }


    @Test
    public void testMarketOrderALL() {
        OrderRequest req = OrderRequest.Open.market("ETH", true, "0.01");
        Order order = client.getExchange().order(req);
        System.out.println(order);
        Order closeOrder = client.getExchange().closePositionMarket("ETH");
        System.out.println(closeOrder);
    }

    @Test
    public void testTriggerOrderALL() {
        // 使用 breakoutAbove 替代已删除的 trigger 方法
        OrderRequest req = OrderRequest.Open.breakoutAbove("ETH", "0.01", "4000.0");
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Test stop loss close position
     */
    @Test
    public void testStopLoss() throws JsonProcessingException {
        OrderRequest req = OrderRequest.Close.stopLoss("ETH", true, "0.01", "3500.0");
        Order order = client.getExchange().order(req);
        System.out.println(JSONUtil.writeValueAsString(order));
    }

    /**
     * Test take profit close position
     */
    @Test
    public void testTakeProfit() throws JsonProcessingException {
        OrderRequest req = OrderRequest.Close.takeProfit("ETH", true, "0.01", "3000.0");
        Order order = client.getExchange().order(req);
        System.out.println(JSONUtil.writeValueAsString(order));
    }
}

 
