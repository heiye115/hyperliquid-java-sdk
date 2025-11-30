package io.github.hyperliquid.sdk.model.order;

/**
 * 订单分组类型枚举。
 * <p>
 * 用于指定订单的分组类型，主要应用于止盈止损（TP/SL）订单。
 * </p>
 */
public enum GroupingType {

    /**
     * 普通订单（无分组）。
     * 使用场景：
     * ✅ 单笔普通订单（开仓、平仓、限价、市价等）
     * ✅ 批量下单但订单之间无关联
     * ✅ 不需要 TP/SL 的任何订单
     */
    NA("na"),

    /**
     * 普通止盈止损组。
     * 使用场景：
     * ✅ 同时开仓并设置 TP/SL
     * ✅ 批量下单：1个开仓订单 + 1个或2个止盈止损订单
     */
    NORMAL_TPSL("normalTpsl"),

    /**
     * 仓位止盈止损组。
     * <p>
     * 使用场景：
     * ✅ 针对已有仓位设置或修改 TP/SL
     * ✅ 不开新仓，只设置现有仓位的保护
     * </p>
     */
    POSITION_TPSL("positionTpsl");

    private final String value;

    GroupingType(String value) {
        this.value = value;
    }

    /**
     * 获取分组类型的字符串值。
     *
     * @return 分组类型的字符串值
     */
    public String getValue() {
        return value;
    }

    /**
     * 根据字符串值获取对应的枚举实例。
     *
     * @param value 字符串值
     * @return 对应的枚举实例，如果未找到则返回 NA
     */
    public static GroupingType fromValue(String value) {
        for (GroupingType type : GroupingType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return NA; // 默认返回 NA
    }

    @Override
    public String toString() {
        return value;
    }
}