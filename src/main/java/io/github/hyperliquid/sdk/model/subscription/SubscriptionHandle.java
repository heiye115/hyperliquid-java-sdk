package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Opaque handle returned by {@link io.github.hyperliquid.sdk.websocket.WebsocketManager#subscribeWithHandle}
 * to remove one callback without affecting other subscribers to the same channel.
 */
public class SubscriptionHandle {

    private final long subscriptionId;

    private final JsonNode subscription;

    public SubscriptionHandle(long subscriptionId, JsonNode subscription) {
        this.subscriptionId = subscriptionId;
        this.subscription = subscription;
    }

    public long getSubscriptionId() {
        return subscriptionId;
    }

    public JsonNode getSubscription() {
        return subscription;
    }
}
