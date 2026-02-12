package io.github.hyperliquid.sdk.model.info;

import lombok.Data;

/** Update leverage operation return */
@Data
public class UpdateLeverage {
    /** Top-level status (e.g., "ok"/"error") */
    private String status;
    /** Response body (type, etc.) */
    private Response response;

    @Data
    public static class Response {
        /** Response type description */
        private String type;
    }
}