package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.hyperliquid.sdk.model.info.ClearinghouseState;
import io.github.hyperliquid.sdk.model.info.UpdateLeverage;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.junit.jupiter.api.Test;


public class SimpleTest {

    String privateKey = "your_private_key_here";
    HyperliquidClient client = HyperliquidClient.builder().addPrivateKey(privateKey).enableDebugLogs().build();

    @Test
    public void updateLeverage() {
        UpdateLeverage leverage = client.getSingleExchange().updateLeverage("BTC", true, 10);
        System.out.println(leverage);

        /*return to the result:
         POST: https://api.hyperliquid.xyz/exchange
         Request: {"action":{"type":"updateLeverage","asset":0,"isCross":true,"leverage":10},"nonce":1762549309755,"signature":{"r":"xxx","s":"xxx","v":27},"vaultAddress":null,"expiresAfter":null}
         Response: {"status":"ok","response":{"type":"default"}}
         UpdateLeverage{status='ok', response=Response{type='default'}}
        * */
    }

    @Test
    public void clearinghouseState() {
        ClearinghouseState clearinghouseState = client.getInfo().clearinghouseState(client.getSingleAddress());
        System.out.println(clearinghouseState);
    }

    /**
     * 测试限价单类型Gtc
     */
    @Test
    public void testLimitOrderGtc() {
        System.out.println("测试限价单类型: Gtc");
        Long cloid = System.currentTimeMillis();
        OrderRequest req = OrderRequest.createDefaultPerpLimitOrder(Tif.GTC, "ETH", true, 0.01, 2000.0);
        Order order = client.getSingleExchange().order(req);
        System.out.println("限价单响应: " + order);
    }

    /**
     * 测试限价单类型Alo
     */
    @Test
    public void testLimitOrderAlo() {
        System.out.println("测试限价单类型: Alo");
        Long cloid = System.currentTimeMillis();
        OrderRequest req = OrderRequest.createDefaultPerpLimitOrder(Tif.ALO, "ETH", true, 0.01, 2000.0);
        Order order = client.getSingleExchange().order(req);
        System.out.println("限价单响应: " + order);
    }

    /**
     * 测试限价单类型Ioc
     */
    @Test
    public void testLimitOrderIoc() throws JsonProcessingException {
        System.out.println("测试限价单类型: Ioc");
        Long cloid = System.currentTimeMillis();
        OrderRequest req = OrderRequest.createDefaultPerpLimitOrder(Tif.IOC, "ETH", true, 0.01, 2000.0);
        Order order = client.getSingleExchange().order(req);
        System.out.println("限价单响应: " + JSONUtil.writeValueAsString(order));
    }

    /**
     * 测试市价单
     */
    @Test
    public void testMarketOrder() {
        //cloid客户端ID方式
        //Long cloid = System.currentTimeMillis();
        //OrderRequest req = OrderRequest.createPerpMarketOrder("ETH", true, 0.01, cloid);

        //无客户端ID方式
        OrderRequest req = OrderRequest.createDefaultPerpMarketOrder("ETH", true, 0.01);
        Order order = client.getSingleExchange().order(req);
        System.out.println("市价单响应: " + order);
    }
}
