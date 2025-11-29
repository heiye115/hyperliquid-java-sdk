package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Candle（K 线）类型模型。
 */
public class Candle {

    /**
     * 结束时间戳（毫秒）
     */
    @JsonProperty("T")
    private Long endTimestamp;

    /**
     * 起始时间戳（毫秒）
     */
    @JsonProperty("t")
    private Long startTimestamp;

    /**
     * 收盘价
     */
    @JsonProperty("c")
    private String closePrice;

    /**
     * 最高价
     */
    @JsonProperty("h")
    private String highPrice;

    /**
     * 最低价
     */
    @JsonProperty("l")
    private String lowPrice;

    /**
     * 开盘价
     */
    @JsonProperty("o")
    private String openPrice;

    /**
     * 交易量
     */
    @JsonProperty("v")
    private String volume;

    /**
     * 时间间隔（如 "1m"、"15m"、"1h"、"1d" 等）
     */
    @JsonProperty("i")
    private String interval;

    /**
     * 交易对符号（如 "BTC"）
     */
    @JsonProperty("s")
    private String symbol;

    /**
     * 交易次数
     */
    @JsonProperty("n")
    private Integer tradeCount;

    // Getter and Setter methods
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
}