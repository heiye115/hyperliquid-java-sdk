package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 订单线缆（wire）内部表示，用于构造最终发送给服务端的动作负载。
 * 说明：
 * - 本类使用语义化字段名（coin/isBuy/sz/limitPx/orderType/reduceOnly/cloid）在 Java 内部承载订单数据；
 * - 在实际序列化为 L1 动作时，会被转换为官方线缆键名：
 * a(资产ID), b(是否买), p(限价), s(数量), r(仅减仓), t(订单类型), c(客户端订单ID)；
 * - 转换逻辑见 `io.github.hyperliquid.sdk.utils.Signing.orderWiresToOrderAction` 与 `writeMsgpack`；
 * - `sz` 与 `limitPx` 字段为字符串形式，需通过 `Signing.floatToWire` 规范化以避免不可接受的舍入。
 */
public class OrderWire {
    /**
     * 资产整数 ID（Perp 为合约资产，Spot 为现货资产）
     */
    public final Integer coin;
    /**
     * 是否买入（true=买/做多，false=卖/做空）
     */
    public final Boolean isBuy;
    /**
     * 订单数量（字符串形式，由浮点经规范化得到）
     */
    public final String sz; // 转换为字符串表示
    /**
     * 限价（字符串或 null，市价/触发市价时可为空）
     */
    public final String limitPx; // 字符串（或 null）
    /**
     * 原始订单类型结构（Map/POJO），包含 limit(tif) 或 trigger(triggerPx/isMarket/tpsl)
     */
    public final Object orderType; // Map/POJO 结构，符合交易接口的 wire 格式
    /**
     * 仅减仓标记（true 表示不增加仓位，仅减少既有仓位）
     */
    public final Boolean reduceOnly;
    /**
     * 客户端订单 ID（Cloid，0x + 32 十六进制字符），可为 null
     */
    public final Cloid cloid; // 可为 null

    /**
     * 构造方法（支持 Jackson 反序列化）。
     *
     * @param coin       资产整数 ID
     * @param isBuy      是否买入（true=买/多；false=卖/空）
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
