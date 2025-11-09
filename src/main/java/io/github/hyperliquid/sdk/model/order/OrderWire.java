package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 订单线缆（wire）格式，用于实际签名与发送。
 * 字段设计遵循官方 wire 格式：
 * - coin：币种整数 ID；
 * - isBuy：买入/卖出；
 * - sz：订单数量（字符串，便于精度控制与统一处理）；
 * - limitPx：限价（字符串，可为 null）；
 * - orderType：订单类型（对象/映射，兼容各种格式）；
 * - reduceOnly：仅减仓标记；
 * - cloid：客户端订单 ID（可为 null）。
 * </p>
 */
public class OrderWire {

    /**
     * 币种整数 ID
     */
    public final Integer coin;
    /**
     * 是否买入
     */
    public final Boolean isBuy;
    /**
     * 订单数量（字符串形式）
     */
    public final String sz; // 转换为字符串表示
    /**
     * 限价（字符串或 null）
     */
    public final String limitPx; // 字符串（或 null）
    /**
     * 原始订单类型结构
     */
    public final Object orderType; // Map/POJO 结构，符合交易接口的 wire 格式
    /**
     * 仅减仓标记
     */
    public final Boolean reduceOnly;
    /**
     * 客户端订单 ID
     */
    public final Cloid cloid; // 可为 null

    /**
     * Jackson 反序列化构造方法。
     *
     * @param coin       币种整数 ID
     * @param isBuy      是否买入
     * @param sz         数量（字符串）
     * @param limitPx    限价（字符串或 null）
     * @param orderType  订单类型结构（对象/映射）
     * @param reduceOnly 是否仅减仓
     * @param cloid      客户端订单 ID（可为 null）
     */
    @JsonCreator
    public OrderWire(@JsonProperty("coin") Integer coin,
                     @JsonProperty("isBuy") Boolean isBuy,
                     @JsonProperty("sz") String sz,
                     @JsonProperty("limitPx") String limitPx,
                     @JsonProperty("orderType") Object orderType,
                     @JsonProperty("reduceOnly") Boolean reduceOnly,
                     @JsonProperty("cloid") Cloid cloid) {
        this.coin = coin;
        this.isBuy = isBuy;
        this.sz = sz;
        this.limitPx = limitPx;
        this.orderType = orderType;
        this.reduceOnly = reduceOnly;
        this.cloid = cloid;
    }
}
