package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ActiveAssetDataSub extends Subscription {

    @JsonProperty("type")
    private final String type = "activeAssetData";

    @JsonProperty("coin")
    private String coin;

    @JsonProperty("user")
    private String user;

    public ActiveAssetDataSub(String coin, String user) {
        this.coin = coin;
        this.user = user;
    }

    public static ActiveAssetDataSub of(String coin, String user) {
        return new ActiveAssetDataSub(coin, user);
    }

    @Override
    public String getType() {
        return type;
    }
}
