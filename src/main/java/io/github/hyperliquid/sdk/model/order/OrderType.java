package io.github.hyperliquid.sdk.model.order;

/**
 * Order type wrapper: limit (TIF) and trigger (TPSL) can be chosen alternatively;
 * If both exist, only the parsing behavior defined by the backend takes effect (generally not set at the same time).
 */
public class OrderType {

    private final LimitOrderType limit;
    private final TriggerOrderType trigger;

    /**
     * Construct order type.
     *
     * @param limit   limit order type (can be null)
     * @param trigger trigger order type (can be null)
     */
    public OrderType(LimitOrderType limit, TriggerOrderType trigger) {
        this.limit = limit;
        this.trigger = trigger;
    }

    public OrderType(LimitOrderType limit) {
        this.limit = limit;
        this.trigger = null;
    }

    public OrderType(TriggerOrderType trigger) {
        this.trigger = trigger;
        this.limit = null;
    }

    /**
     * GTC limit order type
     * GTC (Good Till Cancel): order remains valid until canceled by user or fully executed.
     **/
    public static OrderType limitByGtc() {
        return new OrderType(new LimitOrderType(Tif.GTC));
    }

    /**
     * ALO limit order type
     * ALO (Add Liquidity Only): only add liquidity, cancel if it would execute immediately.
     **/
    public static OrderType limitByAlo() {
        return new OrderType(new LimitOrderType(Tif.ALO));
    }

    /**
     * IOC limit order type
     * IOC (Immediate Or Cancel): order requires immediate full or partial execution, unexecuted portion will be canceled.
     **/
    public static OrderType limitByIoc() {
        return new OrderType(new LimitOrderType(Tif.IOC));
    }

    public static OrderType trigger(TriggerOrderType trigger) {
        return new OrderType(trigger);
    }


    /**
     * Get limit order type.
     *
     * @return limit order type (may be null)
     */
    public LimitOrderType getLimit() {
        return limit;
    }

    /**
     * Get trigger order type.
     *
     * @return trigger order type (may be null)
     */
    public TriggerOrderType getTrigger() {
        return trigger;
    }
}

