package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Asset context (perpetual) indicator collection */
public class AssetCtx {

    /** Current funding rate (string decimal) */
    @JsonProperty("funding")
    private String funding;

    /** Open interest (nominal USD scale, string) */
    @JsonProperty("openInterest")
    private String openInterest;

    /** Previous day closing price (string) */
    @JsonProperty("prevDayPx")
    private String prevDayPx;

    /** Daily nominal trading volume (USD, string) */
    @JsonProperty("dayNtlVlm")
    private String dayNtlVlm;

    /** Perpetual premium (string) */
    @JsonProperty("premium")
    private String premium;

    /** Oracle price (string) */
    @JsonProperty("oraclePx")
    private String oraclePx;

    /** Mark price (string) */
    @JsonProperty("markPx")
    private String markPx;

    /** Mid price (buy/sell mid price, may be null) */
    @JsonProperty("midPx")
    private String midPx;

    /** Impact prices (estimated buy/sell direction execution impact prices, length is 2) */
    @JsonProperty("impactPxs")
    private List<String> impactPxs;

    /** Daily base quantity trading volume (coin quantity, string) */
    @JsonProperty("dayBaseVlm")
    private String dayBaseVlm;

    /** No-argument constructor */
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