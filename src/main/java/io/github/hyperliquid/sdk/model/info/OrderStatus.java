package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/** 订单状态返回封装 */
public class OrderStatus {

    /** 顶层状态（如 "ok"/"error"） */
    private String status;

    /** 订单详情与状态时间戳 */
    private Order order;

    public static class Order {
        /** 订单详情 */
        private OrderDetail order;
        /** 状态更新时间戳（毫秒） */
        private Long statusTimestamp;

        public static class OrderDetail {
            /** 币种名称 */
            private String coin;
            /** 方向（A/B 或 Buy/Sell） */
            private String side;
            /** 限价（字符串） */
            private String limitPx;
            /** 下单数量（字符串） */
            private String sz;
            /** 订单 ID */
            private Long oid;
            /** 创建时间戳（毫秒） */
            private Long timestamp;
            /** 触发条件描述 */
            private String triggerCondition;
            /** 是否为触发单 */
            private Boolean isTrigger;
            /** 触发价格（字符串） */
            private String triggerPx;
            /** 子订单 ID 列表（若拆分/切片） */
            private List<String> children;
            /** 是否为仓位止盈止损 */
            private Boolean isPositionTpsl;
            /** 是否仅减仓 */
            private Boolean reduceOnly;
            /** 订单类型描述 */
            private String orderType;
            /** 原始下单数量（字符串） */
            private String origSz;
            /** TIF 策略（Gtc/Alo/Ioc） */
            private String tif;
            /** 客户端订单 ID */
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