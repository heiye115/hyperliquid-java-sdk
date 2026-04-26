package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserLedgerUpdateSub extends Subscription {

    @JsonProperty("type")
    private final String type = "userNonFundingLedgerUpdates";

    @JsonProperty("user")
    private String user;

    public UserLedgerUpdateSub(String user) {
        this.user = user;
    }

    public static UserLedgerUpdateSub of(String user) {
        return new UserLedgerUpdateSub(user);
    }

    @Override
    public String getType() {
        return type;
    }
}
