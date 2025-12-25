package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.ClearinghouseState;
import io.github.hyperliquid.sdk.model.info.FrontendOpenOrder;
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
     * your_private_key_here
     */
    String privateKey = "your_private_key_here";

    /**
     * Unified client (testnet)
     */
    HyperliquidClient client = HyperliquidClient.builder()
            //.testNetUrl()
            .addPrivateKey(privateKey)
            .build();

    /**
     * Market order placement
     **/
    @Test
    public void testMarketOrder() throws JsonProcessingException {
        Cloid auto = Cloid.auto();
        System.out.println("auto:" + auto);
        OrderRequest req = OrderRequest.Open.market("BTC", true, "0.001", auto);
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
        OrderRequest req = OrderRequest.Open.limit("BTC", false, "0.001", "80000.0");
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

    /**
     * Test order cancellation by coin and order ID.
     */
    @Test
    public void testCancel() {
        JsonNode node = client.getExchange().cancel("ETH", 42988070692L);
        System.out.println(node.toPrettyString());
    }

    /**
     * Test placing a market order and then immediately closing the position.
     */
    @Test
    public void testMarketOrderALL() {
        OrderRequest req = OrderRequest.Open.market("ETH", true, "0.01");
        Order order = client.getExchange().order(req);
        System.out.println(order);
        Order closeOrder = client.getExchange().closePositionMarket("ETH");
        System.out.println(closeOrder);
    }

    /**
     * Test placing a trigger order (breakout above).
     * <p>
     * Uses breakoutAbove instead of the removed trigger method.
     * </p>
     */
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
                .perp("BTC")
                .buy("0.001")
                //.entryPrice("3500.0")
                .takeProfit("90000.0")
                .stopLoss("86000.0");
        BulkOrder bulkOrder = client.getExchange().bulkOrders(builder.buildNormalTpsl());
        System.out.println(JSONUtil.writeValueAsString(bulkOrder));
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
        BulkOrder bulkOrder = client.getExchange().bulkOrders(builder.buildPositionTpsl());
        System.out.println(bulkOrder);
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
                .build();

        OrderRequest sellOrder = OrderRequest.builder()
                .perp("ETH")
                .sell("0.01")
                .limitPrice("3600.0")
                .build();

        BulkOrder bulkOrder = client.getExchange().bulkOrders(List.of(buyOrder, sellOrder));
        System.out.println(bulkOrder);
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

    /**
     * Test querying order status by address and order ID.
     */
    @Test
    public void testOrderStatus() throws JsonProcessingException {
        Info info = client.getInfo();
        OrderStatus orderStatus = info.orderStatus("0x...", 00000L);
        System.out.println(JSONUtil.writeValueAsString(orderStatus));
    }

    /**
     * Test take profit orders.
     * <p>
     * Includes both regular limit take profit and conditional limit take profit.
     * </p>
     */
    @Test
    public void testTakeProfit2() throws JsonProcessingException {
        // Conditional limit take profit order
        OrderRequest req2 = OrderRequest.builder()
                .perp("ETH")
                .sell("0.01") // or .buy("0.01")
                .limitPrice("3000.0") // Take profit price
                .stopAbove("3000.0") // Trigger price
                .reduceOnly() // Reduce only
                .build();
        Order order2 = client.getExchange().order(req2);
        System.out.println(JSONUtil.writeValueAsString(order2));
    }

    /**
     * Test stop loss orders.
     * <p>
     * Specifically tests conditional limit stop loss orders.
     * </p>
     */
    @Test
    public void testStopLoss2() throws JsonProcessingException {
        // Conditional limit stop loss order
        OrderRequest req2 = OrderRequest.builder()
                .perp("ETH")
                .sell("0.01") // or .buy("0.01")
                .limitPrice("2900.0") // Stop loss price
                .stopBelow("2900.0") // Trigger price
                .reduceOnly() // Reduce only
                .build();
        Order order2 = client.getExchange().order(req2);
        System.out.println(JSONUtil.writeValueAsString(order2));
    }

    @Test
    public void testUserState() throws JsonProcessingException {
        ClearinghouseState clearinghouseState = client.getInfo().userState("0x...");
        System.out.println(JSONUtil.writeValueAsString(clearinghouseState));

    }

    @Test
    public void testFrontendOpenOrders() throws JsonProcessingException {
        List<FrontendOpenOrder> frontendOpenOrders = client.getInfo().frontendOpenOrders("0x...");
        System.out.println(JSONUtil.writeValueAsString(frontendOpenOrders));
    }

    @Test
    public void testModifyOrder() {
        //{"status":"err","response":"Cannot modify canceled or filled order"}
        //{"status":"ok","response":{"type":"default"}}
        ModifyOrderRequest req = ModifyOrderRequest.byOid("BTC", 278432125079L);
        req.setBuy(Boolean.FALSE);
        req.setLimitPx("86200.0");
        req.setSz("0.001");
        req.setReduceOnly(Boolean.TRUE);
        req.setOrderType(TriggerOrderType.sl("86200.0", Boolean.TRUE));
        ModifyOrder modifyOrder = client.getExchange().modifyOrder(req);
        System.out.println(modifyOrder);
    }

    @Test
    public void testModifyOrders() {
        ModifyOrderRequest req1 = ModifyOrderRequest.byOid("BTC", 278483307385L);
        req1.setBuy(Boolean.FALSE);
        req1.setLimitPx("85100.0");
        req1.setSz("0.001");
        req1.setReduceOnly(Boolean.TRUE);
        req1.setOrderType(TriggerOrderType.sl("85100.0", Boolean.TRUE));

        ModifyOrderRequest req2 = ModifyOrderRequest.byOid("BTC", 278483307386L);
        req2.setBuy(Boolean.FALSE);
        req2.setLimitPx("89100.0");
        req2.setSz("0.001");
        req2.setReduceOnly(Boolean.TRUE);
        req2.setOrderType(TriggerOrderType.tp("89100.0", Boolean.TRUE));
        ModifyOrder modifyOrder = client.getExchange().modifyOrders(List.of(req1, req2));
        System.out.println(modifyOrder);
    }
}