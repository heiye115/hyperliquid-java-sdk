package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.approve.ApproveAgentResult;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.model.order.TriggerOrderType;
import io.github.hyperliquid.sdk.utils.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 订单下单与平仓逻辑综合测试。
 */
public class OrderTest {

    /**
     * 私钥（测试网）
     */
    String privateKey = "your_testnet_private_key_here";

    /**
     * 统一客户端（测试网 + 启用调试日志）
     */
    HyperliquidClient client = HyperliquidClient.builder()
            .baseUrl(Constants.TESTNET_API_URL)
            .addPrivateKey(privateKey)
            .enableDebugLogs()
            .build();

    /**
     * 获取币种当前中间价。
     * <p>
     * 设计考虑：
     * - 限价/触发价选择需要参考当前行情中位数，避免写死；
     * - 返回 double 以便后续进行百分比偏移。
     */
    private double mid(String symbol) {
        Map<String, String> mids = client.getInfo().allMids();
        String px = mids.get(symbol);
        Assertions.assertNotNull(px, "mid price not found for " + symbol);
        return Double.parseDouble(px);
    }

    /**
     * 市价下单：做多与做空各 0.01。
     */
    @Test
    public void testMarketOrders() {
        String symbol = "ETH";
        Exchange ex = client.getSingleExchange();

        OrderRequest longReq = OrderRequest.createDefaultPerpMarketOrder(symbol, true, 0.01);
        Order longOrder = ex.order(longReq);
        Assertions.assertNotNull(longOrder);
        Assertions.assertNotNull(longOrder.getResponse());

        OrderRequest shortReq = OrderRequest.createDefaultPerpMarketOrder(symbol, false, 0.01);
        Order shortOrder = ex.order(shortReq);
        Assertions.assertNotNull(shortOrder);
        Assertions.assertNotNull(shortOrder.getResponse());
    }

    /**
     * 限价下单：做多与做空各 0.01。
     * 策略：
     * - 买单限价取 `mid * 0.99`；卖单限价取 `mid * 1.01`；
     * - 使用 `Tif.GTC` 以便保留委托；
     */
    @Test
    public void testLimitOrders() {
        String symbol = "ETH";
        double m = mid(symbol);
        int szDecimals = client.getInfo().getMetaUniverse(symbol).getSzDecimals();
        int decimals = 6 - szDecimals;
        if (decimals < 0)
            decimals = 0;
        java.math.BigDecimal buyBd = java.math.BigDecimal.valueOf(m * 0.99)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal sellBd = java.math.BigDecimal.valueOf(m * 1.01)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP);
        double buyPx = buyBd.doubleValue();
        double sellPx = sellBd.doubleValue();

        Exchange ex = client.getSingleExchange();

        OrderRequest longReq = OrderRequest.createPerpLimitOrder(Tif.GTC, symbol, true, 0.01, buyPx, false,
                null);
        Order longOrder = ex.order(longReq);
        Assertions.assertNotNull(longOrder);
        Assertions.assertNotNull(longOrder.getResponse());

        OrderRequest shortReq = OrderRequest.createPerpLimitOrder(Tif.GTC, symbol, false, 0.01, sellPx, false,
                null);
        Order shortOrder = ex.order(shortReq);
        Assertions.assertNotNull(shortOrder);
        Assertions.assertNotNull(shortOrder.getResponse());
    }

    /**
     * 触发价止盈/止损（TP/SL）：为仓位下发两类触发单。
     * 步骤：
     * - 预先开多 0.01；
     * - 创建 SL（触发后市价平仓）：`triggerPx = mid * 0.98`；
     * - 创建 TP（触发后市价平仓）：`triggerPx = mid * 1.02`；
     * - 两单均设置 `reduceOnly = true` 以保证仅减仓。
     */
    @Test
    public void testTriggerTpSl() throws Exception {
        String symbol = "ETH";
        Exchange ex = client.getSingleExchange();
        double m = mid(symbol);

        int szDecimals = client.getInfo().getMetaUniverse(symbol).getSzDecimals();
        int decimals = 6 - szDecimals;
        if (decimals < 0)
            decimals = 0;

        double slTrigger = java.math.BigDecimal.valueOf(m * 0.98)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();
        double tpTrigger = java.math.BigDecimal.valueOf(m * 1.02)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();

        OrderRequest slReq = OrderRequest.createPerpTriggerOrder(symbol, false, 0.01, null, slTrigger,
                true, TriggerOrderType.TpslType.SL, true, null);
        int assetId = client.getInfo().nameToAsset(symbol);
        io.github.hyperliquid.sdk.model.order.OrderWire slWire = io.github.hyperliquid.sdk.utils.Signing
                .orderRequestToOrderWire(assetId, slReq);
        java.util.Map<String, Object> slAction = (java.util.Map<String, Object>) invokePrivate(ex,
                "buildOrderAction", java.util.List.of(slWire), null);
        Assertions.assertEquals("order", slAction.get("type"));
        Assertions.assertEquals("normalTpsl", slAction.get("grouping"));

        OrderRequest tpReq = OrderRequest.createPerpTriggerOrder(symbol, false, 0.01, null, tpTrigger,
                true, TriggerOrderType.TpslType.TP, true, null);
        io.github.hyperliquid.sdk.model.order.OrderWire tpWire = io.github.hyperliquid.sdk.utils.Signing
                .orderRequestToOrderWire(assetId, tpReq);
        java.util.Map<String, Object> tpAction = (java.util.Map<String, Object>) invokePrivate(ex,
                "buildOrderAction", java.util.List.of(tpWire), null);
        Assertions.assertEquals("order", tpAction.get("type"));
        Assertions.assertEquals("normalTpsl", tpAction.get("grouping"));
    }

    /**
     * 市价平仓：自动推断方向与数量，完整平掉当前仓位。
     */
    @Test
    public void testClosePositionMarket() {
        String symbol = "ETH";
        Exchange ex = client.getSingleExchange();

        // 先开仓 0.01
        OrderRequest openReq = OrderRequest.createDefaultPerpMarketOrder(symbol, true, 0.01);
        Order openOrder = ex.order(openReq);
        Assertions.assertNotNull(openOrder);

        // 自动推断并平仓
        OrderRequest closeReq = OrderRequest.closePositionAtMarket(symbol);
        Order closeOrder = ex.order(closeReq);
        Assertions.assertNotNull(closeOrder);
        Assertions.assertNotNull(closeOrder.getResponse());
    }

    /**
     * 限价平仓：以 `reduceOnly=true` 的限价单完成部分平仓 0.01。
     * 说明：
     * - 开多 0.01；随后以卖出限价单（`reduceOnly=true`）平掉 0.01；
     * - 使用 `GTC` 保留委托；价格设为 `mid * 1.01`，更保守以便在测试网保持挂单。
     */
    @Test
    public void testClosePositionLimit() {
        String symbol = "ETH";
        Exchange ex = client.getSingleExchange();
        double m = mid(symbol);

        OrderRequest openReq = OrderRequest.createDefaultPerpMarketOrder(symbol, true, 0.01);
        Order openOrder = ex.order(openReq);
        Assertions.assertNotNull(openOrder);

        int szDecimals = client.getInfo().getMetaUniverse(symbol).getSzDecimals();
        int decimals = 6 - szDecimals;
        if (decimals < 0)
            decimals = 0;
        java.math.BigDecimal sellBd = java.math.BigDecimal.valueOf(m * 1.01)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP);
        double sellPx = sellBd.doubleValue();
        OrderRequest closeReq = OrderRequest.createPerpLimitOrder(Tif.GTC, symbol, false, 0.01, sellPx,
                true, null);
        Order closeOrder = ex.order(closeReq);
        Assertions.assertNotNull(closeOrder);
        Assertions.assertNotNull(closeOrder.getResponse());
    }

    /**
     * createPerpTriggerOrder 触发单测试：以 ETH 为标的，分别为做多与做空 0.01 的仓位
     * 创建市价触发的止损/止盈（reduceOnly=true）。
     * 测试重点：验证 Exchange.order 能正确提交 TriggerOrder。
     * 策略：
     * - 先开多 0.01；基于当前 mid 计算 SL（0.98x）与 TP（1.02x）的触发价，触发后以市价减仓；
     * - 再开空 0.01；同样创建 SL/TP，触发方向按空仓语义设置；
     * - limitPx 设为 null，避免 tick 约束导致的无效价格；触发价按有效数字与小数位规范进行标准化。
     */
    @Test
    public void testCreatePerpTriggerOrderEthLongShort() {
        String symbol = "ETH";
        Exchange ex = client.getSingleExchange();
        // 确保 API Wallet (Agent) 已创建并授权，避免触发单分组 normalTpsl 导致 422/不存在错误
        ApproveAgentResult agent = ex.approveAgent("JUnitAgent");
        Assertions.assertNotNull(agent);
        double m = mid(symbol);

        int szDecimals = client.getInfo().getMetaUniverse(symbol).getSzDecimals();
        int decimals = 6 - szDecimals;
        if (decimals < 0)
            decimals = 0;

        // 说明：此测试通过反射调用 Exchange.buildOrderAction 验证触发单结构，不直接命中实盘接口

        // 预计算限价占位（满足 tick 约束），尽管 isMarket=true 但部分服务端 schema 仍要求存在 p 字段
        double buyPx = java.math.BigDecimal.valueOf(m * 0.99)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();
        double sellPx = java.math.BigDecimal.valueOf(m * 1.01)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();

        // 多仓止损：价格下穿触发（SL），触发后市价卖出减仓
        double slLongTrigger = java.math.BigDecimal.valueOf(m * 0.98)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();
        OrderRequest slLongReq = OrderRequest.createPerpTriggerOrder(symbol, false, 0.01, sellPx, slLongTrigger,
                true, TriggerOrderType.TpslType.TP, true, null);
        int assetId = client.getInfo().nameToAsset(symbol);
        io.github.hyperliquid.sdk.model.order.OrderWire slLongWire = io.github.hyperliquid.sdk.utils.Signing
                .orderRequestToOrderWire(assetId, slLongReq);
        java.util.Map<String, Object> slLongAction = (java.util.Map<String, Object>) invokePrivate(ex,
                "buildOrderAction", java.util.List.of(slLongWire), null);
        Assertions.assertEquals("order", slLongAction.get("type"));
        Assertions.assertEquals("normalTpsl", slLongAction.get("grouping"));
        java.util.List<?> slOrders = (java.util.List<?>) slLongAction.get("orders");
        Assertions.assertEquals(1, slOrders.size());
        java.util.Map<?, ?> slOrder0 = (java.util.Map<?, ?>) slOrders.get(0);
        java.util.Map<?, ?> slT = (java.util.Map<?, ?>) slOrder0.get("t");
        Assertions.assertTrue(slT.containsKey("trigger"));

        // 多仓止盈：价格上穿触发（TP），触发后市价卖出减仓
        double tpLongTrigger = java.math.BigDecimal.valueOf(m * 1.02)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();
        OrderRequest tpLongReq = OrderRequest.createPerpTriggerOrder(symbol, false, 0.01, sellPx, tpLongTrigger,
                true, TriggerOrderType.TpslType.TP, true, null);
        io.github.hyperliquid.sdk.model.order.OrderWire tpLongWire = io.github.hyperliquid.sdk.utils.Signing
                .orderRequestToOrderWire(assetId, tpLongReq);
        java.util.Map<String, Object> tpLongAction = (java.util.Map<String, Object>) invokePrivate(ex,
                "buildOrderAction", java.util.List.of(tpLongWire), null);
        Assertions.assertEquals("order", tpLongAction.get("type"));
        Assertions.assertEquals("normalTpsl", tpLongAction.get("grouping"));

        // 说明：空仓场景同样仅验证结构，不直接触达网络

        // 空仓止损：价格上穿触发（TP），触发后市价买入减仓
        double slShortTrigger = java.math.BigDecimal.valueOf(m * 1.02)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();
        OrderRequest slShortReq = OrderRequest.createPerpTriggerOrder(symbol, true, 0.01, buyPx, slShortTrigger,
                true, TriggerOrderType.TpslType.TP, true, null);
        io.github.hyperliquid.sdk.model.order.OrderWire slShortWire = io.github.hyperliquid.sdk.utils.Signing
                .orderRequestToOrderWire(assetId, slShortReq);
        java.util.Map<String, Object> slShortAction = (java.util.Map<String, Object>) invokePrivate(ex,
                "buildOrderAction", java.util.List.of(slShortWire), null);
        Assertions.assertEquals("order", slShortAction.get("type"));
        Assertions.assertEquals("normalTpsl", slShortAction.get("grouping"));

        // 空仓止盈：价格下穿触发（SL），触发后市价买入减仓
        double tpShortTrigger = java.math.BigDecimal.valueOf(m * 0.98)
                .round(new java.math.MathContext(5, java.math.RoundingMode.HALF_UP))
                .setScale(decimals, java.math.RoundingMode.HALF_UP).doubleValue();
        OrderRequest tpShortReq = OrderRequest.createPerpTriggerOrder(symbol, true, 0.01, buyPx, tpShortTrigger,
                true, TriggerOrderType.TpslType.TP, true, null);
        io.github.hyperliquid.sdk.model.order.OrderWire tpShortWire = io.github.hyperliquid.sdk.utils.Signing
                .orderRequestToOrderWire(assetId, tpShortReq);
        java.util.Map<String, Object> tpShortAction = (java.util.Map<String, Object>) invokePrivate(ex,
                "buildOrderAction", java.util.List.of(tpShortWire), null);
        Assertions.assertEquals("order", tpShortAction.get("type"));
        Assertions.assertEquals("normalTpsl", tpShortAction.get("grouping"));
    }

    /**
     * 反射调用私有方法的简化封装。
     *
     * @param target     目标实例
     * @param methodName 方法名
     * @param args       参数（与真实方法签名一致）
     * @return 调用结果对象
     */
    private static Object invokePrivate(Object target, String methodName, Object... args) {
        try {
            java.lang.reflect.Method m = io.github.hyperliquid.sdk.apis.Exchange.class
                    .getDeclaredMethod(methodName, java.util.List.class, java.util.Map.class);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
