package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ActiveAssetCtxSub extends Subscription {

    @JsonProperty("type")
    private final String type = "activeAssetCtx";

    @JsonProperty("coin")
    private String coin;

    public ActiveAssetCtxSub(String coin) {
        this.coin = coin;
    }

    public static ActiveAssetCtxSub of(String coin) {
        return new ActiveAssetCtxSub(coin);
    }

    @Override
    public String getType() {
        return type;
    }
}
