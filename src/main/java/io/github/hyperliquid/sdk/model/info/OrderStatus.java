package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Order status return wrapper
 */
public class OrderStatus {

    /**
     * Top-level status (e.g., "ok"/"error")
     */
    private String status;

    /**
     * Order details and status timestamp
     */
    private Order order;

    public static class Order {
        /**
         * Order status description (filled)
         */
        private String status;

        /**
         * Order details
         */
        private OrderDetail order;
        /**
         * Status update timestamp (milliseconds)
         */
        private Long statusTimestamp;

        public static class OrderDetail {
            /**
             * Currency name
             */
            private String coin;
            /**
             * Direction (A/B or Buy/Sell)
             */
            private String side;
            /**
             * Limit price (string)
             */
            private String limitPx;
            /**
             * Order quantity (string)
             */
            private String sz;
            /**
             * Order ID
             */
            private Long oid;
            /**
             * Creation timestamp (milliseconds)
             */
            private Long timestamp;
            /**
             * Trigger condition description
             */
            private String triggerCondition;
            /**
             * Whether it is a trigger order
             */
            private Boolean isTrigger;
            /**
             * Trigger price (string)
             */
            private String triggerPx;
            /**
             * Child order ID list (if split/sliced)
             */
            private List<OrderDetail> children;
            /**
             * Whether it is a position take-profit/stop-loss
             */
            private Boolean isPositionTpsl;
            /**
             * Whether to reduce position only
             */
            private Boolean reduceOnly;
            /**
             * Order type description
             */
            private String orderType;
            /**
             * Original order quantity (string)
             */
            private String origSz;
            /**
             * TIF strategy (Gtc/Alo/Ioc)
             */
            private String tif;
            /**
             * Client order ID
             */
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

            @JsonProperty("isTrigger")
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

            public List<OrderDetail> getChildren() {
                return children;
            }

            public void setChildren(List<OrderDetail> children) {
                this.children = children;
            }

            @JsonProperty("isPositionTpsl")
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

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
