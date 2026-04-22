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
    @JsonProperty("dex")
    private final String dex;

    /**
     * Construct all currencies mid price subscription.
     */
    public AllMidsSubscription() {
        //Empty string is HL
        this.dex = "";
    }

    /**
     * Construct all currencies mid price subscription by dex
     */
    public AllMidsSubscription(String dex) {
        this.dex = dex;
    }

    /**
     * Static factory method: create all currencies mid price subscription.
     *
     * @return AllMidsSubscription instance
     */
    public static AllMidsSubscription create() {
        return new AllMidsSubscription();
    }

    /**
     * Static factory method: create all currencies mid price subscription for a specific dex.
     *
     * @return AllMidsSubscription instance
     */
    public static AllMidsSubscription create(String dex) {
        return new AllMidsSubscription(dex);
    }

    @Override
    public String getType() {
        return type;
    }
}
