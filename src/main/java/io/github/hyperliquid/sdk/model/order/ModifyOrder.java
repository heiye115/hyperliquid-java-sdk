package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.databind.JsonNode;

public class ModifyOrder {

    private String status;

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

    @Override
    public String toString() {
        return "ModifyOrder{" +
                "status='" + status + '\'' +
                ", response=" + response +
                '}';
    }
}
