package io.github.hyperliquid.sdk.model.info;

/***
 * Funding rate history.
 **/
public class FundingHistory {

    /**
     * Currency name
     **/
    private String coin;

    /***
     * Funding rate
     **/
    private String fundingRate;

    /***
     * Premium rate
     **/
    private String premium;

    /***
     * Timestamp (milliseconds)
     **/
    private Long time;

    // Getter and Setter methods
    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public String getFundingRate() {
        return fundingRate;
    }

    public void setFundingRate(String fundingRate) {
        this.fundingRate = fundingRate;
    }

    public String getPremium() {
        return premium;
    }

    public void setPremium(String premium) {
        this.premium = premium;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}