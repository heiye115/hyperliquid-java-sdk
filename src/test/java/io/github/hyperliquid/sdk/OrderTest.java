package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.model.order.*;
import org.junit.jupiter.api.Test;

/**
 * 订单下单与平仓逻辑综合测试。
 */
public class OrderTest {

    /**
     * 私钥（测试网）
     */
    String privateKey = "your_private_key_here";

    /**
     * 统一客户端（测试网 + 启用调试日志）
     */
    HyperliquidClient client = HyperliquidClient.builder()
            .testNetUrl()
            .addPrivateKey(privateKey)
            .enableDebugLogs()
            .build();

    /**
     * 市价下单
     **/
    @Test
    public void testMarketOrder() {
        OrderRequest req = OrderRequest.Open.market("ETH", true, "0.01");
        Order order = client.getSingleExchange().order(req);
        System.out.println(order);
    }

    /**
     * 市价平仓
     **/
    @Test
    public void testMarketCloseOrder() {
        OrderRequest req = OrderRequest.Close.market("ETH", "0.01", Cloid.auto());
        Order order = client.getSingleExchange().order(req);
        System.out.println(order);
    }

    /**
     * 全部市价平仓
     **/
    @Test
    public void testMarketCloseAllOrder() {
        Order order = client.getSingleExchange().closePositionAtMarketAll("ETH");
        System.out.println(order);
    }

    /**
     * 限价下单
     **/
    @Test
    public void testLimitOrder() {
        OrderRequest req = OrderRequest.Open.limit(Tif.GTC, "ETH", true, "0.01", "1800.0");
        Order order = client.getSingleExchange().order(req);
        System.out.println(order);
    }

    /**
     * 限价平仓
     **/
    @Test
    public void testLimitCloseOrder() {
        OrderRequest req = OrderRequest.Close.limit(Tif.GTC, "ETH", "0.01", "4000.0", Cloid.auto());
        Order order = client.getSingleExchange().order(req);
        System.out.println(order);
    }

    /**
     * 全部限价平仓
     **/
    @Test
    public void testLimitCloseAllOrder() {
        Order order = client.getSingleExchange().closePositionLimitAll(Tif.GTC, "ETH", "4000.0", Cloid.auto());
        System.out.println(order);
    }

    @Test
    public void testCancel() {
        JsonNode node = client.getSingleExchange().cancel("ETH", 42988070692L);
        System.out.println(node.toPrettyString());
    }


    @Test
    public void testMarketOrderALL() {
        OrderRequest req = OrderRequest.Open.market("ETH", true, "0.01");
        Order order = client.getSingleExchange().order(req);
        System.out.println(order);
        Order closeOrder = client.getSingleExchange().closePositionAtMarketAll("ETH");
        System.out.println(closeOrder);
    }

    @Test
    public void testTriggerOrderALL() {
        // 使用 breakoutAbove 替代已删除的 trigger 方法
        OrderRequest req = OrderRequest.Open.breakoutAbove("ETH", "0.01", "4000.0");
        Order order = client.getSingleExchange().order(req);
        System.out.println(order);
    }
}

 
