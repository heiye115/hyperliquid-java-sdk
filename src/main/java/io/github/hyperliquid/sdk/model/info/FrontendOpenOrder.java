package io.github.hyperliquid.sdk.model.info;

/** 前端未成交订单实体封装（携带触发/止盈止损等额外信息） */
public class FrontendOpenOrder {
    /** 币种（如 "BTC" 或 Spot 索引 "@107"） */
    private String coin;
    /** 是否为仓位止盈止损单 */
    private Boolean isPositionTpsl;
    /** 是否为触发单 */
    private Boolean isTrigger;
    /** 限价（字符串） */
    private String limitPx;
    /** 订单 ID */
    private Long oid;
    /** 订单类型描述 */
    private String orderType;
    /** 原始下单数量（字符串） */
    private String origSz;
    /** 是否仅减仓 */
    private Boolean reduceOnly;
    /** 方向（A/B 或 Buy/Sell） */
    private String side;
    /** 当前剩余数量（字符串） */
    private String sz;
    /** 创建时间戳（毫秒） */
    private Long timestamp;
    /** 触发条件（上穿/下穿等） */
    private String triggerCondition;
    /** 触发价格（字符串） */
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