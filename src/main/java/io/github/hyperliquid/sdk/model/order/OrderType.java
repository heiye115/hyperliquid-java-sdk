package io.github.hyperliquid.sdk.model.order;

/**
 * 订单类型封装：限价（TIF）与触发（TPSL）可二择一；
 * 若同时存在，仅后端定义的解析行为生效（一般不同时设置）。
 */
public class OrderType {

    private final LimitOrderType limit;
    private final TriggerOrderType trigger;

    /**
     * 构造订单类型。
     *
     * @param limit   限价单类型（可为 null）
     * @param trigger 触发单类型（可为 null）
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
     * Gtc限价单类型
     * GTC (Good Till Cancel) ：订单在取消前一直有效，直到被用户手动取消或完全成交。
     **/
    public static OrderType limitByGtc() {
        return new OrderType(new LimitOrderType(Tif.GTC));
    }

    /**
     * Alo限价单类型
     * ALO (Add Liquidity Only)：仅添加流动性，若会立即成交则取消。
     **/
    public static OrderType limitByAlo() {
        return new OrderType(new LimitOrderType(Tif.ALO));
    }

    /**
     * Ioc限价单类型
     * IOC (Immediate Or Cancel) ：订单要求立即全部或部分成交，未成交部分将被取消。
     **/
    public static OrderType limitByIoc() {
        return new OrderType(new LimitOrderType(Tif.IOC));
    }

    public static OrderType trigger(TriggerOrderType trigger) {
        return new OrderType(trigger);
    }


    /**
     * 获取限价单类型。
     *
     * @return 限价单类型（可能为 null）
     */
    public LimitOrderType getLimit() {
        return limit;
    }

    /**
     * 获取触发单类型。
     *
     * @return 触发单类型（可能为 null）
     */
    public TriggerOrderType getTrigger() {
        return trigger;
    }
}

