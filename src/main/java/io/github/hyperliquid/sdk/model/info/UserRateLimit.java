package io.github.hyperliquid.sdk.model.info;

/**
 * Rate-limit snapshot for a user from the {@code userRateLimit} info request.
 * <p>
 * Fields mirror the exchange JSON: cumulative volume and request usage vs cap.
 * </p>
 */
public class UserRateLimit {
    /** Cumulative trading volume (string, exchange format). */
    private String cumVlm;
    /** Number of API requests consumed in the current window. */
    private Long nRequestsUsed;
    /** Maximum requests allowed in the window. */
    private Long nRequestsCap;

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