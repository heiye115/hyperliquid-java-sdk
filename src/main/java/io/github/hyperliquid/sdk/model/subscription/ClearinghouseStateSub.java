package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClearinghouseStateSub extends Subscription {

    @JsonProperty("type")
    private final String type = "clearinghouseState";

    @JsonProperty("user")
    private String user;


    public ClearinghouseStateSub(String user) {
        this.user = user;
    }

    public static ClearinghouseStateSub of(String user) {
        return new ClearinghouseStateSub(user);
    }

    @Override
    public String getType() {
        return type;
    }
}
