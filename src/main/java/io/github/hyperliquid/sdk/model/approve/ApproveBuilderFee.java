package io.github.hyperliquid.sdk.model.approve;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response body for {@link io.github.hyperliquid.sdk.apis.Exchange#approveBuilderFee(String, String)}:
 * top-level {@code status} plus raw JSON {@code response} from the exchange.
 */
public class ApproveBuilderFee {

    /**
     * Top-level status ({@code ok} / {@code err}).
     */
    private String status;

    /**
     * Parsed JSON payload for the approve-builder-fee action.
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
