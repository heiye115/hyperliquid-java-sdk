package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Retrieve a user's historical orders
 * Returns at most 2000 most recent historical orders
 **/
public class HistoricalOrders {

    private Order order;
    private String status;
    private Long statusTimestamp;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(Long statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public static class Order {
        private String coin;
        private String side;
        private String limitPx;
        private String sz;
        private Long oid;
        private Long timestamp;
        private String triggerCondition;
        @JsonProperty("isTrigger")
        private Boolean isTrigger;
        private String triggerPx;
        private List<Order> children;
        @JsonProperty("isPositionTpsl")
        private Boolean isPositionTpsl;
        private Boolean reduceOnly;
        private String orderType;
        private String origSz;
        private String tif;
        private String cloid;

        public String getCoin() {
            return coin;
        }

        public void setCoin(String coin) {
            this.coin = coin;
        }

        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }

        public String getLimitPx() {
            return limitPx;
        }

        public void setLimitPx(String limitPx) {
            this.limitPx = limitPx;
        }

        public String getSz() {
            return sz;
        }

        public void setSz(String sz) {
            this.sz = sz;
        }

        public Long getOid() {
            return oid;
        }

        public void setOid(Long oid) {
            this.oid = oid;
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

        public Boolean getTrigger() {
            return isTrigger;
        }

        public void setTrigger(Boolean trigger) {
            isTrigger = trigger;
        }

        public String getTriggerPx() {
            return triggerPx;
        }

        public void setTriggerPx(String triggerPx) {
            this.triggerPx = triggerPx;
        }

        public List<Order> getChildren() {
            return children;
        }

        public void setChildren(List<Order> children) {
            this.children = children;
        }

        public Boolean getPositionTpsl() {
            return isPositionTpsl;
        }

        public void setPositionTpsl(Boolean positionTpsl) {
            isPositionTpsl = positionTpsl;
        }

        public Boolean getReduceOnly() {
            return reduceOnly;
        }

        public void setReduceOnly(Boolean reduceOnly) {
            this.reduceOnly = reduceOnly;
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

        public String getTif() {
            return tif;
        }

        public void setTif(String tif) {
            this.tif = tif;
        }

        public String getCloid() {
            return cloid;
        }

        public void setCloid(String cloid) {
            this.cloid = cloid;
        }
    }
}
