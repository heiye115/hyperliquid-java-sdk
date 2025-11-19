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

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public String getCoin() {
        return coin;
    }

    public void setFundingRate(String fundingRate) {
        this.fundingRate = fundingRate;
    }

    public String getFundingRate() {
        return fundingRate;
    }

    public void setPremium(String premium) {
        this.premium = premium;
    }

    public String getPremium() {
        return premium;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
