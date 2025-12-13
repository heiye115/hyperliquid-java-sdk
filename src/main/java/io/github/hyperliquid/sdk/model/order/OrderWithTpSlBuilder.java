package io.github.hyperliquid.sdk.model.order;

import java.util.ArrayList;
import java.util.List;

/**
 * Open position + take-profit/stop-loss combined order builder.
 * <p>
 * Used to build normalTpsl or positionTpsl order groups, simplifying one-click setup of take-profit/stop-loss operations.
 * <p>
 * Usage examples:
 * <pre>
 * // 1. Limit order open + take-profit/stop-loss (normalTpsl)
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     .buy(0.1)
 *     .entryPrice(3500.0)  // Set entryPrice, will use limit order
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildNormalTpsl();
 *
 * // Automatically infer grouping="normalTpsl" when submitting
 * JsonNode result = exchange.bulkOrders(orderGroup);
 *
 * // 2. Market order open + take-profit/stop-loss (normalTpsl)
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     .buy(0.1)
 *     // No entryPrice set, will use market order
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildNormalTpsl();
 *
 * // Automatically infer grouping="normalTpsl" when submitting
 * JsonNode result = exchange.bulkOrders(orderGroup);
 *
 * // 3. Add take-profit/stop-loss to existing position (positionTpsl - manual specification)
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     .closePosition(0.5, true)  // Close 0.5 ETH long position
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildPositionTpsl();
 *
 * // Automatically infer grouping="positionTpsl" when submitting
 * JsonNode result = exchange.bulkOrders(orderGroup);
 *
 * // 4. Add take-profit/stop-loss to existing position (positionTpsl - automatic inference)
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     // Don't call closePosition(), Exchange will automatically query account positions and infer direction and quantity
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildPositionTpsl();
 *
 * // Exchange will automatically call API to get position information and fill direction and quantity
 * JsonNode result = exchange.bulkOrders(orderGroup);
 * </pre>
 */
public class OrderWithTpSlBuilder {
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
     * Entry limit price (string, required for buildAll mode)
     */
    private String entryPrice;

    /**
     * Take-profit price (string)
     */
    private String takeProfitPrice;

    /**
     * Stop-loss price (string)
     */
    private String stopLossPrice;

    /**
     * Entry order TIF strategy (default GTC)
     */
    private Tif entryTif = Tif.GTC;

    /**
     * Client order ID
     */
    private Cloid cloid;

    /**
     * Order expiration time (milliseconds)
     */
    private Long expiresAfter;

    public OrderWithTpSlBuilder() {
    }

    // ========================================
    // 5. Expiration Time
    // ========================================

    /**
     * Sets the order expiration time.
     *
     * @param expiresAfter Expiration time (milliseconds)
     * @return this
     */
    public OrderWithTpSlBuilder expiresAfter(Long expiresAfter) {
        this.expiresAfter = expiresAfter;
        return this;
    }

    // ========================================
    // 1. Instrument Type
    // ========================================

    /**
     * Sets the instrument type to perpetual contract.
     *
     * @param coin Currency name
     * @return this
     */
    public OrderWithTpSlBuilder perp(String coin) {
        this.instrumentType = InstrumentType.PERP;
        this.coin = coin;
        return this;
    }

    /**
     * Sets the instrument type to spot trading.
     *
     * @param coin Currency name
     * @return this
     */
    public OrderWithTpSlBuilder spot(String coin) {
        this.instrumentType = InstrumentType.SPOT;
        this.coin = coin;
        return this;
    }

    // ========================================
    // 2. Direction and Quantity
    // ========================================

    /**
     * Buys to open a position.
     *
     * @param sz Quantity (string)
     * @return this
     */
    public OrderWithTpSlBuilder buy(String sz) {
        this.isBuy = true;
        this.sz = sz;
        return this;
    }

    /**
     * Sells to open a position.
     *
     * @param sz Quantity (string)
     * @return this
     */
    public OrderWithTpSlBuilder sell(String sz) {
        this.isBuy = false;
        this.sz = sz;
        return this;
    }

    /**
     * Close position mode (used for positionTpsl).
     * <p>
     * Used when you already hold a position and want to add take-profit/stop-loss to it.
     *
     * @param sz             Position quantity (string)
     * @param isLongPosition Whether it's a long position (true=long, false=short)
     * @return this
     */
    public OrderWithTpSlBuilder closePosition(String sz, boolean isLongPosition) {
        this.isBuy = isLongPosition; // Long positions need to sell to close, short positions need to buy to close
        this.sz = sz;
        this.entryPrice = null; // No entry price needed
        return this;
    }

    // ========================================
    // 3. Price Settings
    // ========================================

    /**
     * Sets the entry limit price.
     *
     * @param entryPrice Entry price (string)
     * @return this
     */
    public OrderWithTpSlBuilder entryPrice(String entryPrice) {
        this.entryPrice = entryPrice;
        return this;
    }

    /**
     * Sets the take-profit price.
     *
     * @param tpPrice Take-profit price (string)
     * @return this
     */
    public OrderWithTpSlBuilder takeProfit(String tpPrice) {
        this.takeProfitPrice = tpPrice;
        return this;
    }

    /**
     * Sets the stop-loss price.
     *
     * @param slPrice Stop-loss price (string)
     * @return this
     */
    public OrderWithTpSlBuilder stopLoss(String slPrice) {
        this.stopLossPrice = slPrice;
        return this;
    }

    // ========================================
    // 4. Other Options
    // ========================================

    /**
     * Sets the TIF strategy for the entry order.
     *
     * @param tif TIF strategy
     * @return this
     */
    public OrderWithTpSlBuilder entryTif(Tif tif) {
        this.entryTif = tif;
        return this;
    }

    /**
     * Sets the client order ID.
     *
     * @param cloid Cloid
     * @return this
     */
    public OrderWithTpSlBuilder cloid(Cloid cloid) {
        this.cloid = cloid;
        return this;
    }

    // ========================================
    // 5. Build
    // ========================================

    /**
     * Builds a normalTpsl order group (entry + TP + SL).
     * <p>
     * The returned OrderGroup will automatically carry grouping="normalTpsl" type information.
     * When calling exchange.bulkOrders(orderGroup), it will automatically infer the use of normalTpsl grouping.
     *
     * @return OrderGroup containing order list and grouping type
     * @throws IllegalStateException Thrown when required fields are missing
     */
    public OrderGroup buildNormalTpsl() {
        return new OrderGroup(buildOrderList(true), GroupingType.NORMAL_TPSL);
    }

    /**
     * Builds a positionTpsl order group (TP + SL only, no entry order).
     * <p>
     * The returned OrderGroup will automatically carry grouping="positionTpsl" type information.
     * Used to add or modify take-profit/stop-loss for existing positions.
     * <p>
     * <b>Automatic position inference feature:</b>
     * <ul>
     *   <li>If closePosition(sz, isLong) is called - manually specify position direction and quantity</li>
     *   <li>If closePosition() is not called - Exchange will automatically query account positions and infer direction and quantity</li>
     * </ul>
     *
     * @return OrderGroup containing order list and grouping type
     * @throws IllegalStateException Thrown when required fields are missing
     */
    public OrderGroup buildPositionTpsl() {
        return new OrderGroup(buildOrderList(false), GroupingType.POSITION_TPSL);
    }

    /**
     * Builds the order list.
     *
     * @param includeEntry Whether to include the entry order (true=normalTpsl, false=positionTpsl)
     * @return Order list
     */
    private List<OrderRequest> buildOrderList(boolean includeEntry) {
        validate(includeEntry);
        List<OrderRequest> orders = new ArrayList<>();
        
        // 1. Entry order (only added in normalTpsl mode)
        if (includeEntry) {
            OrderRequest entry;
            if (entryPrice == null) {
            // Market order - Creates a placeholder request, which will be processed by the marketOpenTransition method in Exchange
            entry = new OrderRequest();
            entry.setInstrumentType(instrumentType != null ? instrumentType : InstrumentType.PERP);
            entry.setCoin(coin);
            entry.setIsBuy(isBuy);
            entry.setSz(sz);
            entry.setReduceOnly(false);
            entry.setCloid(cloid);
            // Set as IOC market order, price will be calculated in Exchange.marketOpenTransition
            LimitOrderType limitOrderType = new LimitOrderType(Tif.IOC);
            OrderType orderType = new OrderType(limitOrderType);
            entry.setOrderType(orderType);
        } else {
            // Limit order
            entry = new OrderRequest(
                    instrumentType != null ? instrumentType : InstrumentType.PERP,
                    coin,
                    isBuy,
                    sz,
                    entryPrice,
                    new OrderType(new LimitOrderType(entryTif)),
                    false,
                    cloid
            );
        }
            if (expiresAfter != null) {
                entry.setExpiresAfter(expiresAfter);
            }
            orders.add(entry);
        }
        // 2. Take-profit order (close position)
        if (takeProfitPrice != null) {
            OrderRequest tp = new OrderRequest(
                    instrumentType != null ? instrumentType : InstrumentType.PERP,
                    coin,
                    isBuy != null ? !isBuy : null,  // Reverse close position (if isBuy is null, keep it null)
                    sz,
                    takeProfitPrice,
                    new OrderType(new TriggerOrderType(takeProfitPrice, true, TriggerOrderType.TpslType.TP)),
                    true,
                    null
            );

            if (expiresAfter != null) {
                tp.setExpiresAfter(expiresAfter);
            }
            orders.add(tp);
        }

        // 3. Stop-loss order (close position)
        if (stopLossPrice != null) {
            OrderRequest sl = new OrderRequest(
                    instrumentType != null ? instrumentType : InstrumentType.PERP,
                    coin,
                    isBuy != null ? !isBuy : null,
                    sz,
                    stopLossPrice,
                    new OrderType(new TriggerOrderType(stopLossPrice, true, TriggerOrderType.TpslType.SL)),
                    true,
                    null
            );

            if (expiresAfter != null) {
                sl.setExpiresAfter(expiresAfter);
            }
            orders.add(sl);
        }

        return orders;
    }

    /**
     * Validates required fields.
     *
     * @param isNormalTpsl Whether it's normalTpsl mode (true=normalTpsl, false=positionTpsl)
     */
    private void validate(boolean isNormalTpsl) {
        if (coin == null || coin.isEmpty()) {
            throw new IllegalStateException("coin is required");
        }
        
        // normalTpsl mode: direction and quantity must be specified
        if (isNormalTpsl) {
            if (isBuy == null) {
                throw new IllegalStateException("direction is required for normalTpsl (call buy() or sell())");
            }
            if (sz == null || sz.isEmpty()) {
                throw new IllegalStateException("size is required for normalTpsl (call buy() or sell())");
            }
        }
        // positionTpsl mode: allows isBuy and sz to be null (automatically inferred by Exchange)
        // But if sz is set, it cannot be an empty string
        else {
            if (sz != null && sz.isEmpty()) {
                throw new IllegalStateException("size cannot be empty if specified");
            }
        }
        
        if (takeProfitPrice == null && stopLossPrice == null) {
            throw new IllegalStateException("at least one of takeProfit or stopLoss is required");
        }
    }
}
