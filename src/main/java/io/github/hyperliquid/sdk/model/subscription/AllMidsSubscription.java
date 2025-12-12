package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * All currencies mid price subscription.
 * <p>
 * Subscribe to mid prices (average of best bid and ask prices) for all currencies, used for quick market overview.
 * </p>
 */
public class AllMidsSubscription extends Subscription {
    
    @JsonProperty("type")
    private final String type = "allMids";
    
    /**
     * Construct all currencies mid price subscription.
     */
    public AllMidsSubscription() {
    }
    
    /**
     * Static factory method: create all currencies mid price subscription.
     *
     * @return AllMidsSubscription instance
     */
    public static AllMidsSubscription create() {
        return new AllMidsSubscription();
    }
    
    @Override
    public String getType() {
        return type;
    }
}
