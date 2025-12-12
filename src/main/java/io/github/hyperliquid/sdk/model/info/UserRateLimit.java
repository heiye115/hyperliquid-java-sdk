package io.github.hyperliquid.sdk.model.info;

/** User rate limit information */
public class UserRateLimit {
    /** Cumulative trading volume (string) */
    private String cumVlm;
    /** Number of requests used */
    private Long nRequestsUsed;
    /** Request count limit */
    private Long nRequestsCap;

    // Getter and Setter methods
    public String getCumVlm() {
        return cumVlm;
    }

    public void setCumVlm(String cumVlm) {
        this.cumVlm = cumVlm;
    }

    public Long getnRequestsUsed() {
        return nRequestsUsed;
    }

    public void setnRequestsUsed(Long nRequestsUsed) {
        this.nRequestsUsed = nRequestsUsed;
    }

    public Long getnRequestsCap() {
        return nRequestsCap;
    }

    public void setnRequestsCap(Long nRequestsCap) {
        this.nRequestsCap = nRequestsCap;
    }
}