package io.github.hyperliquid.sdk.model.order;

import static io.github.hyperliquid.sdk.model.order.TriggerOrderType.TpslType;

/**
 * Order request structure (Java side semantic representation).
 * Note:
 * - Market orders are expressed as "limit + IOC" at protocol level, `limitPx` can be empty, price is calculated by business layer based on mid price and slippage;
 * - Trigger orders carry trigger parameters via `orderType.trigger`;
 * - Will be converted to wire structure and sent (see `utils.Signing.orderRequestToOrderWire`).
 */
public class OrderRequest {

    /**
     * Instrument type (PERP perpetual / SPOT spot).
     * <p>
     * - PERP: perpetual contract
     * - SPOT: spot trading
     * </p>
     */
    private InstrumentType instrumentType;

    /**
     * Currency name (e.g., "ETH", "BTC").
     */
    private String coin;

    /**
     * Whether to buy (true=buy/long, false=sell/short).
     * <p>
     * Can be empty for market close scenarios, inferred by business layer.
     * </p>
     */
    private Boolean isBuy;

    /**
     * Order quantity (string).
     * <p>
     * Use string representation to avoid floating-point precision issues.
     * Examples: "0.1", "0.123456789"
     * </p>
     */
    private String sz;

    /**
     * Limit price (string).
     * <p>
     * - Can be empty (market order or trigger order market execution)
     * - Use string representation to avoid floating-point precision issues
     * - Examples: "3500.0", "3500.123456"
     * </p>
     */
    private String limitPx;

    /**
     * Order type: limit (TIF) or trigger (triggerPx/isMarket/tpsl).
     * <p>
     * Can be empty to represent default limit/market behavior.
     * </p>
     */
    private OrderType orderType;

    /**
     * Reduce-only flag (true means will not increase position).
     * <p>
     * Used for closing positions or trigger reductions to prevent reverse opening.
     * </p>
     */
    private Boolean reduceOnly;

    /**
     * Client order ID (Cloid), can be empty.
     * <p>
     * Used for idempotency and subsequent cancellation operations.
     * </p>
     */
    private Cloid cloid;

    /**
     * Market order slippage ratio (string, e.g., "0.05" for 5%).
     * <p>
     * Only used by business layer to simulate "market = IOC limit with slippage" when calculating placeholder price.
     * Default value: "0.05" (5%).
     * </p>
     */
    private String slippage = "0.05";

    /**
     * Order expiration time (milliseconds).
     * <p>
     * - If < 1,000,000,000,000 (i.e., less than 1e12), treated as relative time, actual expiration time is nonce + expiresAfter;
     * - Otherwise treated as absolute timestamp (UTC).
     * - Default value: 120,000ms (120 seconds).
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
     * @param sz         数量（字符串）
     * @param limitPx    限价价格（字符串，可为 null）
     * @param orderType  订单类型（可为 null）
     * @param reduceOnly 是否只减仓
     * @param cloid      客户端订单 ID（可为 null）
     */
    public OrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, String sz, String limitPx,
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

    public OrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, String sz, String limitPx,
                        OrderType orderType, Boolean reduceOnly, Cloid cloid, String slippage) {
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

    public String getSz() {
        return sz;
    }

    public void setSz(String sz) {
        this.sz = sz;
    }

    public String getLimitPx() {
        return limitPx;
    }

    public void setLimitPx(String limitPx) {
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

    public String getSlippage() {
        return slippage;
    }

    public void setSlippage(String slippage) {
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
        // ========================================
        // 永续合约 - 市价开仓
        // ========================================

        /**
         * 创建永续合约市价开仓订单（无 cloid）。
         *
         * @param coin  币种名称
         * @param isBuy 是否买入
         * @param sz    数量（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz) {
            return market(coin, isBuy, sz, null);
        }

        /**
         * 创建永续合约市价开仓订单。
         *
         * @param coin  币种名称
         * @param isBuy 是否买入
         * @param sz    数量（字符串）
         * @param cloid 客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz, Cloid cloid) {
            OrderRequest req = createMarket(InstrumentType.PERP, coin, isBuy, sz);
            req.setCloid(cloid);
            return req;
        }

        // ========================================
        // 永续合约 - 限价开仓
        // ========================================

        /**
         * 创建永续合约限价开仓订单（默认 GTC，无 cloid）。
         *
         * @param coin    币种名称
         * @param isBuy   是否买入
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(String coin, boolean isBuy, String sz, String limitPx) {
            return limit(Tif.GTC, coin, isBuy, sz, limitPx, null);
        }

        /**
         * 创建永续合约限价开仓订单（默认 GTC）。
         *
         * @param coin    币种名称
         * @param isBuy   是否买入
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @param cloid   客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(String coin, boolean isBuy, String sz, String limitPx, Cloid cloid) {
            return limit(Tif.GTC, coin, isBuy, sz, limitPx, cloid);
        }

        /**
         * 创建永续合约限价开仓订单（无 cloid）。
         *
         * @param tif     时间生效方式
         * @param coin    币种名称
         * @param isBuy   是否买入
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(Tif tif, String coin, boolean isBuy, String sz, String limitPx) {
            return limit(tif, coin, isBuy, sz, limitPx, null);
        }

        /**
         * 创建永续合约限价开仓订单。
         *
         * @param tif     时间生效方式
         * @param coin    币种名称
         * @param isBuy   是否买入
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @param cloid   客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(Tif tif, String coin, boolean isBuy, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.PERP, tif, coin, isBuy, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        // ========================================
        // 永续合约 - 触发单（突破开仓）
        // ========================================

        /**
         * 创建突破开仓订单（价格向上突破时触发，市价执行，无 cloid）。
         * 使用场景：做多突破策略，当价格突破关键阻力位时追涨买入。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 触发价格（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest breakoutAbove(String coin, String sz, String triggerPx) {
            return breakoutAbove(coin, sz, triggerPx, null);
        }

        /**
         * 创建突破开仓订单（价格向上突破时触发，市价执行）。
         * 使用场景：做多突破策略，当价格突破关键阻力位时追涨买入。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 触发价格（字符串）
         * @param cloid     客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest breakoutAbove(String coin, String sz, String triggerPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(true);
            req.setSz(sz);
            req.setLimitPx(null);
            req.setReduceOnly(false);
            req.setCloid(cloid);
            TriggerOrderType triggerOrderType = new TriggerOrderType(triggerPx, true, TpslType.TP);
            OrderType orderType = new OrderType(triggerOrderType);
            req.setOrderType(orderType);
            return req;
        }

        /**
         * 创建突破开仓订单（价格向下跌破时触发，市价执行，无 cloid）。
         * 使用场景：做空突破策略，当价格跌破关键支撑位时追空卖出。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 触发价格（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest breakoutBelow(String coin, String sz, String triggerPx) {
            return breakoutBelow(coin, sz, triggerPx, null);
        }

        /**
         * 创建突破开仓订单（价格向下跌破时触发，市价执行）。
         * <p>
         * 使用场景：做空突破策略，当价格跌破关键支撑位时追空卖出。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 触发价格（字符串）
         * @param cloid     客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest breakoutBelow(String coin, String sz, String triggerPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(false);
            req.setSz(sz);
            req.setLimitPx(null);
            req.setReduceOnly(false);
            req.setCloid(cloid);
            TriggerOrderType triggerOrderType = new TriggerOrderType(triggerPx, true, TpslType.SL);
            OrderType orderType = new OrderType(triggerOrderType);
            req.setOrderType(orderType);
            return req;
        }

        // ========================================
        // 现货 - 便捷方法
        // ========================================

        /**
         * 创建现货市价买入订单（无 cloid）。
         *
         * @param coin 币种名称
         * @param sz   数量（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotMarketBuy(String coin, String sz) {
            return spotMarketBuy(coin, sz, null);
        }

        /**
         * 创建现货市价买入订单。
         *
         * @param coin  币种名称
         * @param sz    数量（字符串）
         * @param cloid 客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotMarketBuy(String coin, String sz, Cloid cloid) {
            OrderRequest req = createMarket(InstrumentType.SPOT, coin, true, sz);
            req.setCloid(cloid);
            return req;
        }

        /**
         * 创建现货市价卖出订单（无 cloid）。
         *
         * @param coin 币种名称
         * @param sz   数量（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotMarketSell(String coin, String sz) {
            return spotMarketSell(coin, sz, null);
        }

        /**
         * 创建现货市价卖出订单。
         *
         * @param coin  币种名称
         * @param sz    数量（字符串）
         * @param cloid 客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotMarketSell(String coin, String sz, Cloid cloid) {
            OrderRequest req = createMarket(InstrumentType.SPOT, coin, false, sz);
            req.setCloid(cloid);
            return req;
        }

        /**
         * 创建现货限价买入订单（无 cloid）。
         *
         * @param coin    币种名称
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotLimitBuy(String coin, String sz, String limitPx) {
            return spotLimitBuy(coin, sz, limitPx, null);
        }

        /**
         * 创建现货限价买入订单。
         *
         * @param coin    币种名称
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @param cloid   客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotLimitBuy(String coin, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.SPOT, Tif.GTC, coin, true, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        /**
         * 创建现货限价卖出订单（无 cloid）。
         *
         * @param coin    币种名称
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotLimitSell(String coin, String sz, String limitPx) {
            return spotLimitSell(coin, sz, limitPx, null);
        }

        /**
         * 创建现货限价卖出订单。
         *
         * @param coin    币种名称
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @param cloid   客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest spotLimitSell(String coin, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.SPOT, Tif.GTC, coin, false, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        // ========================================
        // 内部工具方法（私有）
        // ========================================

        /**
         * 创建市价开仓订单（内部方法）。
         *
         * @param instrumentType 交易品种类型
         * @param coin           币种名称
         * @param isBuy          是否买入
         * @param sz             数量（字符串）
         * @return OrderRequest 实例
         */
        private static OrderRequest createMarket(InstrumentType instrumentType, String coin, boolean isBuy, String sz) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(instrumentType);
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
         * 创建限价开仓订单（内部方法）。
         *
         * @param instrumentType 交易品种类型
         * @param tif            时间生效方式
         * @param coin           币种名称
         * @param isBuy          是否买入
         * @param sz             数量（字符串）
         * @param limitPx        限价（字符串）
         * @return OrderRequest 实例
         */
        private static OrderRequest createLimit(InstrumentType instrumentType, Tif tif, String coin, boolean isBuy, String sz, String limitPx) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(instrumentType);
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
    }

    /**
     * 静态内部类，用于快速创建平仓订单请求。
     */
    public static class Close {

        // ========================================
        // 市价平仓
        // ========================================

        /**
         * 创建市价平仓订单（自动推断方向，无 cloid）。
         *
         * @param coin 币种名称
         * @param sz   数量（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, String sz) {
            return market(coin, sz, null);
        }

        /**
         * 创建市价平仓订单（自动推断方向）。
         *
         * @param coin  币种名称
         * @param sz    数量（字符串）
         * @param cloid 客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, String sz, Cloid cloid) {
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
         * 创建市价平仓订单（指定方向，无 cloid）。
         *
         * @param coin  币种名称
         * @param isBuy 是否买入
         * @param sz    数量（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz) {
            return market(coin, isBuy, sz, null);
        }

        /**
         * 创建市价平仓订单（指定方向）。
         *
         * @param coin  币种名称
         * @param isBuy 是否买入
         * @param sz    数量（字符串）
         * @param cloid 客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz, Cloid cloid) {
            OrderRequest req = market(coin, sz, cloid);
            req.setIsBuy(isBuy);
            return req;
        }

        /**
         * 创建市价全平订单（无 cloid）。
         *
         * @param coin 币种名称
         * @return OrderRequest 实例
         */
        public static OrderRequest marketAll(String coin) {
            return marketAll(coin, null);
        }

        /**
         * 创建市价全平订单。
         *
         * @param coin  币种名称
         * @param cloid 客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest marketAll(String coin, Cloid cloid) {
            return market(coin, null, cloid);
        }

        // ========================================
        // 限价平仓
        // ========================================

        /**
         * 创建限价平仓订单（默认 GTC，无 cloid）。
         *
         * @param coin    币种名称
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(String coin, String sz, String limitPx) {
            return limit(Tif.GTC, coin, null, sz, limitPx, null);
        }

        /**
         * 创建限价平仓订单（默认 GTC）。
         *
         * @param coin    币种名称
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @param cloid   客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(String coin, String sz, String limitPx, Cloid cloid) {
            return limit(Tif.GTC, coin, null, sz, limitPx, cloid);
        }

        /**
         * 创建限价平仓订单。
         *
         * @param tif     时间生效方式
         * @param coin    币种名称
         * @param isBuy   是否买入
         * @param sz      数量（字符串）
         * @param limitPx 限价（字符串）
         * @param cloid   客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest limit(Tif tif, String coin, Boolean isBuy, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
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

        // ========================================
        // 止盈止损平仓（便捷方法）
        // ========================================

        /**
         * 创建止盈平仓订单（价格向上突破时触发，市价执行，无 cloid）。
         * 使用场景：多仓止盈，当价格达到目标位时自动平仓锁定利润。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 止盈触发价格（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest takeProfit(String coin, Boolean isBuy, String sz, String triggerPx) {
            return takeProfit(coin, isBuy, sz, triggerPx, null);
        }

        /**
         * 创建止盈平仓订单（价格向上突破时触发，市价执行）。
         * <p>
         * 使用场景：多仓止盈，当价格达到目标位时自动平仓锁定利润。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 止盈触发价格（字符串）
         * @param cloid     客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest takeProfit(String coin, Boolean isBuy, String sz, String triggerPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
            req.setSz(sz);
            req.setLimitPx(null);
            req.setReduceOnly(true);
            req.setCloid(cloid);
            TriggerOrderType triggerOrderType = new TriggerOrderType(triggerPx, true, TpslType.TP);
            OrderType orderType = new OrderType(triggerOrderType);
            req.setOrderType(orderType);
            return req;
        }

        /**
         * 创建止损平仓订单（价格向下跌破时触发，市价执行，无 cloid）。
         * 使用场景：多仓止损，当价格跌破止损位时自动平仓限制亏损。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 止损触发价格（字符串）
         * @return OrderRequest 实例
         */
        public static OrderRequest stopLoss(String coin, Boolean isBuy, String sz, String triggerPx) {
            return stopLoss(coin, isBuy, sz, triggerPx, null);
        }

        /**
         * 创建止损平仓订单（价格向下跌破时触发，市价执行）。
         * 使用场景：多仓止损，当价格跌破止损位时自动平仓限制亏损。
         *
         * @param coin      币种名称
         * @param sz        数量（字符串）
         * @param triggerPx 止损触发价格（字符串）
         * @param cloid     客户端订单 ID（可为 null）
         * @return OrderRequest 实例
         */
        public static OrderRequest stopLoss(String coin, Boolean isBuy, String sz, String triggerPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
            req.setSz(sz);
            req.setLimitPx(null);
            req.setReduceOnly(true);
            req.setCloid(cloid);
            TriggerOrderType triggerOrderType = new TriggerOrderType(triggerPx, true, TpslType.SL);
            OrderType orderType = new OrderType(triggerOrderType);
            req.setOrderType(orderType);
            return req;
        }
    }
}
