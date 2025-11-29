package io.github.hyperliquid.sdk.model.info;

/***
 * 资金费率历史记录。
 **/
public class FundingHistory {

    /**
     * 币种名称
     **/
    private String coin;

    /***
     * 资金费率
     **/
    private String fundingRate;

    /***
     * 溢价率
     **/
    private String premium;

    /***
     * 时间戳（毫秒）
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