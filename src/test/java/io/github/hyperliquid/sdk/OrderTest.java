package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.OrderStatus;
import io.github.hyperliquid.sdk.model.order.*;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test for order placement and closing logic.
 */
public class OrderTest {

    /**
     * Private key (testnet)
     */
    String privateKey = "your_private_key_here";

    /**
     * Unified client (testnet)
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
        OrderRequest req = OrderRequest.Open.market("ETH", false, "0.02");
        Order order = client.getExchange().order(req);
        System.out.println(JSONUtil.writeValueAsString(order));
    }

    /**
     * Market close position
     **/
    @Test
    public void testMarketCloseOrder() throws JsonProcessingException {
        OrderRequest req = OrderRequest.Close.market("ETH", "0.01", Cloid.auto());
        Order order = client.getExchange().order(req);
        System.out.println(JSONUtil.writeValueAsString(order));
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
        OrderRequest req = OrderRequest.Open.limit("ETH", false, "0.01", "3500.0");
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Limit close position
     **/
    @Test
    public void testLimitCloseOrder() {
        OrderRequest req = OrderRequest.Close.limit("ETH", "0.01", "3500.0", Cloid.auto());
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
        // Use breakoutAbove instead of the removed trigger method
        OrderRequest req = OrderRequest.Open.breakoutAbove("ETH", "0.01", "4000.0");
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Market order placement with explicit Cloid.
     */
    @Test
    public void testMarketOrderWithCloid() {
        OrderRequest req = OrderRequest.Open.market("ETH", true, "0.01", Cloid.auto());
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Limit order placement using builder pattern.
     */
    @Test
    public void testBuilderLimitOrder() {
        OrderRequest req = OrderRequest.builder()
                .perp("ETH")
                .buy("0.01")
                .limitPrice("3500.0")
                .gtc()
                .build();
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Trigger order (breakout above) using builder pattern.
     */
    @Test
    public void testBuilderTriggerOrder() {
        OrderRequest req = OrderRequest.builder()
                .perp("ETH")
                .buy("0.01")
                .stopAbove("2950.0")
                .limitPrice("3000.0")
                .build();
        Order order = client.getExchange().order(req);
        System.out.println(order);
    }

    /**
     * Combined order: open position with take-profit and stop-loss using
     * bulkOrders.
     */
    @Test
    public void testBulkNormalTpsl() throws JsonProcessingException {
        OrderWithTpSlBuilder builder = OrderRequest.entryWithTpSl()
                .perp("ETH")
                .buy("0.01")
                .entryPrice("3500.0")
                .takeProfit("3600.0")
                .stopLoss("3400.0");
        JsonNode result = client.getExchange().bulkOrders(builder.buildNormalTpsl());
        System.out.println(result.toPrettyString());
    }

    /**
     * Combined order: add position-level take-profit/stop-loss using bulkOrders.
     */
    @Test
    public void testBulkPositionTpsl() throws JsonProcessingException {
        OrderWithTpSlBuilder builder = OrderRequest.entryWithTpSl()
                .perp("ETH")
                .closePosition("0.01", true)
                .takeProfit("3600.0")
                .stopLoss("3400.0");
        JsonNode result = client.getExchange().bulkOrders(builder.buildPositionTpsl());
        System.out.println(result.toPrettyString());
    }

    /**
     * Batch order placement using normal grouping (na) with a list of orders.
     */
    @Test
    public void testBulkNormalOrdersList() {
        OrderRequest buyOrder = OrderRequest.builder()
                .perp("ETH")
                .buy("0.01")
                .limitPrice("3500.0")
                .gtc()
                .build();

        OrderRequest sellOrder = OrderRequest.builder()
                .perp("ETH")
                .sell("0.01")
                .limitPrice("3600.0")
                .gtc()
                .build();

        JsonNode result = client.getExchange().bulkOrders(List.of(buyOrder, sellOrder));
        System.out.println(result.toPrettyString());
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

    /**
     * Convenience factories: takeProfitForLong/Short and stopLossForLong/Short.
     */
    @Test
    public void testTpSlConvenienceFactories() {
        OrderRequest tpLong = OrderRequest.Close.takeProfitForLong("ETH", "0.01", "3600.0");
        OrderRequest tpShort = OrderRequest.Close.takeProfitForShort("ETH", "0.01", "3000.0");
        OrderRequest slLong = OrderRequest.Close.stopLossForLong("ETH", "0.01", "3400.0");
        OrderRequest slShort = OrderRequest.Close.stopLossForShort("ETH", "0.01", "3200.0");

        assertTrue(tpLong.getReduceOnly());
        assertTrue(tpShort.getReduceOnly());
        assertTrue(slLong.getReduceOnly());
        assertTrue(slShort.getReduceOnly());

        assertEquals(Boolean.FALSE, tpLong.getIsBuy());
        assertEquals(Boolean.TRUE, tpShort.getIsBuy());
        assertEquals(Boolean.FALSE, slLong.getIsBuy());
        assertEquals(Boolean.TRUE, slShort.getIsBuy());
    }

    @Test
    public void testOrderStatus() throws JsonProcessingException {
        Info info = client.getInfo();
        OrderStatus orderStatus = info.orderStatus("0x...", 00000L);
        System.out.println(JSONUtil.writeValueAsString(orderStatus));
    }
}
