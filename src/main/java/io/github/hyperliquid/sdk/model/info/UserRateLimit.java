package io.github.hyperliquid.sdk.model.info;

/** 用户速率限制信息 */
public class UserRateLimit {
    /** 累计交易量（字符串） */
    private String cumVlm;
    /** 已使用的请求次数 */
    private Long nRequestsUsed;
    /** 请求次数上限 */
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