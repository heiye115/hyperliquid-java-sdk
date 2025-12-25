package io.github.hyperliquid.sdk.model.order;

/**
 * OrderRequest builder, providing convenient chain call API.
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
        return orderType(LimitOrderType.gtc());
    }

    /**
     * Market order (no need to set limit price, placeholder price will be automatically calculated internally).
     *
     * @return this
     */
    public OrderBuilder market() {
        this.limitPx = null;
        return orderType(LimitOrderType.ioc());
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
        return orderType(LimitOrderType.ioc());
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
        return orderType(TriggerOrderType.tp(triggerPx, false));
    }

    /**
     * Triggers when price breaks below (suitable for stop-loss or short breakout).
     *
     * @param triggerPx Trigger price (string)
     * @return this
     */
    public OrderBuilder stopBelow(String triggerPx) {
        return orderType(TriggerOrderType.sl(triggerPx, false));
    }


    /**
     * Sets the trigger order type.
     *
     * @param trigger Trigger order type
     * @return this
     */
    public OrderBuilder orderType(TriggerOrderType trigger) {
        this.orderType = new OrderType(trigger);
        return this;
    }

    /**
     * Sets the limit order type.
     *
     * @param limit Limit order type
     * @return this
     */
    public OrderBuilder orderType(LimitOrderType limit) {
        this.orderType = new OrderType(limit);
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

        // Regular limit/market order
        if (this.orderType == null) {
            // Default GTC
            this.orderType = new OrderType(LimitOrderType.gtc());
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
