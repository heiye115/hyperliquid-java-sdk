package io.github.hyperliquid.sdk.model.order;

import static io.github.hyperliquid.sdk.model.order.TriggerOrderType.TpslType;

/**
 * Order request structure (Java side semantic representation).
 * Note:
 * - Market orders are expressed as "limit + IOC" at protocol level, `limitPx`
 * can be empty, price is calculated by business layer based on mid price and
 * slippage;
 * - Trigger orders carry trigger parameters via `orderType.trigger`;
 * - Will be converted to wire structure and sent (see
 * `utils.Signing.orderRequestToOrderWire`).
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
     * Only used by business layer to simulate "market = IOC limit with slippage"
     * when calculating placeholder price.
     * Default value: "0.05" (5%).
     * </p>
     */
    private String slippage = "0.05";

    /**
     * Order expiration time (milliseconds).
     * <p>
     * - If < 1,000,000,000,000 (i.e., less than 1e12), treated as relative time,
     * actual expiration time is nonce + expiresAfter;
     * - Otherwise treated as absolute timestamp (UTC).
     * - Default value: 120,000ms (120 seconds).
     * </p>
     */
    private Long expiresAfter;

    /**
     * Default constructor.
     * <p>
     * Typically used together with setter methods or static helper methods when
     * building the request step by step.
     * </p>
     */
    public OrderRequest() {
    }

    /**
     * Constructs an order request.
     *
     * @param coin       Currency name (e.g., "ETH")
     * @param isBuy      Whether to buy
     * @param sz         Quantity (string)
     * @param limitPx    Limit price (string, can be null)
     * @param orderType  Order type (can be null)
     * @param reduceOnly Reduce-only flag
     * @param cloid      Client order ID (can be null)
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

    /**
     * Constructs an order request with custom slippage parameter.
     *
     * @param instrumentType Instrument type (PERP or SPOT)
     * @param coin           Currency name (e.g., "ETH")
     * @param isBuy          Whether to buy (true=buy/long, false=sell/short)
     * @param sz             Quantity (string)
     * @param limitPx        Limit price (string, can be null)
     * @param orderType      Order type (can be null)
     * @param reduceOnly     Reduce-only flag
     * @param cloid          Client order ID (can be null)
     * @param slippage       Market order slippage ratio (string)
     */
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
    // Builder Pattern: Chained Calls to Build Orders
    // ========================================

    /**
     * Creates an order builder (Builder pattern).
     * <p>
     * Usage examples:
     *
     * <pre>
     * // Limit order entry
     * OrderRequest req = OrderRequest.builder()
     *         .perp("ETH")
     *         .buy(0.1)
     *         .limitPrice(3000.0)
     *         .gtc()
     *         .build();
     *
     * // Market order entry
     * OrderRequest req = OrderRequest.builder()
     *         .perp("ETH")
     *         .sell(0.1)
     *         .market()
     *         .build();
     *
     * // Conditional order: Buy when price breaks above 2950
     * OrderRequest req = OrderRequest.builder()
     *         .perp("ETH")
     *         .buy(0.1)
     *         .stopPrice(2950.0) // Trigger on upward break
     *         .limitPrice(3000.0)
     *         .build();
     *
     * // Entry with take-profit and stop-loss (requires bulkOrders)
     * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
     *         .perp("ETH")
     *         .buy(0.1)
     *         .entryPrice(3500.0)
     *         .takeProfit(3600.0)
     *         .stopLoss(3400.0)
     *         .buildNormalTpsl();
     * // Automatically infers grouping="normalTpsl"
     * JsonNode result = exchange.bulkOrders(orderGroup);
     * </pre>
     *
     * @return OrderBuilder instance
     */
    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    /**
     * Creates an order builder with take-profit and stop-loss.
     *
     * @return OrderWithTpSlBuilder instance
     */
    public static OrderWithTpSlBuilder entryWithTpSl() {
        return new OrderWithTpSlBuilder();
    }

    /**
     * Gets the instrument type.
     *
     * @return Instrument type
     */
    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    /**
     * Sets the instrument type.
     *
     * @param instrumentType Instrument type
     */
    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
    }

    /**
     * Gets the currency name.
     *
     * @return Currency name
     */
    public String getCoin() {
        return coin;
    }

    /**
     * Sets the currency name.
     *
     * @param coin Currency name
     */
    public void setCoin(String coin) {
        this.coin = coin;
    }

    /**
     * Gets the buy flag.
     *
     * @return Whether to buy (true=buy/long, false=sell/short)
     */
    public Boolean getIsBuy() {
        return isBuy;
    }

    /**
     * Sets the buy flag.
     *
     * @param isBuy Whether to buy (true=buy/long, false=sell/short)
     */
    public void setIsBuy(Boolean isBuy) {
        this.isBuy = isBuy;
    }

    /**
     * Gets the order quantity.
     *
     * @return Quantity (string)
     */
    public String getSz() {
        return sz;
    }

    /**
     * Sets the order quantity.
     *
     * @param sz Quantity (string)
     */
    public void setSz(String sz) {
        this.sz = sz;
    }

    /**
     * Gets the limit price.
     *
     * @return Limit price (string)
     */
    public String getLimitPx() {
        return limitPx;
    }

    /**
     * Sets the limit price.
     *
     * @param limitPx Limit price (string)
     */
    public void setLimitPx(String limitPx) {
        this.limitPx = limitPx;
    }

    /**
     * Gets the order type.
     *
     * @return Order type
     */
    public OrderType getOrderType() {
        return orderType;
    }

    /**
     * Sets the order type.
     *
     * @param orderType Order type
     */
    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    /**
     * Gets the reduce-only flag.
     *
     * @return Reduce-only flag
     */
    public Boolean getReduceOnly() {
        return reduceOnly;
    }

    /**
     * Sets the reduce-only flag.
     *
     * @param reduceOnly Reduce-only flag
     */
    public void setReduceOnly(Boolean reduceOnly) {
        this.reduceOnly = reduceOnly;
    }

    /**
     * Gets the client order ID.
     *
     * @return Client order ID
     */
    public Cloid getCloid() {
        return cloid;
    }

    /**
     * Sets the client order ID.
     *
     * @param cloid Client order ID
     */
    public void setCloid(Cloid cloid) {
        this.cloid = cloid;
    }

    /**
     * Gets the market order slippage ratio.
     *
     * @return Slippage ratio (string)
     */
    public String getSlippage() {
        return slippage;
    }

    /**
     * Sets the market order slippage ratio.
     *
     * @param slippage Slippage ratio (string)
     */
    public void setSlippage(String slippage) {
        this.slippage = slippage;
    }

    /**
     * Gets the order expiration time.
     *
     * @return Expiration time (milliseconds)
     */
    public Long getExpiresAfter() {
        return expiresAfter;
    }

    /**
     * Sets the order expiration time.
     *
     * @param expiresAfter Expiration time (milliseconds)
     */
    public void setExpiresAfter(Long expiresAfter) {
        this.expiresAfter = expiresAfter;
    }

    /**
     * Static inner class for quickly creating entry order requests.
     */
    public static class Open {
        // ========================================
        // Perpetual Contracts - Market Entry
        // ========================================

        /**
         * Creates a perpetual contract market order entry (without cloid).
         *
         * @param coin  Currency name
         * @param isBuy Whether to buy
         * @param sz    Quantity (string)
         * @return OrderRequest instance
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz) {
            return market(coin, isBuy, sz, null);
        }

        /**
         * Creates a perpetual contract market order entry.
         *
         * @param coin  Currency name
         * @param isBuy Whether to buy
         * @param sz    Quantity (string)
         * @param cloid Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz, Cloid cloid) {
            OrderRequest req = createMarket(InstrumentType.PERP, coin, isBuy, sz);
            req.setCloid(cloid);
            return req;
        }

        // ========================================
        // Perpetual Contracts - Limit Entry
        // ========================================

        /**
         * Creates a perpetual contract limit order entry (default GTC, without cloid).
         *
         * @param coin    Currency name
         * @param isBuy   Whether to buy
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest limit(String coin, boolean isBuy, String sz, String limitPx) {
            return limit(Tif.GTC, coin, isBuy, sz, limitPx, null);
        }

        /**
         * Creates a perpetual contract limit order entry (default GTC).
         *
         * @param coin    Currency name
         * @param isBuy   Whether to buy
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest limit(String coin, boolean isBuy, String sz, String limitPx, Cloid cloid) {
            return limit(Tif.GTC, coin, isBuy, sz, limitPx, cloid);
        }

        /**
         * Creates a perpetual contract limit order entry (without cloid).
         *
         * @param tif     Time in force
         * @param coin    Currency name
         * @param isBuy   Whether to buy
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest limit(Tif tif, String coin, boolean isBuy, String sz, String limitPx) {
            return limit(tif, coin, isBuy, sz, limitPx, null);
        }

        /**
         * Creates a perpetual contract limit order entry.
         *
         * @param tif     Time in force
         * @param coin    Currency name
         * @param isBuy   Whether to buy
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest limit(Tif tif, String coin, boolean isBuy, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.PERP, tif, coin, isBuy, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        // ========================================
        // Perpetual Contracts - Trigger Orders (Breakout Entry)
        // ========================================

        /**
         * Creates a breakout entry order (triggers when price breaks above, executes at
         * market, without cloid).
         * Use case: Long breakout strategy, chasing upward momentum when price breaks
         * key resistance.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest breakoutAbove(String coin, String sz, String triggerPx) {
            return breakoutAbove(coin, true, sz, triggerPx, null);
        }

        /**
         * Creates a breakout entry order (triggers when price breaks above, executes at
         * market).
         *
         * @param coin      Currency name
         * @param isBuy     Whether to buy
         * @param sz        Quantity (string)
         * @param triggerPx Trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest breakoutAbove(String coin, Boolean isBuy, String sz, String triggerPx) {
            return breakoutAbove(coin, isBuy, sz, triggerPx, null);
        }

        /**
         * Creates a breakout entry order (triggers when price breaks above, executes at
         * market).
         * Use case: Long breakout strategy, chasing upward momentum when price breaks
         * key resistance.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Trigger price (string)
         * @param cloid     Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest breakoutAbove(String coin, Boolean isBuy, String sz, String triggerPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
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
         * Creates a breakout entry order (triggers when price breaks below, executes at
         * market, without cloid).
         * Use case: Short breakout strategy, chasing downward momentum when price
         * breaks key support.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest breakoutBelow(String coin, String sz, String triggerPx) {
            return breakoutBelow(coin, false, sz, triggerPx, null);
        }

        /**
         * Creates a breakout entry order (triggers when price breaks below, executes at
         * market).
         *
         * @param coin      Currency name
         * @param isBuy     Whether to buy
         * @param sz        Quantity (string)
         * @param triggerPx Trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest breakoutBelow(String coin, Boolean isBuy, String sz, String triggerPx) {
            return breakoutBelow(coin, isBuy, sz, triggerPx, null);
        }

        /**
         * Creates a breakout entry order (triggers when price breaks below, executes at
         * market).
         * <p>
         * Use case: Short breakout strategy, chasing downward momentum when price
         * breaks key support.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Trigger price (string)
         * @param cloid     Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest breakoutBelow(String coin, Boolean isBuy, String sz, String triggerPx, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
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
        // Spot Trading - Convenience Methods
        // ========================================

        /**
         * Creates a spot market buy order (without cloid).
         *
         * @param coin Currency name
         * @param sz   Quantity (string)
         * @return OrderRequest instance
         */
        public static OrderRequest spotMarketBuy(String coin, String sz) {
            return spotMarketBuy(coin, sz, null);
        }

        /**
         * Creates a spot market buy order.
         *
         * @param coin  Currency name
         * @param sz    Quantity (string)
         * @param cloid Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest spotMarketBuy(String coin, String sz, Cloid cloid) {
            OrderRequest req = createMarket(InstrumentType.SPOT, coin, true, sz);
            req.setCloid(cloid);
            return req;
        }

        /**
         * Creates a spot market sell order (without cloid).
         *
         * @param coin Currency name
         * @param sz   Quantity (string)
         * @return OrderRequest instance
         */
        public static OrderRequest spotMarketSell(String coin, String sz) {
            return spotMarketSell(coin, sz, null);
        }

        /**
         * Creates a spot market sell order.
         *
         * @param coin  Currency name
         * @param sz    Quantity (string)
         * @param cloid Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest spotMarketSell(String coin, String sz, Cloid cloid) {
            OrderRequest req = createMarket(InstrumentType.SPOT, coin, false, sz);
            req.setCloid(cloid);
            return req;
        }

        /**
         * Creates a spot limit buy order (without cloid).
         *
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitBuy(String coin, String sz, String limitPx) {
            return spotLimitBuy(coin, sz, limitPx, null);
        }

        /**
         * Creates a spot limit buy order.
         *
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitBuy(String coin, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.SPOT, Tif.GTC, coin, true, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        /**
         * Creates a spot limit buy order with custom TIF (without cloid).
         *
         * @param tif     Time in force
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitBuy(Tif tif, String coin, String sz, String limitPx) {
            return spotLimitBuy(tif, coin, sz, limitPx, null);
        }

        /**
         * Creates a spot limit buy order with custom TIF.
         *
         * @param tif     Time in force
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitBuy(Tif tif, String coin, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.SPOT, tif, coin, true, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        /**
         * Creates a spot limit sell order (without cloid).
         *
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitSell(String coin, String sz, String limitPx) {
            return spotLimitSell(coin, sz, limitPx, null);
        }

        /**
         * Creates a spot limit sell order.
         *
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitSell(String coin, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.SPOT, Tif.GTC, coin, false, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        /**
         * Creates a spot limit sell order with custom TIF (without cloid).
         *
         * @param tif     Time in force
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitSell(Tif tif, String coin, String sz, String limitPx) {
            return spotLimitSell(tif, coin, sz, limitPx, null);
        }

        /**
         * Creates a spot limit sell order with custom TIF.
         *
         * @param tif     Time in force
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest spotLimitSell(Tif tif, String coin, String sz, String limitPx, Cloid cloid) {
            OrderRequest req = createLimit(InstrumentType.SPOT, tif, coin, false, sz, limitPx);
            req.setCloid(cloid);
            return req;
        }

        // ========================================
        // Internal Utility Methods (Private)
        // ========================================

        /**
         * Creates a market order entry (internal method).
         *
         * @param instrumentType Instrument type
         * @param coin           Currency name
         * @param isBuy          Whether to buy
         * @param sz             Quantity (string)
         * @return OrderRequest instance
         */
        private static OrderRequest createMarket(InstrumentType instrumentType, String coin, boolean isBuy, String sz) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(instrumentType);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
            req.setSz(sz);
            req.setReduceOnly(false);
            // Set as IOC market order
            LimitOrderType limitOrderType = new LimitOrderType(Tif.IOC);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }

        /**
         * Creates a limit order entry (internal method).
         *
         * @param instrumentType Instrument type
         * @param tif            Time in force
         * @param coin           Currency name
         * @param isBuy          Whether to buy
         * @param sz             Quantity (string)
         * @param limitPx        Limit price (string)
         * @return OrderRequest instance
         */
        private static OrderRequest createLimit(InstrumentType instrumentType, Tif tif, String coin, boolean isBuy,
                String sz, String limitPx) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(instrumentType);
            req.setCoin(coin);
            req.setIsBuy(isBuy);
            req.setSz(sz);
            req.setLimitPx(limitPx);
            req.setReduceOnly(false);
            // Set order type
            LimitOrderType limitOrderType = new LimitOrderType(tif);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }
    }

    /**
     * Static inner class for quickly creating close position order requests.
     */
    public static class Close {

        // ========================================
        // Market Close Orders
        // ========================================

        /**
         * Creates a market close order (direction inferred automatically, without
         * cloid).
         *
         * @param coin Currency name
         * @param sz   Quantity (string)
         * @return OrderRequest instance
         */
        public static OrderRequest market(String coin, String sz) {
            return market(coin, sz, null);
        }

        /**
         * Creates a market close order (direction inferred automatically).
         *
         * @param coin  Currency name
         * @param sz    Quantity (string)
         * @param cloid Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest market(String coin, String sz, Cloid cloid) {
            OrderRequest req = new OrderRequest();
            req.setInstrumentType(InstrumentType.PERP);
            req.setCoin(coin);
            req.setSz(sz);
            req.setReduceOnly(true);
            req.setCloid(cloid);
            // Set as IOC market order
            LimitOrderType limitOrderType = new LimitOrderType(Tif.IOC);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }

        /**
         * Creates a market close order (with specified direction, without cloid).
         *
         * @param coin  Currency name
         * @param isBuy Whether to buy
         * @param sz    Quantity (string)
         * @return OrderRequest instance
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz) {
            return market(coin, isBuy, sz, null);
        }

        /**
         * Creates a market close order (with specified direction).
         *
         * @param coin  Currency name
         * @param isBuy Whether to buy
         * @param sz    Quantity (string)
         * @param cloid Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest market(String coin, boolean isBuy, String sz, Cloid cloid) {
            OrderRequest req = market(coin, sz, cloid);
            req.setIsBuy(isBuy);
            return req;
        }

        /**
         * Creates a market close-all order (without cloid).
         *
         * @param coin Currency name
         * @return OrderRequest instance
         */
        public static OrderRequest marketAll(String coin) {
            return marketAll(coin, null);
        }

        /**
         * Creates a market close-all order.
         *
         * @param coin  Currency name
         * @param cloid Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest marketAll(String coin, Cloid cloid) {
            return market(coin, null, cloid);
        }

        // ========================================
        // Limit Close Orders
        // ========================================

        /**
         * Creates a limit close order (default GTC, without cloid).
         *
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest limit(String coin, String sz, String limitPx) {
            return limit(Tif.GTC, coin, null, sz, limitPx, null);
        }

        /**
         * Creates a limit close order (default GTC).
         *
         * @param coin    Currency name
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
         */
        public static OrderRequest limit(String coin, String sz, String limitPx, Cloid cloid) {
            return limit(Tif.GTC, coin, null, sz, limitPx, cloid);
        }

        /**
         * Creates a limit close order.
         *
         * @param tif     Time in force
         * @param coin    Currency name
         * @param isBuy   Whether to buy
         * @param sz      Quantity (string)
         * @param limitPx Limit price (string)
         * @param cloid   Client order ID (can be null)
         * @return OrderRequest instance
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
            // Set order type
            LimitOrderType limitOrderType = new LimitOrderType(tif);
            OrderType orderType = new OrderType(limitOrderType);
            req.setOrderType(orderType);
            return req;
        }

        // ========================================
        // Take-Profit/Stop-Loss Close Orders (Convenience Methods)
        // ========================================

        /**
         * Creates a take-profit close order (triggers when price breaks above, executes
         * at market, without cloid).
         * Use case: Long position take-profit, automatically closes position to lock in
         * profits when price reaches target.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Take-profit trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest takeProfit(String coin, Boolean isBuy, String sz, String triggerPx) {
            return takeProfit(coin, isBuy, sz, triggerPx, null);
        }

        /**
         * Creates a take-profit close order for a long position.
         * <p>
         * Internally uses a sell order (isBuy=false) to close the long position when
         * the trigger price is reached.
         * </p>
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Take-profit trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest takeProfitForLong(String coin, String sz, String triggerPx) {
            return takeProfit(coin, false, sz, triggerPx, null);
        }

        /**
         * Creates a take-profit close order for a short position.
         * <p>
         * Internally uses a buy order (isBuy=true) to close the short position when
         * the trigger price is reached.
         * </p>
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Take-profit trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest takeProfitForShort(String coin, String sz, String triggerPx) {
            return takeProfit(coin, true, sz, triggerPx, null);
        }

        /**
         * Creates a take-profit close order (triggers when price breaks above, executes
         * at market).
         * <p>
         * Use case: Long position take-profit, automatically closes position to lock in
         * profits when price reaches target.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Take-profit trigger price (string)
         * @param cloid     Client order ID (can be null)
         * @return OrderRequest instance
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
         * Creates a stop-loss close order (triggers when price breaks below, executes
         * at market, without cloid).
         * Use case: Long position stop-loss, automatically closes position to limit
         * losses when price drops below stop level.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Stop-loss trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest stopLoss(String coin, Boolean isBuy, String sz, String triggerPx) {
            return stopLoss(coin, isBuy, sz, triggerPx, null);
        }

        /**
         * Creates a stop-loss close order for a long position.
         * <p>
         * Internally uses a sell order (isBuy=false) to close the long position when
         * the trigger price is reached.
         * </p>
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Stop-loss trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest stopLossForLong(String coin, String sz, String triggerPx) {
            return stopLoss(coin, false, sz, triggerPx, null);
        }

        /**
         * Creates a stop-loss close order for a short position.
         * <p>
         * Internally uses a buy order (isBuy=true) to close the short position when
         * the trigger price is reached.
         * </p>
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Stop-loss trigger price (string)
         * @return OrderRequest instance
         */
        public static OrderRequest stopLossForShort(String coin, String sz, String triggerPx) {
            return stopLoss(coin, true, sz, triggerPx, null);
        }

        /**
         * Creates a stop-loss close order (triggers when price breaks below, executes
         * at market).
         * Use case: Long position stop-loss, automatically closes position to limit
         * losses when price drops below stop level.
         *
         * @param coin      Currency name
         * @param sz        Quantity (string)
         * @param triggerPx Stop-loss trigger price (string)
         * @param cloid     Client order ID (can be null)
         * @return OrderRequest instance
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
