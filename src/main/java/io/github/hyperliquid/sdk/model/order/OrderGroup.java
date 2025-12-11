package io.github.hyperliquid.sdk.model.order;

import java.util.List;

/**
 * Order group, containing order list and grouping type information.
 * <p>
 * Used to automatically infer the grouping parameter of bulkOrders, simplifying API calls.
 * </p>
 */
public class OrderGroup {
    /**
     * Order list
     */
    private final List<OrderRequest> orders;

    /**
     * Grouping type
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
