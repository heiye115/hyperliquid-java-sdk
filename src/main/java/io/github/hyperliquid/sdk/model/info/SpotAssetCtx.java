package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpotAssetCtx {
    @JsonProperty("dayNtlVlm")
    private String dayNtlVlm;

    @JsonProperty("markPx")
    private String markPx;

    @JsonProperty("midPx")
    private String midPx;

    @JsonProperty("prevDayPx")
    private String prevDayPx;

    @JsonProperty("circulatingSupply")
    private String circulatingSupply;

    @JsonProperty("coin")
    private String coin;

    public String getDayNtlVlm() {
        return dayNtlVlm;
    }

    public void setDayNtlVlm(String dayNtlVlm) {
        this.dayNtlVlm = dayNtlVlm;
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

    public String getPrevDayPx() {
        return prevDayPx;
    }

    public void setPrevDayPx(String prevDayPx) {
        this.prevDayPx = prevDayPx;
    }

    public String getCirculatingSupply() {
        return circulatingSupply;
    }

    public void setCirculatingSupply(String circulatingSupply) {
        this.circulatingSupply = circulatingSupply;
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }
}
