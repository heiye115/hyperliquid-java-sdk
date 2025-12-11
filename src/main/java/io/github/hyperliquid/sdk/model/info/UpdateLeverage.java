package io.github.hyperliquid.sdk.model.info;

/** Update leverage operation return */
public class UpdateLeverage {
    /** Top-level status (e.g., "ok"/"error") */
    private String status;
    /** Response body (type, etc.) */
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

    public static class Response {
        /** Response type description */
        private String type;

        // Getter and Setter methods
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}