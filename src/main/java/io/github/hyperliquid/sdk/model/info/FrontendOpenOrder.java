package io.github.hyperliquid.sdk.model.info;

/** Frontend open order entity wrapper (carrying trigger/take-profit/stop-loss and other additional information) */
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
    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public Boolean getIsPositionTpsl() {
        return isPositionTpsl;
    }

    public void setIsPositionTpsl(Boolean isPositionTpsl) {
        this.isPositionTpsl = isPositionTpsl;
    }

    public Boolean getIsTrigger() {
        return isTrigger;
    }

    public void setIsTrigger(Boolean isTrigger) {
        this.isTrigger = isTrigger;
    }

    public String getLimitPx() {
        return limitPx;
    }

    public void setLimitPx(String limitPx) {
        this.limitPx = limitPx;
    }

    public Long getOid() {
        return oid;
    }

    public void setOid(Long oid) {
        this.oid = oid;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrigSz() {
        return origSz;
    }

    public void setOrigSz(String origSz) {
        this.origSz = origSz;
    }

    public Boolean getReduceOnly() {
        return reduceOnly;
    }

    public void setReduceOnly(Boolean reduceOnly) {
        this.reduceOnly = reduceOnly;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getSz() {
        return sz;
    }

    public void setSz(String sz) {
        this.sz = sz;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(String triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public String getTriggerPx() {
        return triggerPx;
    }

    public void setTriggerPx(String triggerPx) {
        this.triggerPx = triggerPx;
    }
}