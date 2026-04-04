package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response model for {@link io.github.hyperliquid.sdk.apis.Exchange#modifyOrder(ModifyOrderRequest)} and
 * {@link io.github.hyperliquid.sdk.apis.Exchange#modifyOrders(java.util.List)}.
 */
public class ModifyOrder {

    /**
     * Top-level status string from the exchange (e.g. {@code "ok"} or {@code "err"}).
     */
    private String status;

    /**
     * Raw JSON payload returned by the API for the modify operation.
     */
    private JsonNode response;

    /**
     * @return Status field from the exchange response
     */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return Parsed JSON body of the response
     */
    public JsonNode getResponse() {
        return response;
    }

    public void setResponse(JsonNode response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "ModifyOrder{" +
                "status='" + status + '\'' +
                ", response=" + response +
                '}';
    }
}
