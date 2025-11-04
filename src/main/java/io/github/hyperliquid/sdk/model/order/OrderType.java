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

