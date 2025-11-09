package io.github.hyperliquid.sdk.model.order;

/**
 * 限价下单请求结构。
 */
public class LimitOrderRequest extends OrderRequest {


    /**
     * 构造下单请求。
     *
     * @param instrumentType 交易品种类型
     * @param coin           币种名称（如 "ETH"）
     * @param isBuy          是否买入
     * @param sz             数量
     * @param limitPx        限价价格（可为 null）
     * @param tif            TIF 策略（Gtc/Alo/Ioc）
     * @param reduceOnly     是否只减仓
     * @param cloid          客户端订单 ID
     */
    public LimitOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, Tif tif, Boolean reduceOnly, Cloid cloid) {
        super(instrumentType, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)), reduceOnly, cloid);
    }

    public LimitOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, Tif tif, Boolean reduceOnly, Long cloid) {
        super(instrumentType, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)), reduceOnly, cloid != null ? Cloid.fromLong(cloid) : null);
    }

    public LimitOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, Tif tif, Boolean reduceOnly, String cloid) {
        super(instrumentType, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)), reduceOnly, cloid != null ? Cloid.fromStr(cloid) : null);
    }
}
