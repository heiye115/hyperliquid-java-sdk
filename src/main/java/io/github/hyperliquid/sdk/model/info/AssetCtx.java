package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 资产上下文（永续）指标集合 */
public class AssetCtx {

    /** 当前资金费率（字符串小数） */
    @JsonProperty("funding")
    private String funding;

    /** 未平仓量（名义美元规模，字符串） */
    @JsonProperty("openInterest")
    private String openInterest;

    /** 前一日收盘价（字符串） */
    @JsonProperty("prevDayPx")
    private String prevDayPx;

    /** 当日名义成交量（美元，字符串） */
    @JsonProperty("dayNtlVlm")
    private String dayNtlVlm;

    /** 永续溢价（字符串） */
    @JsonProperty("premium")
    private String premium;

    /** 预言机价格（字符串） */
    @JsonProperty("oraclePx")
    private String oraclePx;

    /** 标记价格（字符串） */
    @JsonProperty("markPx")
    private String markPx;

    /** 中间价（买卖中间价，可能为 null） */
    @JsonProperty("midPx")
    private String midPx;

    /** 冲击价格（估算买/卖方向成交影响价，长度为 2） */
    @JsonProperty("impactPxs")
    private List<String> impactPxs;

    /** 当日基础数量成交量（币的数量，字符串） */
    @JsonProperty("dayBaseVlm")
    private String dayBaseVlm;

    /** 无参构造函数 */
    public AssetCtx() {
    }

    // Getter and Setter methods
    public String getFunding() {
        return funding;
    }

    public void setFunding(String funding) {
        this.funding = funding;
    }

    public String getOpenInterest() {
        return openInterest;
    }

    public void setOpenInterest(String openInterest) {
        this.openInterest = openInterest;
    }

    public String getPrevDayPx() {
        return prevDayPx;
    }

    public void setPrevDayPx(String prevDayPx) {
        this.prevDayPx = prevDayPx;
    }

    public String getDayNtlVlm() {
        return dayNtlVlm;
    }

    public void setDayNtlVlm(String dayNtlVlm) {
        this.dayNtlVlm = dayNtlVlm;
    }

    public String getPremium() {
        return premium;
    }

    public void setPremium(String premium) {
        this.premium = premium;
    }

    public String getOraclePx() {
        return oraclePx;
    }

    public void setOraclePx(String oraclePx) {
        this.oraclePx = oraclePx;
    }

    public String getMarkPx() {
        return markPx;
    }

    public void setMarkPx(String markPx) {
        this.markPx = markPx;
    }

    public String getMidPx() {
        return midPx;
    }

    public void setMidPx(String midPx) {
        this.midPx = midPx;
    }

    public List<String> getImpactPxs() {
        return impactPxs;
    }

    public void setImpactPxs(List<String> impactPxs) {
        this.impactPxs = impactPxs;
    }

    public String getDayBaseVlm() {
        return dayBaseVlm;
    }

    public void setDayBaseVlm(String dayBaseVlm) {
        this.dayBaseVlm = dayBaseVlm;
    }
}