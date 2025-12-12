package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * Tick-by-tick trade subscription.
 * <p>
 * Subscribe to each real-time trade data for specified currency, including price, quantity, direction, and time.
 * </p>
 */
public class TradesSubscription extends Subscription {
    
    @JsonProperty("type")
    private final String type = "trades";
    
    @JsonProperty("coin")
    private String coin;
    
    /**
     * Construct tick-by-tick trade subscription (no-argument constructor, used for Jackson deserialization).
     */
    public TradesSubscription() {
    }
    
    /**
     * Construct tick-by-tick trade subscription.
     *
     * @param coin currency name (e.g., "BTC", "ETH") or asset ID
     */
    public TradesSubscription(String coin) {
        this.coin = coin;
    }
    
    /**
     * Static factory method: create tick-by-tick trade subscription.
     *
     * @param coin currency name (e.g., "BTC", "ETH")
     * @return TradesSubscription instance
     */
    public static TradesSubscription of(String coin) {
        return new TradesSubscription(coin);
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
    
    @Override
    public String toIdentifier() {
        if (coin == null) {
            return type;
        }
        String coinKey = coin.toLowerCase(Locale.ROOT);
        return type + ":" + coinKey;
    }
}
