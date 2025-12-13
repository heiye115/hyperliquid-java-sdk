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
    // 7. Expiration Time
    // ========================================

    /**
     * Sets the order expiration time.
     *
     * @param expiresAfter Expiration time (milliseconds)
     * @return this
     */
    public OrderBuilder expiresAfter(Long expiresAfter) {
        this.expiresAfter = expiresAfter;
        return this;
    }

    // ========================================
    // 1. Instrument Type
    // ========================================

    /**
     * Sets the instrument type to perpetual contract.
     *
     * @param coin Currency name (e.g., "ETH")
     * @return this
     */
    public OrderBuilder perp(String coin) {
        this.instrumentType = InstrumentType.PERP;
        this.coin = coin;
        return this;
    }

    /**
     * Sets the instrument type to spot trading.
     *
     * @param coin Currency name (e.g., "PURR")
     * @return this
     */
    public OrderBuilder spot(String coin) {
        this.instrumentType = InstrumentType.SPOT;
        this.coin = coin;
        return this;
    }

    // ========================================
    // 2. Direction and Quantity
    // ========================================

    /**
     * Buys the specified quantity.
     *
     * @param sz Quantity (string)
     * @return this
     */
    public OrderBuilder buy(String sz) {
        this.isBuy = true;
        this.sz = sz;
        return this;
    }

    /**
     * Sells the specified quantity.
     *
     * @param sz Quantity (string)
     * @return this
     */
    public OrderBuilder sell(String sz) {
        this.isBuy = false;
        this.sz = sz;
        return this;
    }

    // ========================================
    // 3. Price Settings
    // ========================================

    /**
     * Sets the limit price.
     *
     * @param limitPx Limit price (string)
     * @return this
     */
    public OrderBuilder limitPrice(String limitPx) {
        this.limitPx = limitPx;
        return this;
    }

    /**
     * Market order (no need to set limit price, placeholder price will be automatically calculated internally).
     *
     * @return this
     */
    public OrderBuilder market() {
        this.limitPx = null;
        this.orderType = new OrderType(new LimitOrderType(Tif.IOC));
        return this;
    }

    /**
     * Market order with custom slippage.
     *
     * @param slippage Slippage ratio (string, e.g., "0.05" for 5%)
     * @return this
     */
    public OrderBuilder market(String slippage) {
        this.limitPx = null;
        this.slippage = slippage;
        this.orderType = new OrderType(new LimitOrderType(Tif.IOC));
        return this;
    }

    // ========================================
    // 4. Trigger Conditions
    // ========================================

    /**
     * Triggers when price breaks above (suitable for take-profit or long breakout).
     *
     * @param triggerPx Trigger price (string)
     * @return this
     */
    public OrderBuilder stopAbove(String triggerPx) {
        this.triggerPx = triggerPx;
        this.tpsl = TriggerOrderType.TpslType.TP;
        this.isMarketTrigger = false; // Default to placing limit order after trigger
        return this;
    }

    /**
     * Triggers when price breaks below (suitable for stop-loss or short breakout).
     *
     * @param triggerPx Trigger price (string)
     * @return this
     */
    public OrderBuilder stopBelow(String triggerPx) {
        this.triggerPx = triggerPx;
        this.tpsl = TriggerOrderType.TpslType.SL;
        this.isMarketTrigger = false;
        return this;
    }

    /**
     * Executes at market price after trigger (requires calling stopAbove or stopBelow first).
     *
     * @return this
     */
    public OrderBuilder marketTrigger() {
        this.isMarketTrigger = true;
        return this;
    }

    // ========================================
    // 5. TIF Strategies
    // ========================================

    /**
     * Good Til Cancel (GTC).
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
     * Immediate or Cancel (IOC).
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
     * Add Liquidity Only (ALO).
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
    // 6. Other Options
    // ========================================

    /**
     * Reduce-only (close position order).
     *
     * @return this
     */
    public OrderBuilder reduceOnly() {
        this.reduceOnly = true;
        return this;
    }

    /**
     * Sets the client order ID.
     *
     * @param cloid Cloid
     * @return this
     */
    public OrderBuilder cloid(Cloid cloid) {
        this.cloid = cloid;
        return this;
    }

    /**
     * Automatically generates a client order ID.
     *
     * @return this
     */
    public OrderBuilder autoCloid() {
        this.cloid = Cloid.auto();
        return this;
    }

    // ========================================
    // 7. Build
    // ========================================

    /**
     * Builds the OrderRequest object.
     *
     * @return OrderRequest instance
     * @throws IllegalStateException Thrown when required fields are missing
     */
    public OrderRequest build() {
        // Validate required fields
        if (coin == null || coin.isEmpty()) {
            throw new IllegalStateException("coin is required");
        }
        if (isBuy == null) {
            throw new IllegalStateException("direction is required (call buy() or sell())");
        }
        if (sz == null || sz.isEmpty()) {
            throw new IllegalStateException("size is required");
        }

        // Build OrderType
        if (triggerPx != null) {
            // Trigger order
            if (tpsl == null) {
                throw new IllegalStateException("tpsl is required for trigger order (call stopAbove() or stopBelow())");
            }
            this.orderType = new OrderType(new TriggerOrderType(triggerPx, isMarketTrigger != null && isMarketTrigger, tpsl));
        } else {
            // Regular limit/market order
            if (this.orderType == null) {
                // Default GTC
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
