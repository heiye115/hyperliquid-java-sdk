package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Represents a bulk order response.
 */
public class BulkOrder {

    /**
     * The status of the bulk order. (ok)
     */
    private String status;

    /**
     * The response of the bulk order.
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

    public static class Response {

        private String type;

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

        public static class Data {
            /**
             * The statuses of the orders.
             */
            private List<JsonNode> statuses;

            public List<JsonNode> getStatuses() {
                return statuses;
            }

            public void setStatuses(List<JsonNode> statuses) {
                this.statuses = statuses;
            }

            @Override
            public String toString() {
                return "Data{" +
                        "statuses=" + statuses +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "Response{" +
                    "type='" + type + '\'' +
                    ", data=" + data +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BulkOrder{" +
                "status='" + status + '\'' +
                ", response=" + response +
                '}';
    }
}
