package io.github.hyperliquid.sdk.model.order;

import java.util.Optional;

/**
 * 订单类型封装（可选限价单与可选触发单组合）。
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
     * ALO (All Or None) ：订单要求全部成交，否则将被取消。
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


    /**
     * 获取限价单类型。
     *
     * @return Optional 包装的限价单类型
     */
    public Optional<LimitOrderType> getLimit() {
        return Optional.ofNullable(limit);
    }

    /**
     * 获取触发单类型。
     *
     * @return Optional 包装的触发单类型
     */
    public Optional<TriggerOrderType> getTrigger() {
        return Optional.ofNullable(trigger);
    }
}

