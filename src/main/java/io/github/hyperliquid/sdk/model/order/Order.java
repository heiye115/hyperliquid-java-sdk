package io.github.hyperliquid.sdk.model.order;

import java.util.List;

public class Order {

    private String status;

    private Response response;

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public static class Resting {

        private long oid;

        public void setOid(long oid) {
            this.oid = oid;
        }

        public long getOid() {
            return oid;
        }

    }

    public static class Statuses {
        
        private Resting resting;
        private Filled filled;
        private String error;

        public void setResting(Resting resting) {
            this.resting = resting;
        }

        public Resting getResting() {
            return resting;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Filled getFilled() {
            return filled;
        }

        public void setFilled(Filled filled) {
            this.filled = filled;
        }
    }


    public static class Filled {

        private String totalSz;

        private String avgPx;

        private Long oid;

        private String cloid;

        public void setTotalSz(String totalSz) {
            this.totalSz = totalSz;
        }

        public String getTotalSz() {
            return totalSz;
        }

        public void setAvgPx(String avgPx) {
            this.avgPx = avgPx;
        }

        public String getAvgPx() {
            return avgPx;
        }

        public void setOid(Long oid) {
            this.oid = oid;
        }

        public long getOid() {
            return oid;
        }

        public void setCloid(String cloid) {
            this.cloid = cloid;
        }

        public String getCloid() {
            return cloid;
        }

    }

    public static class Data {

        private List<Statuses> statuses;

        public void setStatuses(List<Statuses> statuses) {
            this.statuses = statuses;
        }

        public List<Statuses> getStatuses() {
            return statuses;
        }
    }

    public static class Response {
        private String type;
        private Data data;

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public Data getData() {
            return data;
        }
    }
}
