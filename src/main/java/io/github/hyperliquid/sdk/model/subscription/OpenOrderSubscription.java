package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenOrderSubscription extends Subscription {

    @JsonProperty("type")
    private final String type = "openOrders";

    @JsonProperty("user")
    private String user;

    public OpenOrderSubscription(String user) {
        this.user = user;
    }

    public static OpenOrderSubscription of(String user) {
        return new OpenOrderSubscription(user);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String toIdentifier() {
        return type + ":" + user;
    }
}
