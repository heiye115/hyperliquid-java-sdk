package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserFundingsSub extends Subscription {

    @JsonProperty("type")
    private final String type = "userFundings";

    @JsonProperty("user")
    private String user;

    public UserFundingsSub(String user) {
        this.user = user;
    }

    public static UserFundingsSub of(String user) {
        return new UserFundingsSub(user);
    }

    @Override
    public String getType() {
        return type;
    }
}
