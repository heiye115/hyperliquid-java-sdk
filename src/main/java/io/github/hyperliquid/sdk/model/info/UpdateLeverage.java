package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response model for {@link io.github.hyperliquid.sdk.apis.Exchange#updateLeverage(String, boolean, int)}.
 */
public class UpdateLeverage {
    /**
     * Top-level status (e.g. {@code "ok"} or {@code "err"}).
     */
    private String status;
    
    /**
     * Raw JSON response body from the exchange.
     */
    private JsonNode response;

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