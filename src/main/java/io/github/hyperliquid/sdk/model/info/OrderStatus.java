package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Order status return wrapper
 */
@Data
public class OrderStatus {

    /**
     * Top-level status (e.g., "ok"/"error")
     */
    private String status;

    /**
     * Order details and status timestamp
     */
    private Order order;

    @Data
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

        @Data
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

            @JsonProperty("isTrigger")
            public Boolean getTrigger() {
                return isTrigger;
            }

            @JsonProperty("isPositionTpsl")
            public Boolean getPositionTpsl() {
                return isPositionTpsl;
            }

            @Override
            public String toString() {
                return "OrderDetail{" +
                        "coin='" + coin + '\'' +
                        ", side='" + side + '\'' +
                        ", limitPx='" + limitPx + '\'' +
                        ", sz='" + sz + '\'' +
                        ", oid=" + oid +
                        ", timestamp=" + timestamp +
                        ", triggerCondition='" + triggerCondition + '\'' +
                        ", isTrigger=" + isTrigger +
                        ", triggerPx='" + triggerPx + '\'' +
                        ", children=" + children +
                        ", isPositionTpsl=" + isPositionTpsl +
                        ", reduceOnly=" + reduceOnly +
                        ", orderType='" + orderType + '\'' +
                        ", origSz='" + origSz + '\'' +
                        ", tif='" + tif + '\'' +
                        ", cloid='" + cloid + '\'' +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "Order{" +
                    "status='" + status + '\'' +
                    ", order=" + order +
                    ", statusTimestamp=" + statusTimestamp +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "OrderStatus{" +
                "status='" + status + '\'' +
                ", order=" + order +
                '}';
    }
}
