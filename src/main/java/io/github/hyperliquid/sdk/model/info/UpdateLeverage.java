package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Update leverage operation return
 */
public class UpdateLeverage {
    /**
     * Top-level status (e.g., "ok"/"error")
     */
    private String status;
    
    /**
     * Response body (type, etc.)
     */
    private JsonNode response;

    // Getter and Setter methods
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