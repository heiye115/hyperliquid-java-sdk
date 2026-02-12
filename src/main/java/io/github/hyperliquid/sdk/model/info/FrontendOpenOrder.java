package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Frontend open order entity wrapper (carrying trigger/take-profit/stop-loss and other additional information) */
@Data
@ToString
@EqualsAndHashCode
public class FrontendOpenOrder {
    /** Currency (e.g., "BTC" or Spot index "@107") */
    private String coin;
    /** Whether it is a position take-profit/stop-loss order */
    private Boolean isPositionTpsl;
    /** Whether it is a trigger order */
    private Boolean isTrigger;
    /** Limit price (string) */
    private String limitPx;
    /** Order ID */
    private Long oid;
    /** Order type description */
    private String orderType;
    /** Original order quantity (string) */
    private String origSz;
    /** Whether to reduce position only */
    private Boolean reduceOnly;
    /** Direction (A/B or Buy/Sell) */
    private String side;
    /** Current remaining quantity (string) */
    private String sz;
    /** Creation timestamp (milliseconds) */
    private Long timestamp;
    /** Trigger condition (cross above/cross below, etc.) */
    private String triggerCondition;
    /** Trigger price (string) */
    private String triggerPx;

    // Getter and Setter methods
    @JsonProperty("isPositionTpsl")
    public Boolean getIsPositionTpsl() {
        return isPositionTpsl;
    }

    @JsonProperty("isTrigger")
    public Boolean getIsTrigger() {
        return isTrigger;
    }
}
