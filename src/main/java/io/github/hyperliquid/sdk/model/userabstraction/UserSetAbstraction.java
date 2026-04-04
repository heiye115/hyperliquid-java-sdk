package io.github.hyperliquid.sdk.model.userabstraction;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response model for {@link io.github.hyperliquid.sdk.apis.Exchange#userSetAbstraction(String, UserAbstractionMode)}.
 */
public class UserSetAbstraction {

    /**
     * Top-level status (typically {@code "ok"}).
     */
    private String status;

    /**
     * Raw JSON response from the exchange.
     */
    private JsonNode response;

    /**
     * Optional structured subtype field when the server returns a typed wrapper.
     */
    public static class Response {

        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JsonNode getResponse() {
        return response;
    }

    public void setResponse(JsonNode response) {
        this.response = response;
    }
}
