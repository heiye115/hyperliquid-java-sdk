package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpotStateSubscription extends Subscription {

    @JsonProperty("type")
    private final String type = "spotState";

    @JsonProperty("user")
    private String user;

    public SpotStateSubscription(String user) {
        this.user = user;
    }

    public static SpotStateSubscription of(String user) {
        return new SpotStateSubscription(user);
    }

    @Override
    public String getType() {
        return type;
    }
}
