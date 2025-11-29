package io.github.hyperliquid.sdk.model.order;

import java.util.List;

/**
 * 下单响应封装（包含 resting/filled/error 状态）
 */
public class Order {

    /**
     * 顶层状态（如 "ok"/"error"）
     */
    private String status;

    /**
     * 响应体，包含类型与数据
     */
    private Response response;

    // Getter and Setter methods
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public static class Resting {
        /**
         * 挂单订单 ID
         */
        private long oid;
        
        /**
         * 客户端订单 ID
         */
        private String cloid;

        // Getter and Setter methods
        public long getOid() {
            return oid;
        }

        public void setOid(long oid) {
            this.oid = oid;
        }

        public String getCloid() {
            return cloid;
        }

        public void setCloid(String cloid) {
            this.cloid = cloid;
        }
    }

    public static class Statuses {
        /**
         * 未成交挂单信息
         */
        private Resting resting;
        /**
         * 已成交信息
         */
        private Filled filled;
        /**
         * 错误描述（若有）
         */
        private String error;

        // Getter and Setter methods
        public Resting getResting() {
            return resting;
        }

        public void setResting(Resting resting) {
            this.resting = resting;
        }

        public Filled getFilled() {
            return filled;
        }

        public void setFilled(Filled filled) {
            this.filled = filled;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }


    public static class Filled {
        /**
         * 成交总数量（字符串）
         */
        private String totalSz;
        /**
         * 成交均价（字符串）
         */
        private String avgPx;
        /**
         * 订单 ID
         */
        private Long oid;
        /**
         * 客户端订单 ID
         */
        private String cloid;

        // Getter and Setter methods
        public String getTotalSz() {
            return totalSz;
        }

        public void setTotalSz(String totalSz) {
            this.totalSz = totalSz;
        }

        public String getAvgPx() {
            return avgPx;
        }

        public void setAvgPx(String avgPx) {
            this.avgPx = avgPx;
        }

        public Long getOid() {
            return oid;
        }

        public void setOid(Long oid) {
            this.oid = oid;
        }

        public String getCloid() {
            return cloid;
        }

        public void setCloid(String cloid) {
            this.cloid = cloid;
        }
    }

    public static class Data {
        /**
         * 各订单状态列表
         */
        private List<Statuses> statuses;

        // Getter and Setter methods
        public List<Statuses> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<Statuses> statuses) {
            this.statuses = statuses;
        }
    }

    public static class Response {
        /**
         * 响应类型（如 "order"）
         */
        private String type;
        /**
         * 订单状态数据
         */
        private Data data;

        // Getter and Setter methods
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }
    }
}