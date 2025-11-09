package io.github.hyperliquid.sdk.model.order;

/**
 * 下单请求结构。
 */
public class OrderRequest {

    /**
     * 交易品种类型
     **/
    private final InstrumentType instrumentType;
    private final String coin;
    private final Boolean isBuy;
    private final Double sz;
    private final Double limitPx; // 可为 null（市价）或与触发单组合
    private final OrderType orderType; // 可为 null（普通限价/市价）
    private final Boolean reduceOnly;
    private final Cloid cloid; // 可为 null

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
    public OrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, OrderType orderType, Boolean reduceOnly, Cloid cloid) {
        this.instrumentType = instrumentType;
        this.coin = coin;
        this.isBuy = isBuy;
        this.sz = sz;
        this.limitPx = limitPx;
        this.orderType = orderType;
        this.reduceOnly = reduceOnly;
        this.cloid = cloid;
    }

    public String getCoin() {
        return coin;
    }

    public Boolean getBuy() {
        return isBuy;
    }

    public Double getSz() {
        return sz;
    }

    public Double getLimitPx() {
        return limitPx;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public Boolean getReduceOnly() {
        return reduceOnly;
    }

    public Cloid getCloid() {
        return cloid;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }
}
