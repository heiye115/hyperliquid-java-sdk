package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Response model for {@link io.github.hyperliquid.sdk.apis.Exchange#cancel(String, long)},
 * {@link io.github.hyperliquid.sdk.apis.Exchange#cancels(java.util.List)}, and cancel-by-cloid variants.
 */
public class Cancel {

    /**
     * Top-level status string from the exchange (e.g. {@code "ok"} or {@code "err"}).
     */
    private String status;

    /**
     * Structured response body when status is successful.
     */
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

    @Override
    public String toString() {
        return "Cancel{" +
                "status='" + status + '\'' +
                ", response=" + response +
                '}';
    }

    /**
     * Nested payload for a successful cancel response.
     */
    public static class Response {

        /**
         * Response type discriminator (typically {@code "cancel"}).
         */
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

        @Override
        public String toString() {
            return "Response{" +
                    "type='" + type + '\'' +
                    ", data=" + data +
                    '}';
        }

        /**
         * Per-order cancel outcomes from the API.
         */
        public static class Data {

            private List<JsonNode> statuses;

            /**
             * @return One JSON node per cancel in the batch (order matches request order)
             */
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

    }


}
