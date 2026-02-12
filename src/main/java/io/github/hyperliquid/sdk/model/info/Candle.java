package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Candle (K-line) type model.
 */
@Data
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