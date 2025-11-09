package io.github.hyperliquid.sdk.model.order;

/**
 * 市价下单请求结构。
 */
public class MarketOrderRequest extends OrderRequest {


    /**
     * 构造下单请求。
     */
    public MarketOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Boolean reduceOnly, Cloid cloid) {
        //TODO 确认市价单的 limitPx  值
        super(instrumentType, coin, isBuy, sz, 4000.0, new OrderType(new LimitOrderType(Tif.IOC)), reduceOnly, cloid);
    }

    public MarketOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Boolean reduceOnly, Long cloid) {
        super(instrumentType, coin, isBuy, sz, 4000.0, new OrderType(new LimitOrderType(Tif.IOC)), reduceOnly, cloid != null ? Cloid.fromLong(cloid) : null);
    }

    public MarketOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Boolean reduceOnly, String cloid) {
        super(instrumentType, coin, isBuy, sz, 4000.0, new OrderType(new LimitOrderType(Tif.IOC)), reduceOnly, cloid != null ? Cloid.fromStr(cloid) : null);
    }


}
