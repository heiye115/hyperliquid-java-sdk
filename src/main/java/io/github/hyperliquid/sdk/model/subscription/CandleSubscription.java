package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * Candle subscription.
 * <p>
 * Subscribe to candle data for specified currency and time period, including open, high, low, close, and volume.
 * </p>
 */
public class CandleSubscription extends Subscription {
    
    @JsonProperty("type")
    private final String type = "candle";
    
    @JsonProperty("coin")
    private String coin;
    
    @JsonProperty("interval")
    private String interval;
    
    /**
     * Construct candle subscription (no-argument constructor, used for Jackson deserialization).
     */
    public CandleSubscription() {
    }
    
    /**
     * Construct candle subscription.
     *
     * @param coin     currency name (e.g., "BTC", "ETH") or asset ID
     * @param interval time period (e.g., "1m", "5m", "15m", "1h", "1d")
     */
    public CandleSubscription(String coin, String interval) {
        this.coin = coin;
        this.interval = interval;
    }
    
    /**
     * Static factory method: create candle subscription.
     *
     * @param coin     currency name (e.g., "BTC", "ETH")
     * @param interval time period (e.g., "1m", "5m", "15m", "1h", "1d")
     * @return CandleSubscription instance
     */
    public static CandleSubscription of(String coin, String interval) {
        return new CandleSubscription(coin, interval);
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    public String getCoin() {
        return coin;
    }
    
    public void setCoin(String coin) {
        this.coin = coin;
    }
    
    public String getInterval() {
        return interval;
    }
    
    public void setInterval(String interval) {
        this.interval = interval;
    }
    
    @Override
    public String toIdentifier() {
        if (coin == null || interval == null) {
            return type;
        }
        String coinKey = coin.toLowerCase(Locale.ROOT);
        return type + ":" + coinKey + "," + interval;
    }
}
