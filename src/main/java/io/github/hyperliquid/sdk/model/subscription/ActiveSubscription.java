package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;

/**
 * One registered subscription: the exact JSON sent to the server, the callback, and a local id for
 * targeted unsubscription via {@link io.github.hyperliquid.sdk.websocket.WebsocketManager#unsubscribe(long)}.
 */
public class ActiveSubscription {

    /**
     * Subscription body as sent in {@code subscribe}.
     */
    public final JsonNode subscription;
    /**
     * User callback for matching channel messages.
     */
    public final io.github.hyperliquid.sdk.websocket.WebsocketManager.MessageCallback callback;
    /**
     * JVM-unique monotonic id assigned by the global {@code GLOBAL_SUB_ID} generator.
     * Used for targeted unsubscribe via {@link io.github.hyperliquid.sdk.websocket.WebsocketManager#unsubscribe(long)}.
     */
    public final long subscriptionId;

    /**
     * Legacy constructor: assigns {@link #subscriptionId} {@code 0} (not targetable by
     * {@link io.github.hyperliquid.sdk.websocket.WebsocketManager#unsubscribe(long)}).
     */
    public ActiveSubscription(JsonNode s, WebsocketManager.MessageCallback c) {
        this(s, c, 0L);
    }

    public ActiveSubscription(JsonNode s, WebsocketManager.MessageCallback c, long subscriptionId) {
        this.subscription = s;
        this.callback = c;
        this.subscriptionId = subscriptionId;
    }

    public JsonNode getSubscription() {
        return subscription;
    }

    public long getSubscriptionId() {
        return subscriptionId;
    }
}
