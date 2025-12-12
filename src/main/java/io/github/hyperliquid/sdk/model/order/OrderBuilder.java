package io.github.hyperliquid.sdk.model.order;

/**
 * OrderRequest builder, providing convenient chain call API.
 * <p>
 * Usage examples:
 * <pre>
 * // 1. Limit order open
 * OrderRequest req = OrderRequest.builder()
 *     .perp("ETH")
 *     .buy(0.1)
 *     .limitPrice(3000.0)
 *     .gtc()
 *     .build();
 *
 * // 2. Market order open
 * OrderRequest req = OrderRequest.builder()
 *     .spot("PURR")
 *     .sell(100.0)
 *     .market()
 *     .build();
 *
 * // 3. Conditional order: buy when price breaks above 2950
 * OrderRequest req = OrderRequest.builder()
 *     .perp("ETH")
 *     .buy(0.1)
 *     .stopAbove(2950.0)  // trigger on upward breakout
 *     .limitPrice(3000.0)
 *     .build();
 *
 * // 4. Conditional order: sell when price breaks below 3100
 * OrderRequest req = OrderRequest.builder()
 *     .perp("ETH")
 *     .sell(0.1)
 *     .stopBelow(3100.0)  // trigger on downward breakdown
 *     .limitPrice(3050.0)
 *     .build();
 *
 * // 5. Close position take-profit (requires existing long position)
 * OrderRequest req = OrderRequest.builder()
 *     .perp("ETH")
 *     .sell(0.5)
 *     .stopAbove(3600.0)  // take-profit trigger price
 *     .marketTrigger()    // execute at market price after trigger
 *     .reduceOnly()
 *     .build();
 * </pre>
 */
public class OrderBuilder {
    /**
     * Instrument type (PERP or SPOT)
     */
    private InstrumentType instrumentType;

    /**
     * Currency name
     */
    private String coin;

    /**
     * Whether to buy (true=buy/long, false=sell/short)
     */
    private Boolean isBuy;

    /**
     * Order quantity (string)
     */
    private String sz;

    /**
     * Limit price (string)
     */
    private String limitPx;

    /**
     * Order type (limit or trigger)
     */
    private OrderType orderType;

    /**
     * Reduce-only flag (default false)
     */
    private Boolean reduceOnly = false;

    /**
     * Client order ID
     */
    private Cloid cloid;

    /**
     * Market order slippage ratio (string)
     */
    private String slippage;

    // Trigger order parameters
    /**
     * Trigger price (string)
     */
    private String triggerPx;

    /**
     * Whether to execute at market price after trigger
     */
    private Boolean isMarketTrigger;

    /**
     * Trigger direction type (TP=break above, SL=break below)
     */
    private TriggerOrderType.TpslType tpsl;

    /**
     * Order expiration time (milliseconds)
     */
    private Long expiresAfter;

    public OrderBuilder() {
    }

    // ========================================
    // 7. 过期时间
    // ========================================

    /**
     * 设置订单过期时间。
     *
     * @param expiresAfter 过期时间（毫秒）
     * @return this
     */
    public OrderBuilder expiresAfter(Long expiresAfter) {
        this.expiresAfter = expiresAfter;
        return this;
    }

    // ========================================
    // 1. 交易品种
    // ========================================

    /**
     * 设置为永续合约。
     *
     * @param coin 币种名称（如 "ETH"）
     * @return this
     */
    public OrderBuilder perp(String coin) {
        this.instrumentType = InstrumentType.PERP;
        this.coin = coin;
        return this;
    }

    /**
     * 设置为现货。
     *
     * @param coin 币种名称（如 "PURR"）
     * @return this
     */
    public OrderBuilder spot(String coin) {
        this.instrumentType = InstrumentType.SPOT;
        this.coin = coin;
        return this;
    }

    // ========================================
    // 2. 方向与数量
    // ========================================

    /**
     * 买入指定数量。
     *
     * @param sz 数量（字符串）
     * @return this
     */
    public OrderBuilder buy(String sz) {
        this.isBuy = true;
        this.sz = sz;
        return this;
    }

    /**
     * 卖出指定数量。
     *
     * @param sz 数量（字符串）
     * @return this
     */
    public OrderBuilder sell(String sz) {
        this.isBuy = false;
        this.sz = sz;
        return this;
    }

    // ========================================
    // 3. 价格设置
    // ========================================

    /**
     * 设置限价价格。
     *
     * @param limitPx 限价（字符串）
     * @return this
     */
    public OrderBuilder limitPrice(String limitPx) {
        this.limitPx = limitPx;
        return this;
    }

    /**
     * 市价单（无需设置限价，内部会自动计算占位价）。
     *
     * @return this
     */
    public OrderBuilder market() {
        this.limitPx = null;
        this.orderType = new OrderType(new LimitOrderType(Tif.IOC));
        return this;
    }

    /**
     * 市价单，自定义滑点。
     *
     * @param slippage 滑点比例（字符串，例如 "0.05" 表示 5%）
     * @return this
     */
    public OrderBuilder market(String slippage) {
        this.limitPx = null;
        this.slippage = slippage;
        this.orderType = new OrderType(new LimitOrderType(Tif.IOC));
        return this;
    }

    // ========================================
    // 4. 触发条件（Trigger）
    // ========================================

    /**
     * 价格向上突破时触发（适合止盈或做多突破）。
     *
     * @param triggerPx 触发价格（字符串）
     * @return this
     */
    public OrderBuilder stopAbove(String triggerPx) {
        this.triggerPx = triggerPx;
        this.tpsl = TriggerOrderType.TpslType.TP;
        this.isMarketTrigger = false; // 默认触发后挂限价单
        return this;
    }

    /**
     * 价格向下跌破时触发（适合止损或做空突破）。
     *
     * @param triggerPx 触发价格（字符串）
     * @return this
     */
    public OrderBuilder stopBelow(String triggerPx) {
        this.triggerPx = triggerPx;
        this.tpsl = TriggerOrderType.TpslType.SL;
        this.isMarketTrigger = false;
        return this;
    }

    /**
     * 触发后以市价成交（需先调用 stopAbove 或 stopBelow）。
     *
     * @return this
     */
    public OrderBuilder marketTrigger() {
        this.isMarketTrigger = true;
        return this;
    }

    // ========================================
    // 5. TIF 策略
    // ========================================

    /**
     * Good Til Cancel（GTC）。
     *
     * @return this
     */
    public OrderBuilder gtc() {
        if (this.orderType == null || this.orderType.getLimit() == null) {
            this.orderType = new OrderType(new LimitOrderType(Tif.GTC));
        }
        return this;
    }

    /**
     * Immediate or Cancel（IOC）。
     *
     * @return this
     */
    public OrderBuilder ioc() {
        if (this.orderType == null || this.orderType.getLimit() == null) {
            this.orderType = new OrderType(new LimitOrderType(Tif.IOC));
        }
        return this;
    }

    /**
     * Add Liquidity Only（ALO）。
     *
     * @return this
     */
    public OrderBuilder alo() {
        if (this.orderType == null || this.orderType.getLimit() == null) {
            this.orderType = new OrderType(new LimitOrderType(Tif.ALO));
        }
        return this;
    }

    // ========================================
    // 6. 其他选项
    // ========================================

    /**
     * 仅减仓（平仓单）。
     *
     * @return this
     */
    public OrderBuilder reduceOnly() {
        this.reduceOnly = true;
        return this;
    }

    /**
     * 设置客户端订单 ID。
     *
     * @param cloid Cloid
     * @return this
     */
    public OrderBuilder cloid(Cloid cloid) {
        this.cloid = cloid;
        return this;
    }

    /**
     * 自动生成客户端订单 ID。
     *
     * @return this
     */
    public OrderBuilder autoCloid() {
        this.cloid = Cloid.auto();
        return this;
    }

    // ========================================
    // 7. 构建
    // ========================================

    /**
     * 构建 OrderRequest 对象。
     *
     * @return OrderRequest 实例
     * @throws IllegalStateException 当必填字段缺失时抛出
     */
    public OrderRequest build() {
        // 校验必填字段
        if (coin == null || coin.isEmpty()) {
            throw new IllegalStateException("coin is required");
        }
        if (isBuy == null) {
            throw new IllegalStateException("direction is required (call buy() or sell())");
        }
        if (sz == null || sz.isEmpty()) {
            throw new IllegalStateException("size is required");
        }

        // 构建 OrderType
        if (triggerPx != null) {
            // 触发单
            if (tpsl == null) {
                throw new IllegalStateException("tpsl is required for trigger order (call stopAbove() or stopBelow())");
            }
            this.orderType = new OrderType(new TriggerOrderType(triggerPx, isMarketTrigger != null && isMarketTrigger, tpsl));
        } else {
            // 普通限价/市价单
            if (this.orderType == null) {
                // 默认 GTC
                this.orderType = new OrderType(new LimitOrderType(Tif.GTC));
            }
        }

        OrderRequest req = new OrderRequest(
                instrumentType != null ? instrumentType : InstrumentType.PERP,
                coin,
                isBuy,
                sz,
                limitPx,
                orderType,
                reduceOnly,
                cloid
        );

        if (slippage != null) {
            req.setSlippage(slippage);
        }
        
        if (expiresAfter != null) {
            req.setExpiresAfter(expiresAfter);
        }

        return req;
    }
}
