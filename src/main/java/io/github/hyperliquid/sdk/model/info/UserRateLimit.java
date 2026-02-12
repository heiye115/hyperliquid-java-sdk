package io.github.hyperliquid.sdk.model.info;

import lombok.Data;

/** User rate limit information */
@Data
public class UserRateLimit {
    /** Cumulative trading volume (string) */
    private String cumVlm;
    /** Number of requests used */
    private Long nRequestsUsed;
    /** Request count limit */
    private Long nRequestsCap;
}