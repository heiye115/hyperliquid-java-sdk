package io.github.hyperliquid.sdk.model.order;

/**
 * Order modification request wrapper.
 * <p>
 * Used for batch order modification, supports locating orders via OID or Cloid.
 * </p>
 */
public class ModifyOrderRequest {

    /**
     * Order ID (OID)
     */
    private Long oid;

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

    public ModifyOrderRequest() {
    }


    public static ModifyOrderRequest byOid(String coin, Long oid) {
        ModifyOrderRequest modifyOrderRequest = new ModifyOrderRequest();
        modifyOrderRequest.setOid(oid);
        modifyOrderRequest.setCoin(coin);
        return modifyOrderRequest;
    }

    public void setOrderType(TriggerOrderType trigger) {
        this.orderType = new OrderType(trigger);
    }

    public void setOrderType(LimitOrderType limit) {
        this.orderType = new OrderType(limit);
    }

    public Long getOid() {
        return oid;
    }

    public void setOid(Long oid) {
        this.oid = oid;
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

    public Boolean isBuy() {
        return isBuy;
    }

    public void setBuy(Boolean buy) {
        isBuy = buy;
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
}
