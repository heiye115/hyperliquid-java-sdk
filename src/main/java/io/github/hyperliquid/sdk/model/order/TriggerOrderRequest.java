package io.github.hyperliquid.sdk.model.order;

public class TriggerOrderRequest extends OrderRequest {

    /**
     * 构造下单请求。
     */
    public TriggerOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Boolean reduceOnly, Cloid cloid) {
        super(instrumentType, coin, isBuy, sz, limitPx, new OrderType(new TriggerOrderType(triggerPx, isMarket, tpsl)), reduceOnly, cloid);
    }

    public TriggerOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Boolean reduceOnly, Long cloid) {
        super(instrumentType, coin, isBuy, sz, limitPx, new OrderType(new TriggerOrderType(triggerPx, isMarket, tpsl)), reduceOnly, cloid != null ? Cloid.fromLong(cloid) : null);
    }

    public TriggerOrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Boolean reduceOnly, String cloid) {
        super(instrumentType, coin, isBuy, sz, limitPx, new OrderType(new TriggerOrderType(triggerPx, isMarket, tpsl)), reduceOnly, cloid != null ? Cloid.fromStr(cloid) : null);
    }

}
