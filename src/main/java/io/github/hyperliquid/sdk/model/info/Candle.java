package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Candle (K-line) type model.
 */
public class Candle {

    /**
     * End timestamp (milliseconds)
     */
    @JsonProperty("T")
    private Long endTimestamp;

    /**
     * Start timestamp (milliseconds)
     */
    @JsonProperty("t")
    private Long startTimestamp;

    /**
     * Closing price
     */
    @JsonProperty("c")
    private String closePrice;

    /**
     * Highest price
     */
    @JsonProperty("h")
    private String highPrice;

    /**
     * Lowest price
     */
    @JsonProperty("l")
    private String lowPrice;

    /**
     * Opening price
     */
    @JsonProperty("o")
    private String openPrice;

    /**
     * Trading volume
     */
    @JsonProperty("v")
    private String volume;

    /**
     * Time interval (e.g., "1m", "15m", "1h", "1d", etc.)
     */
    @JsonProperty("i")
    private String interval;

    /**
     * Trading pair symbol (e.g., "BTC")
     */
    @JsonProperty("s")
    private String symbol;

    /**
     * Number of trades
     */
    @JsonProperty("n")
    private Integer tradeCount;

    public Long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public String getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(String closePrice) {
        this.closePrice = closePrice;
    }

    public String getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(String highPrice) {
        this.highPrice = highPrice;
    }

    public String getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(String lowPrice) {
        this.lowPrice = lowPrice;
    }

    public String getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(String openPrice) {
        this.openPrice = openPrice;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getTradeCount() {
        return tradeCount;
    }

    public void setTradeCount(Integer tradeCount) {
        this.tradeCount = tradeCount;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "endTimestamp=" + endTimestamp +
                ", startTimestamp=" + startTimestamp +
                ", closePrice='" + closePrice + '\'' +
                ", highPrice='" + highPrice + '\'' +
                ", lowPrice='" + lowPrice + '\'' +
                ", openPrice='" + openPrice + '\'' +
                ", volume='" + volume + '\'' +
                ", interval='" + interval + '\'' +
                ", symbol='" + symbol + '\'' +
                ", tradeCount=" + tradeCount +
                '}';
    }
}