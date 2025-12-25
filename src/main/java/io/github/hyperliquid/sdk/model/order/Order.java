package io.github.hyperliquid.sdk.model.order;

import java.util.List;

/**
 * Order response encapsulation (contains resting/filled/error status)
 */
public class Order {

    /**
     * Top-level status (e.g., "ok"/"error")
     */
    private String status;

    /**
     * Response body, contains type and data
     */
    private Response response;

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

    @Override
    public String toString() {
        return "Order{" +
                "status='" + status + '\'' +
                ", response=" + response +
                '}';
    }

    public static class Resting {
        /**
         * Resting order ID
         */
        private long oid;

        /**
         * Client order ID
         */
        private String cloid;

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

        @Override
        public String toString() {
            return "Resting{" +
                    "oid=" + oid +
                    ", cloid='" + cloid + '\'' +
                    '}';
        }
    }

    public static class Statuses {
        /**
         * Unfilled resting order information
         */
        private Resting resting;
        /**
         * Filled order information
         */
        private Filled filled;
        /**
         * Error description (if any)
         */
        private String error;

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

        @Override
        public String toString() {
            return "Statuses{" +
                    "resting=" + resting +
                    ", filled=" + filled +
                    ", error='" + error + '\'' +
                    '}';
        }
    }


    public static class Filled {
        /**
         * Total filled quantity (string)
         */
        private String totalSz;
        /**
         * Average filled price (string)
         */
        private String avgPx;
        /**
         * Order ID
         */
        private Long oid;
        /**
         * Client order ID
         */
        private String cloid;

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

        @Override
        public String toString() {
            return "Filled{" +
                    "totalSz='" + totalSz + '\'' +
                    ", avgPx='" + avgPx + '\'' +
                    ", oid=" + oid +
                    ", cloid='" + cloid + '\'' +
                    '}';
        }
    }

    public static class Data {
        /**
         * List of order statuses
         */
        private List<Statuses> statuses;

        public List<Statuses> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<Statuses> statuses) {
            this.statuses = statuses;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "statuses=" + statuses +
                    '}';
        }
    }

    public static class Response {
        /**
         * Response type (e.g., "order")
         */
        private String type;
        /**
         * Order status data
         */
        private Data data;

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

        @Override
        public String toString() {
            return "Response{" +
                    "type='" + type + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}