package io.github.hyperliquid.sdk.model.order;

import static io.github.hyperliquid.sdk.model.order.TriggerOrderType.TpslType;

/**
 * 下单请求结构（Java 侧语义化表示）。
 * <p>
 * 说明：
 * - 市价单在协议层以“限价 + IOC”表达，`limitPx` 可为空，价格由业务层根据中间价及滑点计算；
 * - 触发单通过 `orderType.trigger` 承载触发参数；
 * - 最终会被转换为线缆结构并发送（见 `utils.Signing.orderRequestToOrderWire`）。
 */
public class OrderRequest {

    /**
     * 交易品种类型（PERP 永续 / SPOT 现货）。
     * <p>
     * - PERP: 永续合约
     * - SPOT: 现货交易
     * </p>
     */
    private InstrumentType instrumentType;

    /**
     * 币种名称（例如 "ETH"、"BTC"）。
     */
    private String coin;

    /**
     * 是否买入（true=买/做多，false=卖/做空）。
     * <p>
     * 市价平仓场景可为空，交由业务层自动推断。
     * </p>
     */
    private Boolean isBuy;

    /**
     * 下单数量（浮点）。
     * <p>
     * 最终会根据资产的 szDecimals 规范化精度。
     * </p>
     */
    private Double sz;

    /**
     * 限价价格。
     * <p>
     * - 可为空（市价单或触发单的市价执行）
     * - PERP 价格会按"5 位有效数字 + (6 - szDecimals) 小数位"规范化
     * - SPOT 价格会按"5 位有效数字 + (8 - szDecimals) 小数位"规范化
     * </p>
     */
    private Double limitPx;

    /**
     * 订单类型：限价（TIF）或触发（triggerPx/isMarket/tpsl）。
     * <p>
     * 可为空表示普通限价/市价默认行为。
     * </p>
     */
    private OrderType orderType;

    /**
     * 仅减仓标记（true 表示不会增加仓位）。
     * <p>
     * 用于平仓或触发减仓，防止反向开仓。
     * </p>
     */
    private Boolean reduceOnly;

    /**
     * 客户端订单 ID（Cloid），可为空。
     * <p>
     * 用于幂等性保证和后续撤单操作。
     * </p>
     */
    private Cloid cloid;

    /**
     * 市价下单滑点比例（例如 0.05 表示 5%）。
     * <p>
     * 仅用于业务层模拟"市价=带滑点的 IOC 限价"时计算占位价格。
     * 默认值为 0.05（5%）。
     * </p>
     */
    private Double slippage = 0.05;

    /**
     * 订单过期时间（毫秒）。
     * <p>
     * - 若 < 1,000,000,000,000（即小于 1e12），则视为相对时间，实际过期时间为 nonce + expiresAfter；
     * - 否则视为绝对时间戳（UTC）。
     * - 默认值：120,000ms（120 秒）。
     * </p>
     */
    private Long expiresAfter;

    public OrderRequest() {
    }

    /**
     * 构造下单请求。
     *
     * @param coin       币种名称（如 "ETH"）
     * @param isBuy      是否买入
     * @param sz         数量
     * @param limitPx    限价价格（可为 null）
     * @param orderType  订单类型（可为 null）
     * @param reduceOnly 是否只减仓
     * @param cloid      客户端订单 ID（可为 null）
     */
    public OrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx,
                        OrderType orderType, Boolean reduceOnly, Cloid cloid) {
        this.instrumentType = instrumentType;
        this.coin = coin;
        this.isBuy = isBuy;
        this.sz = sz;
        this.limitPx = limitPx;
        this.orderType = orderType;
        this.reduceOnly = reduceOnly;
        this.cloid = cloid;
    }

    public OrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx,
                        OrderType orderType, Boolean reduceOnly, Cloid cloid, Double slippage) {
        this.instrumentType = instrumentType;
        this.coin = coin;
        this.isBuy = isBuy;
        this.sz = sz;
        this.limitPx = limitPx;
        this.orderType = orderType;
        this.reduceOnly = reduceOnly;
        this.cloid = cloid;
        this.slippage = slippage;
    }

    // ========================================
    // Builder 模式：链式调用构建订单
    // ========================================

    /**
     * 创建订单构建器（Builder 模式）。
     * <p>
     * 使用示例：
     * <pre>
     * // 限价开仓
     * OrderRequest req = OrderRequest.builder()
     *     .perp("ETH")
     *     .buy(0.1)
     *     .limitPrice(3000.0)
     *     .gtc()
     *     .build();
     *
     * // 市价开仓
     * OrderRequest req = OrderRequest.builder()
     *     .perp("ETH")
     *     .sell(0.1)
     *     .market()
     *     .build();
     *
     * // 条件单：价格突破 2950 时买入
     * OrderRequest req = OrderRequest.builder()
     *     .perp("ETH")
     *     .buy(0.1)
     *     .stopPrice(2950.0)  // 向上突破触发
     *     .limitPrice(3000.0)
     *     .build();
     *
     * // 开仓+止盈止损（需配合 bulkOrders）
     * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
     *     .perp("ETH")
     *     .buy(0.1)
     *     .entryPrice(3500.0)
     *     .takeProfit(3600.0)
     *     .stopLoss(3400.0)
     *     .buildNormalTpsl();
     * // 自动推断 grouping="normalTpsl"
     * JsonNode result = exchange.bulkOrders(orderGroup);
     * </pre>
     *
     * @return OrderBuilder 实例
     */
    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    /**
     * 创建带止盈止损的开仓订单构建器。
     *
     * @return OrderWithTpSlBuilder 实例
     */
    public static OrderWithTpSlBuilder entryWithTpSl() {
        return new OrderWithTpSlBuilder();
    }

    /**
     * 永续开仓
     **/

    // Getter and Setter methods
    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public Boolean getIsBuy() {
        return isBuy;
    }

    public void setIsBuy(Boolean isBuy) {
        this.isBuy = isBuy;
    }

    public Double getSz() {
        return sz;
    }

    public void setSz(Double sz) {
        this.sz = sz;
    }

    public Double getLimitPx() {
        return limitPx;
    }

    public void setLimitPx(Double limitPx) {
        this.limitPx = limitPx;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public Boolean getReduceOnly() {
        return reduceOnly;
    }

    public void setReduceOnly(Boolean reduceOnly) {
        this.reduceOnly = reduceOnly;
    }

    public Cloid getCloid() {
        return cloid;
    }

    public void setCloid(Cloid cloid) {
        this.cloid = cloid;
    }

    public Double getSlippage() {
        return slippage;
    }

    public void setSlippage(Double slippage) {
        this.slippage = slippage;
    }

    public Long getExpiresAfter() {
        return expiresAfter;
    }

    public void setExpiresAfter(Long expiresAfter) {
        this.expiresAfter = expiresAfter;
    }

    /**
     * 静态内部类，用于快速创建开仓订单请求。
     */
    public static class Open {
        /**
         * 创建市价开仓订单。
         *
         * @param coin  币种名称
         * @param isBuy 是否买入
         * @param sz    数量
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, boolean isBuy, double sz) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
            req.setSz(sz);
            req.setReduceOnly(false);
            // 设置为 IOC 市价单
            LimitOrderType limitOrderType = new LimitOrderType(Tif.IOC);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }

        /**
         * 创建限价开仓订单。
         *
         * @param tif     时间生效方式
         * @param coin    币种名称
         * @param isBuy   是否买入
         * @param sz      数量
         * @param limitPx 限价
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(Tif tif, String coin, boolean isBuy, double sz, double limitPx) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
            req.setSz(sz);
            req.setLimitPx(limitPx);
            req.setReduceOnly(false);
            // 设置订单类型
            LimitOrderType limitOrderType = new LimitOrderType(tif);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }

        /**
         * 创建触发订单。
         *
         * @param coin      币种名称
         * @param isBuy     是否买入
         * @param sz        数量
         * @param triggerPx 触发价格
         * @param limitPx   限价（若为市价触发则为null）
         * @param isMarket  是否市价触发
         * @param tpsl      TP/SL类型
         * @return OrderRequest 实例
         */
        public static OrderRequest trigger(String coin, boolean isBuy, double sz, double triggerPx, Double limitPx, boolean isMarket, TpslType tpsl) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
            req.setSz(sz);
            req.setLimitPx(limitPx);
            req.setReduceOnly(false);
            // 设置订单类型
            TriggerOrderType triggerOrderType = new TriggerOrderType(triggerPx, isMarket, tpsl);
            OrderType orderType = new OrderType(triggerOrderType);
            req.setOrderType(orderType);
            return req;
        }
    }

    /**
     * 静态内部类，用于快速创建平仓订单请求。
     */
    public static class Close {
        /**
         * 创建市价平仓订单（自动推断方向）。
         *
         * @param coin  币种名称
         * @param sz    数量
         * @param cloid 客户端订单 ID
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, Double sz, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setSz(sz);
            req.setReduceOnly(true);
            req.setCloid(cloid);
            // 设置为 IOC 市价单
            LimitOrderType limitOrderType = new LimitOrderType(Tif.IOC);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }

        /**
         * 创建市价平仓订单（指定方向）。
         *
         * @param coin  币种名称
         * @param isBuy 是否买入
         * @param sz    数量
         * @param cloid 客户端订单 ID
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, boolean isBuy, double sz, Cloid cloid) {
            OrderRequest req = market(coin, sz, cloid);
            req.setIsBuy(isBuy);
            return req;
        }

        /**
         * 创建市价全平订单。
         *
         * @param coin 币种名称
         * @return OrderRequest 实例
         */
        public static OrderRequest positionAtMarketAll(String coin) {
            return market(coin, null, null);
        }

        /**
         * 创建限价平仓订单。
         *
         * @param tif     时间生效方式
         * @param coin    币种名称
         * @param sz      数量
         * @param limitPx 限价
         * @param cloid   客户端订单 ID
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(Tif tif, String coin, double sz, double limitPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(null); // 将在 prepareRequest 中推断
            req.setSz(sz);
            req.setLimitPx(limitPx);
            req.setReduceOnly(true);
            req.setCloid(cloid);
            // 设置订单类型
            LimitOrderType limitOrderType = new LimitOrderType(tif);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }
    }
}
