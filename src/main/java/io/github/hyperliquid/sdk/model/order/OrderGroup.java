package io.github.hyperliquid.sdk.model.order;

import java.util.List;

/**
 * 订单组，包含订单列表和分组类型信息。
 * <p>
 * 用于自动推断 bulkOrders 的 grouping 参数，简化API调用。
 * </p>
 */
public class OrderGroup {
    /**
     * 订单列表
     */
    private final List<OrderRequest> orders;

    /**
     * 分组类型
     */
    private final GroupingType groupingType;

    public OrderGroup(List<OrderRequest> orders, GroupingType groupingType) {
        this.orders = orders;
        this.groupingType = groupingType;
    }

    public List<OrderRequest> getOrders() {
        return orders;
    }

    public GroupingType getGroupingType() {
        return groupingType;
    }
}
