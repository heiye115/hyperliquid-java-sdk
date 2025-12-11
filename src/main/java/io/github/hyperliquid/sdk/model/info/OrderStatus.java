package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/** Order status return wrapper */
public class OrderStatus {

    /** Top-level status (e.g., "ok"/"error") */
    private String status;

    /** Order details and status timestamp */
    private Order order;

    public static class Order {
        /** Order details */
        private OrderDetail order;
        /** Status update timestamp (milliseconds) */
        private Long statusTimestamp;

        public static class OrderDetail {
            /** Currency name */
            private String coin;
            /** Direction (A/B or Buy/Sell) */
            private String side;
            /** Limit price (string) */
            private String limitPx;
            /** Order quantity (string) */
            private String sz;
            /** Order ID */
            private Long oid;
            /** Creation timestamp (milliseconds) */
            private Long timestamp;
            /** Trigger condition description */
            private String triggerCondition;
            /** Whether it is a trigger order */
            private Boolean isTrigger;
            /** Trigger price (string) */
            private String triggerPx;
            /** Child order ID list (if split/sliced) */
            private List<String> children;
            /** Whether it is a position take-profit/stop-loss */
            private Boolean isPositionTpsl;
            /** Whether to reduce position only */
            private Boolean reduceOnly;
            /** Order type description */
            private String orderType;
            /** Original order quantity (string) */
            private String origSz;
            /** TIF strategy (Gtc/Alo/Ioc) */
            private String tif;
            /** Client order ID */
            private String cloid;

            // Getter and Setter methods
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

            public Boolean getIsTrigger() {
                return isTrigger;
            }

            public void setIsTrigger(Boolean isTrigger) {
                this.isTrigger = isTrigger;
            }

            public String getTriggerPx() {
                return triggerPx;
            }

            public void setTriggerPx(String triggerPx) {
                this.triggerPx = triggerPx;
            }

            public List<String> getChildren() {
                return children;
            }

            public void setChildren(List<String> children) {
                this.children = children;
            }

            public Boolean getIsPositionTpsl() {
                return isPositionTpsl;
            }

            public void setIsPositionTpsl(Boolean isPositionTpsl) {
                this.isPositionTpsl = isPositionTpsl;
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

        // Getter and Setter methods
        public OrderDetail getOrder() {
            return order;
        }

        public void setOrder(OrderDetail order) {
            this.order = order;
        }

        public Long getStatusTimestamp() {
            return statusTimestamp;
        }

        public void setStatusTimestamp(Long statusTimestamp) {
            this.statusTimestamp = statusTimestamp;
        }
    }

    // Getter and Setter methods
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}