package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Order wire (wire) internal representation, used to construct the final action payload sent to the server.
 * Note:
 * - This class uses semantic field names (coin/isBuy/sz/limitPx/orderType/reduceOnly/cloid) to carry order data internally in Java;
 * - When actually serialized to L1 action, it will be converted to official wire key names:
 * a(assetID), b(whether to buy), p(limit price), s(quantity), r(reduce only), t(order type), c(client order ID);
 * - Conversion logic see `io.github.hyperliquid.sdk.utils.Signing.orderWiresToOrderAction` and `writeMsgpack`;
 * - `sz` and `limitPx` fields are in string form, need to be normalized via `Signing.floatToWire` to avoid unacceptable rounding.
 */
public class OrderWire {
    /**
     * Asset integer ID (Perp for contract assets, Spot for spot assets)
     */
    public final Integer coin;
    /**
     * Whether to buy (true=buy/long, false=sell/short)
     */
    public final Boolean isBuy;
    /**
     * Order quantity (string form, obtained from float after normalization)
     */
    public final String sz; // Converted to string representation
    /**
     * Limit price (string or null, can be empty for market/trigger market)
     */
    public final String limitPx; // String (or null)
    /**
     * Original order type structure (Map/POJO), containing limit(tif) or trigger(triggerPx/isMarket/tpsl)
     */
    public final Object orderType; // Map/POJO structure, conforming to trading interface wire format
    /**
     * Reduce-only flag (true means not to increase position, only reduce existing position)
     */
    public final Boolean reduceOnly;
    /**
     * Client order ID (Cloid, 0x + 32 hexadecimal characters), can be null
     */
    public final Cloid cloid; // Can be null

    /**
     * Constructor method (supports Jackson deserialization).
     *
     * @param coin       asset integer ID
     * @param isBuy      whether to buy (true=buy/long; false=sell/short)
     * @param sz         quantity (string)
     * @param limitPx    limit price (string or null)
     * @param orderType  order type structure (object/map)
     * @param reduceOnly whether to reduce only
     * @param cloid      client order ID (can be null)
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
