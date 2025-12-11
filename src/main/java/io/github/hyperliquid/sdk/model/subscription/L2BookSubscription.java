package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * L2 order book subscription.
 * <p>
 * Subscribe to complete order book depth data for specified currency, including bid/ask prices and quantities.
 * </p>
 */
public class L2BookSubscription extends Subscription {
    
    @JsonProperty("type")
    private final String type = "l2Book";
    
    @JsonProperty("coin")
    private String coin;
    
    /**
     * Construct L2 order book subscription (no-argument constructor, used for Jackson deserialization).
     */
    public L2BookSubscription() {
    }
    
    /**
     * Construct L2 order book subscription.
     *
     * @param coin currency name (e.g., "BTC", "ETH") or asset ID
     */
    public L2BookSubscription(String coin) {
        this.coin = coin;
    }
    
    /**
     * Static factory method: create L2 order book subscription.
     *
     * @param coin currency name (e.g., "BTC", "ETH")
     * @return L2BookSubscription instance
     */
    public static L2BookSubscription of(String coin) {
        return new L2BookSubscription(coin);
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
