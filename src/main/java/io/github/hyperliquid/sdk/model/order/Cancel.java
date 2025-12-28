package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class Cancel {

    /**
     * The status of the request.
     */
    private String status;

    /**
     * The response of the request.
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

    public static class Response {

        /**
         * The type of the response. "cancel"
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

        public static class Data {

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

    }


}
