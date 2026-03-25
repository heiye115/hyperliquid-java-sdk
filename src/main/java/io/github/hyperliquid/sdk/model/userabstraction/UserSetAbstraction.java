package io.github.hyperliquid.sdk.model.userabstraction;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * User set abstraction
 */
public class UserSetAbstraction {

    /**
     * Status default "ok"
     */
    private String status;

    /**
     * Response
     */
    private JsonNode response;

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
