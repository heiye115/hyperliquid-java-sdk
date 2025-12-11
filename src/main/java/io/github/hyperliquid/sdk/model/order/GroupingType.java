package io.github.hyperliquid.sdk.model.order;

/**
 * Order grouping type enum.
 * <p>
 * Used to specify the grouping type of orders, mainly applied to take-profit/stop-loss (TP/SL) orders.
 * </p>
 */
public enum GroupingType {

    /**
     * Normal order (no grouping).
     * Usage scenarios:
     * ✅ Single normal order (open, close, limit, market, etc.)
     * ✅ Batch orders with no association between orders
     * ✅ Any order that doesn't need TP/SL
     */
    NA("na"),

    /**
     * Normal take-profit/stop-loss group.
     * Usage scenarios:
     * ✅ Open position and set TP/SL simultaneously
     * ✅ Batch orders: 1 open order + 1 or 2 take-profit/stop-loss orders
     */
    NORMAL_TPSL("normalTpsl"),

    /**
     * Position take-profit/stop-loss group.
     * <p>
     * Usage scenarios:
     * ✅ Set or modify TP/SL for existing positions
     * ✅ Don't open new positions, only set protection for existing positions
     * </p>
     */
    POSITION_TPSL("positionTpsl");

    private final String value;

    GroupingType(String value) {
        this.value = value;
    }

    /**
     * Get the string value of the grouping type.
     *
     * @return string value of the grouping type
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the corresponding enum instance based on string value.
     *
     * @param value string value
     * @return corresponding enum instance, returns NA if not found
     */
    public static GroupingType fromValue(String value) {
        for (GroupingType type : GroupingType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return NA; // Default return NA
    }

    @Override
    public String toString() {
        return value;
    }
}