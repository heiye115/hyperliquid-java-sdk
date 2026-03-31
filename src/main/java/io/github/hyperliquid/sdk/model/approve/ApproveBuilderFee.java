package io.github.hyperliquid.sdk.model.approve;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Approve a builder fee result
 */
public class ApproveBuilderFee {

    /**
     * Status ok/ err
     */
    private String status;

    /**
     * Response
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
