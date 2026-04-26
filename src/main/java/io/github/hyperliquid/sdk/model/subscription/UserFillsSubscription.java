package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserFillsSubscription extends Subscription {

    @JsonProperty("type")
    private final String type = "userFills";

    @JsonProperty("user")
    private String user;

    @JsonProperty("aggregateByTime")
    private Boolean aggregateByTime;

    public UserFillsSubscription(String user) {
        this.user = user;
    }

    public UserFillsSubscription(String user, Boolean aggregateByTime) {
        this.user = user;
        this.aggregateByTime = aggregateByTime;
    }

    public static UserFillsSubscription of(String user) {
        return new UserFillsSubscription(user);
    }

    public static UserFillsSubscription of(String user, Boolean aggregateByTime) {
        return new UserFillsSubscription(user, aggregateByTime);
    }

    @Override
    public String getType() {
        return type;
    }
}
