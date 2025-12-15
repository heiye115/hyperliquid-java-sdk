package io.github.hyperliquid.sdk.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.model.subscription.Subscription;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket Manager.
 * Manages connections, subscriptions, heartbeats, and message distribution.
 */
public class WebsocketManager {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(WebsocketManager.class.getName());

    /**
     * Original API root URL (http/https), used for network availability detection
     */
    private final String baseUrl;
    /**
     * WebSocket connection URL (derived from baseUrl)
     */
    private final String wsUrl;
    /**
     * Network detection URL (optional, overrides baseUrl for detection if set)
     */
    private String probeUrl;
    /**
     * Whether to disable network detection (considered always available if disabled)
     */
    private boolean probeDisabled = false;
    /**
     * WebSocket main client (used to establish and manage WS connections)
     */
    private final OkHttpClient client;
    /**
     * Current WebSocket connection instance
     */
    private WebSocket webSocket;
    /**
     * Whether the manager has been stopped (no reconnection after stop)
     */
    private volatile boolean stopped = false;
    /**
     * Whether the current connection is established
     */
    private volatile boolean connected = false;

    /**
     * Number of reconnection attempts made
     */
    private int reconnectAttempts = 0;
    /**
     * Current reconnection delay in milliseconds (exponential backoff), initial 1s (configurable)
     */
    private long backoffMs = 1_000L;
    /**
     * Initial reconnection delay in milliseconds (reset to this value after successful connection)
     */
    private long initialBackoffMs = backoffMs;
    /**
     * Internal maximum backoff limit in milliseconds (fixed at 30s)
     */
    private final long maxBackoffMs = 30_000L;
    /**
     * Externally configured maximum backoff limit in milliseconds (does not exceed internal limit)
     */
    private long configMaxBackoffMs = maxBackoffMs;
    /**
     * Reference to scheduled reconnection task
     */
    private volatile ScheduledFuture<?> reconnectFuture;


    /**
     * Current network detection status (true means available)
     */
    private volatile boolean networkAvailable = true;
    /**
     * Network status check interval in seconds (default 5 seconds)
     */
    private int networkCheckIntervalSeconds = 5;
    /**
     * Network monitoring task reference (periodic detection when disconnected)
     */
    private volatile ScheduledFuture<?> networkMonitorFuture;
    /**
     * Lightweight HTTP client for network detection (short timeout)
     */
    private final OkHttpClient networkClient;

    /**
     * Active subscription collection, stored and deduplicated by identifier
     */
    private final Map<String, List<ActiveSubscription>> subscriptions = new ConcurrentHashMap<>();
    /**
     * Identifier cache (optimizes string concatenation performance)
     */
    private final Map<String, String> identifierCache = new ConcurrentHashMap<>();
    /**
     * Scheduled task scheduler (used for heartbeats, reconnection, and network monitoring)
     */
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

    /**
     * Connection status listener: used to notify connection, disconnection, reconnection, and network status changes.
     */
    public interface ConnectionListener {
        /**
         * Connection is being established (includes reconnection process)
         */
        void onConnecting(String url);

        /**
         * Connection established
         */
        void onConnected(String url);

        /**
         * Connection disconnected (code/reason/cause may be null)
         */
        void onDisconnected(String url, int code, String reason, Throwable cause);

        /**
         * Entering reconnection: attempt is the attempt number (starting from 1), nextDelayMs is the delay in milliseconds for the next attempt
         */
        void onReconnecting(String url, int attempt, long nextDelayMs);

        /**
         * Reconnection failed: exceeded maximum attempts
         */
        void onReconnectFailed(String url, int attempted, Throwable lastError);

        /**
         * Network unavailable
         */
        void onNetworkUnavailable(String url);

        /**
         * Network recovered
         */
        void onNetworkAvailable(String url);
    }

    /**
     * Connection listener collection (thread-safe)
     */
    private final List<ConnectionListener> connectionListeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Callback interface
     */
    public interface MessageCallback {
        void onMessage(JsonNode msg);
    }

    /**
     * Callback exception listener interface.
     * When user callbacks throw exceptions, the framework captures and notifies this listener.
     */
    public interface CallbackErrorListener {
        /**
         * Triggered when user callback throws an exception.
         *
         * @param url        Current WebSocket URL
         * @param identifier Subscription identifier (generated by subscriptionToIdentifier/wsMsgToIdentifier)
         * @param message    Message that caused the exception (raw JSON)
         * @param error      Exception object
         */
        void onCallbackError(String url, String identifier, JsonNode message, Throwable error);
    }

    /**
     * Active subscription record
     */
    public static class ActiveSubscription {
        public final JsonNode subscription;
        public final MessageCallback callback;

        public ActiveSubscription(JsonNode s, MessageCallback c) {
            this.subscription = s;
            this.callback = c;
        }

        public JsonNode getSubscription() {
            return subscription;
        }
    }

    /**
     * Callback exception listener collection (thread-safe)
     */
    private final List<CallbackErrorListener> callbackErrorListeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Construct WebSocket manager.
     *
     * @param baseUrl API root URL (http/https), automatically converted to ws/wss
     */
    public WebsocketManager(String baseUrl) {
        this.baseUrl = baseUrl;
        String scheme = baseUrl.startsWith("https") ? "wss" : "ws";
        String tail = baseUrl.replaceFirst("https?", "");
        this.wsUrl = scheme + tail + "/ws";
        this.probeUrl = null;
        this.client = new OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(0)) // WebSocket does not set readTimeout
                .build();
        // Lightweight client for network connectivity check (short timeout)
        this.networkClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .callTimeout(Duration.ofSeconds(5))
                .build();
        connect();
        startPing();
    }

    /**
     * Establish (or re-establish) WebSocket connection.
     * Will automatically resend all subscriptions in onOpen.
     */
    private void connect() {
        notifyConnecting();
        Request request = new Request.Builder().url(wsUrl).build();
        this.webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                connected = true;
                reconnectAttempts = 0;
                backoffMs = initialBackoffMs;
                stopNetworkMonitor();
                notifyConnected();
                // Re-subscribe
                for (List<ActiveSubscription> list : subscriptions.values()) {
                    for (ActiveSubscription sub : list) {
                        sendSubscribe(sub.subscription);
                    }
                }
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                try {
                    JsonNode msg = JSONUtil.readTree(text);
                    String identifier = wsMsgToIdentifier(msg);
                    if (identifier != null && subscriptions.containsKey(identifier)) {
                        for (ActiveSubscription sub : subscriptions.get(identifier)) {
                            try {
                                sub.callback.onMessage(msg);
                            } catch (Exception cbEx) {
                                // Log and trigger exception listener, does not affect other subscription callbacks
                                LOG.log(Level.WARNING, "WebSocket callback exception, identifier=" + identifier, cbEx);
                                notifyCallbackError(identifier, msg, cbEx);
                            }
                        }
                    }
                } catch (IOException e) {
                    // ignore parsing error
                }
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                onMessage(webSocket, bytes.utf8());
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
                connected = false;
                notifyDisconnected(-1, String.valueOf(t), t);
                if (!stopped) {
                    scheduleReconnect(t, null, null);
                }
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                connected = false;
                notifyDisconnected(code, reason, null);
                if (!stopped) {
                    scheduleReconnect(null, code, reason);
                }
            }
        });
    }

    private void startPing() {
        scheduler.scheduleAtFixedRate(this::sendPing, 20, 20, TimeUnit.SECONDS);
    }

    /**
     * Send ping message (internal method, automatically called by timer)
     */
    private void sendPing() {
        if (webSocket != null && connected) {
            Map<String, Object> payload = Map.of("method", "ping");
            try {
                webSocket.send(JSONUtil.writeValueAsString(payload));
            } catch (Exception e) {
                LOG.log(Level.FINE, "Failed to send ping message", e);
            }
        }
    }

    /**
     * Stop and close connection
     */
    public void stop() {
        stopped = true;

        // Cancel all scheduled tasks first
        cancelTask(reconnectFuture);
        cancelTask(networkMonitorFuture);

        // Close WebSocket connection
        if (webSocket != null) {
            try {
                webSocket.close(1000, "stop");
            } catch (Exception e) {
                LOG.log(Level.FINE, "Error while closing WebSocket on stop", e);
            }
            webSocket = null;
        }

        // Gracefully shut down scheduler
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close OkHttpClient resources (release connection pool and thread pool)
        try {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error while shutting down WebSocket client", e);
        }

        try {
            networkClient.dispatcher().executorService().shutdown();
            networkClient.connectionPool().evictAll();
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error while shutting down network client", e);
        }
    }

    /**
     * Schedule a reconnection attempt (with exponential backoff, unlimited retries until success).
     * Initial 1s, maximum 30s, unlimited retries; also starts network monitoring, triggers immediate reconnection when network recovers.
     */
    private synchronized void scheduleReconnect(Throwable cause, Integer code, String reason) {
        if (stopped)
            return;
        // Conservatively close old connection resources
        if (webSocket != null) {
            try {
                webSocket.close(1001, "reconnect");
            } catch (Exception e) {
                LOG.log(Level.FINE, "Error while closing WebSocket for reconnect", e);
            }
            webSocket = null;
        }

        long nextDelay = backoffMs + (long) (Math.random() * 250L); // Small jitter
        notifyReconnecting(reconnectAttempts + 1, nextDelay);

        cancelTask(reconnectFuture);
        reconnectFuture = scheduler.schedule(() -> {
            if (!stopped) {
                connect();
            }
        }, nextDelay, TimeUnit.MILLISECONDS);

        reconnectAttempts++;
        // Backoff growth is constrained by two limits: internal limit and external configuration limit
        backoffMs = Math.min(Math.min(maxBackoffMs, configMaxBackoffMs), backoffMs * 2);

        startNetworkMonitor();
    }

    /**
     * Start network status monitoring (only runs when disconnected)
     */
    private synchronized void startNetworkMonitor() {
        if (networkMonitorFuture != null && !networkMonitorFuture.isCancelled())
            return;
        networkMonitorFuture = scheduler.scheduleWithFixedDelay(() -> {
            boolean ok = isNetworkAvailable();
            if (ok) {
                if (!networkAvailable) {
                    networkAvailable = true;
                    notifyNetworkAvailable();
                }
                // Network available and currently not connected: attempt quick reconnection (reset backoff and count)
                if (!connected && !stopped) {
                    backoffMs = initialBackoffMs;
                    // Reset counter after network recovery, allowing reconnection attempts to restart
                    reconnectAttempts = 0;
                    cancelTask(reconnectFuture);
                    notifyReconnecting(1, 0);
                    reconnectFuture = scheduler.schedule(this::connect, 0, TimeUnit.MILLISECONDS);
                }
            } else {
                if (networkAvailable) {
                    networkAvailable = false;
                    notifyNetworkUnavailable();
                }
            }
        }, 0, networkCheckIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Stop network status monitoring
     */
    private synchronized void stopNetworkMonitor() {
        if (networkMonitorFuture != null) {
            networkMonitorFuture.cancel(false);
            networkMonitorFuture = null;
        }
        networkAvailable = true; // Consider available by default when stopping monitoring
    }

    /**
     * Enhanced network availability detection: HEAD request to baseUrl, allows 2xx/3xx, supports retry
     */
    private boolean isNetworkAvailable() {
        if (probeDisabled) {
            return true;
        }
        String url = probeUrl != null ? probeUrl : baseUrl;
        int maxRetries = 2;
        long retryDelayMs = 100;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                Request req = new Request.Builder().url(url).head().build();
                try (Response resp = networkClient.newCall(req).execute()) {
                    if (resp.code() < 400) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // Only return false on the last attempt failure
                if (attempt == maxRetries - 1) {
                    LOG.log(Level.FINE, "Network detection failed, retried " + maxRetries + " times", e);
                    return false;
                }
                // Not the last attempt, wait and retry
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    public void setNetworkProbeUrl(String url) {
        this.probeUrl = url;
    }

    public void setNetworkProbeDisabled(boolean disabled) {
        this.probeDisabled = disabled;
    }

    /**
     * Add connection status listener
     */
    public void addConnectionListener(ConnectionListener l) {
        if (l != null)
            connectionListeners.add(l);
    }

    /**
     * Remove connection status listener
     */
    public void removeConnectionListener(ConnectionListener l) {
        if (l != null)
            connectionListeners.remove(l);
    }

    /**
     * Add callback exception listener
     */
    public void addCallbackErrorListener(CallbackErrorListener l) {
        if (l != null)
            callbackErrorListeners.add(l);
    }

    /**
     * Remove callback exception listener
     */
    public void removeCallbackErrorListener(CallbackErrorListener l) {
        if (l != null)
            callbackErrorListeners.remove(l);
    }

    /**
     * Set network monitoring check interval in seconds (default 5)
     */
    public void setNetworkCheckIntervalSeconds(int seconds) {
        this.networkCheckIntervalSeconds = Math.max(1, seconds);
    }

    /**
     * Set reconnection exponential backoff parameters.
     *
     * @param initialMs Initial reconnection delay in milliseconds (recommended 500ms~2000ms)
     * @param maxMs     Maximum reconnection delay in milliseconds (recommended not to exceed 30000ms)
     */
    public void setReconnectBackoffMs(long initialMs, long maxMs) {
        long init = Math.max(100, initialMs);
        long max = Math.max(init, maxMs);
        this.initialBackoffMs = init;
        this.backoffMs = init;
        this.configMaxBackoffMs = Math.min(maxBackoffMs, max);
    }

    /**
     * Safely cancel scheduled task
     */
    private void cancelTask(ScheduledFuture<?> future) {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }

    /**
     * Generic listener notification method (defensive, single listener exception does not affect others)
     */
    private <T> void notifyListeners(List<T> listeners, java.util.function.Consumer<T> action) {
        synchronized (listeners) {
            for (T listener : listeners) {
                try {
                    action.accept(listener);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Listener callback threw exception: " + listener, e);
                }
            }
        }
    }

    // Listener notification wrapper (use generic method to reduce duplicate code)
    private void notifyConnecting() {
        notifyListeners(connectionListeners, l -> l.onConnecting(wsUrl));
    }

    private void notifyConnected() {
        notifyListeners(connectionListeners, l -> l.onConnected(wsUrl));
    }

    private void notifyDisconnected(int code, String reason, Throwable cause) {
        notifyListeners(connectionListeners, l -> l.onDisconnected(wsUrl, code, reason, cause));
    }

    private void notifyReconnecting(int attempt, long nextDelayMs) {
        notifyListeners(connectionListeners, l -> l.onReconnecting(wsUrl, attempt, nextDelayMs));
    }

    private void notifyReconnectFailed(int attempted, Throwable lastError) {
        notifyListeners(connectionListeners, l -> l.onReconnectFailed(wsUrl, attempted, lastError));
    }

    private void notifyNetworkUnavailable() {
        notifyListeners(connectionListeners, l -> l.onNetworkUnavailable(wsUrl));
    }

    private void notifyNetworkAvailable() {
        notifyListeners(connectionListeners, l -> l.onNetworkAvailable(wsUrl));
    }

    /**
     * Notify: user callback exception
     */
    private void notifyCallbackError(String identifier, JsonNode msg, Throwable error) {
        notifyListeners(callbackErrorListeners, l -> l.onCallbackError(wsUrl, identifier, msg, error));
    }

    /**
     * Subscribe to messages (type-safe version, using Subscription entity class).
     *
     * @param subscription Subscription object (Subscription entity class)
     * @param callback     Callback
     */
    public void subscribe(Subscription subscription, MessageCallback callback) {
        if (stopped) {
            throw new IllegalStateException("WebsocketManager has been stopped, cannot subscribe");
        }
        if (subscription == null) {
            throw new IllegalArgumentException("subscription cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }

        // Convert Subscription object to JsonNode
        JsonNode jsonNode = JSONUtil.convertValue(subscription, JsonNode.class);
        subscribe(jsonNode, callback);
    }

    /**
     * Subscribe to messages (compatible version, using JsonNode).
     *
     * @param subscription Subscription object
     * @param callback     Callback
     */
    public void subscribe(JsonNode subscription, MessageCallback callback) {
        if (stopped) {
            throw new IllegalStateException("WebsocketManager has been stopped, cannot subscribe");
        }
        if (subscription == null) {
            throw new IllegalArgumentException("subscription cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }

        String identifier = subscriptionToIdentifier(subscription);
        List<ActiveSubscription> list = subscriptions.computeIfAbsent(identifier, k -> new CopyOnWriteArrayList<>());
        for (ActiveSubscription s : list) {
            if (s.subscription.equals(subscription)) {
                return;
            }
        }
        list.add(new ActiveSubscription(subscription, callback));
        sendSubscribe(subscription);
    }

    private void sendSubscribe(JsonNode subscription) {
        if (webSocket == null || !connected) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", "subscribe");
        payload.put("subscription", subscription);
        try {
            webSocket.send(JSONUtil.writeValueAsString(payload));
        } catch (Exception e) {
            LOG.log(Level.FINE, "Failed to send subscribe message: " + subscription, e);
        }
    }

    /**
     * Unsubscribe (type-safe version, using Subscription entity class).
     *
     * @param subscription Subscription object (Subscription entity class)
     */
    public void unsubscribe(Subscription subscription) {
        if (subscription == null) {
            throw new IllegalArgumentException("subscription cannot be null");
        }

        // Convert Subscription object to JsonNode
        JsonNode jsonNode = JSONUtil.convertValue(subscription, JsonNode.class);
        unsubscribe(jsonNode);
    }

    /**
     * Unsubscribe (compatible version, using JsonNode).
     *
     * @param subscription Subscription object
     */
    public void unsubscribe(JsonNode subscription) {
        if (subscription == null) {
            throw new IllegalArgumentException("subscription cannot be null");
        }

        String identifier = subscriptionToIdentifier(subscription);
        List<ActiveSubscription> list = subscriptions.get(identifier);
        if (list != null) {
            list.removeIf(s -> s.subscription.equals(subscription));
            if (list.isEmpty())
                subscriptions.remove(identifier);
        }

        if (webSocket == null || !connected) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", "unsubscribe");
        payload.put("subscription", subscription);
        try {
            webSocket.send(JSONUtil.writeValueAsString(payload));
        } catch (Exception e) {
            LOG.log(Level.FINE, "Failed to send unsubscribe message: " + subscription, e);
        }
    }

    /**
     * Convert subscription object to identifier (used for subscription deduplication and routing).
     * <p>
     * This method is package-visible, mainly for internal use and unit testing.
     * Channel identifier conventions:
     * - l2Book:{coin}, trades:{coin}, bbo:{coin}, candle:{coin},{interval}
     * - userEvents, orderUpdates, allMids
     * -
     * userFills:{user}, userFundings:{user}, userNonFundingLedgerUpdates:{user}, webData2:{user}
     * - activeAssetCtx:{coin}, activeAssetData:{coin},{user}
     * - coin can be string or integer; strings are uniformly converted to lowercase.
     * </p>
     */
    String subscriptionToIdentifier(JsonNode subscription) {
        if (subscription == null || !subscription.has("type"))
            return "unknown";
        String type = subscription.get("type").asText();
        switch (type) {
            case "allMids":
            case "userEvents":
            case "orderUpdates":
                return type;
            case "l2Book": {
                JsonNode coinNode = subscription.get("coin");
                String coinKey = extractCoinIdentifier(coinNode);
                return buildCachedIdentifier(type, coinKey);
            }
            case "trades":
            case "bbo":
            case "activeAssetCtx": {
                JsonNode coinNode = subscription.get("coin");
                String coinKey = extractCoinIdentifier(coinNode);
                return buildCachedIdentifier(type, coinKey);
            }
            case "candle": {
                JsonNode coinNode = subscription.get("coin");
                JsonNode iNode = subscription.get("interval");
                String coinKey = extractCoinIdentifier(coinNode);
                String interval = iNode == null ? null : iNode.asText();
                if (coinKey != null && interval != null)
                    return buildCachedIdentifier(type, coinKey + "," + interval);
                return type;
            }
            case "userFills":
            case "userFundings":
            case "userNonFundingLedgerUpdates":
            case "webData2": {
                JsonNode userNode = subscription.get("user");
                String user = userNode == null ? null : userNode.asText().toLowerCase(Locale.ROOT);
                return buildCachedIdentifier(type, user);
            }
            case "activeAssetData": {
                JsonNode coinNode = subscription.get("coin");
                JsonNode userNode = subscription.get("user");
                String coinKey = extractCoinIdentifier(coinNode);
                String user = userNode == null ? null : userNode.asText().toLowerCase(Locale.ROOT);
                if (coinKey != null && user != null)
                    return buildCachedIdentifier(type, coinKey + "," + user);
                return type;
            }
            default:
                return type;
        }
    }

    /**
     * Extract Coin identifier (encapsulates duplicate logic)
     */
    private String extractCoinIdentifier(JsonNode coinNode) {
        if (coinNode == null) return null;
        return coinNode.isNumber()
                ? String.valueOf(coinNode.asInt())
                : coinNode.asText().toLowerCase(Locale.ROOT);
    }

    /**
     * Build cached identifier (optimizes string concatenation performance)
     */
    private String buildCachedIdentifier(String type, String suffix) {
        if (suffix == null) return type;

        String cacheKey = type + "|" + suffix;
        return identifierCache.computeIfAbsent(cacheKey, k -> type + ":" + suffix);
    }

    /**
     * Extract identifier from message, corresponding to subscriptionToIdentifier.
     * <p>
     * This method is package-visible, mainly for internal use and unit testing.
     * Compatible with two message formats: channel as string or object ({type: ..., ...}).
     * </p>
     */
    String wsMsgToIdentifier(JsonNode msg) {
        if (msg == null || !msg.has("channel"))
            return null;
        JsonNode channelNode = msg.get("channel");
        String type = null;
        if (channelNode.isTextual()) {
            type = channelNode.asText();
        } else if (channelNode.isObject() && channelNode.has("type")) {
            type = channelNode.get("type").asText();
        }
        if (type == null)
            return null;
        switch (type) {
            case "pong":
            case "allMids":
            case "userEvents":
            case "orderUpdates":
                return type;
            case "l2Book": {
                JsonNode coinNode = msg.path("data").path("coin");
                String coinKey = extractCoinIdentifier(coinNode);
                return buildCachedIdentifier(type, coinKey);
            }
            case "trades": {
                JsonNode trades = msg.get("data");
                String coinKey = null;
                if (trades != null && trades.isArray() && !trades.isEmpty()) {
                    JsonNode first = trades.get(0);
                    JsonNode coinNode = first.get("coin");
                    if (coinNode != null) {
                        coinKey = extractCoinIdentifier(coinNode);
                    }
                }
                return buildCachedIdentifier(type, coinKey);
            }
            case "candle": {
                JsonNode data = msg.get("data");
                if (data != null) {
                    String s = data.path("s").asText(null);
                    String i = data.path("i").asText(null);
                    if (s != null && i != null)
                        return buildCachedIdentifier(type, s.toLowerCase(Locale.ROOT) + "," + i);
                }
                return type;
            }
            case "bbo": {
                JsonNode coinNode = msg.path("data").path("coin");
                String coinKey = extractCoinIdentifier(coinNode);
                return buildCachedIdentifier(type, coinKey);
            }
            case "userFills":
            case "userFundings":
            case "userNonFundingLedgerUpdates":
            case "webData2": {
                JsonNode userNode = msg.path("data").path("user");
                String user = (userNode != null && userNode.isTextual()) ? userNode.asText().toLowerCase(Locale.ROOT)
                        : null;
                return buildCachedIdentifier(type, user);
            }
            case "activeAssetCtx":
            case "activeSpotAssetCtx": {
                JsonNode coinNode = msg.path("data").path("coin");
                String coinKey = extractCoinIdentifier(coinNode);
                return type.equals("activeSpotAssetCtx")
                        ? buildCachedIdentifier("activeAssetCtx", coinKey != null ? coinKey : "unknown")
                        : buildCachedIdentifier(type, coinKey);
            }
            case "activeAssetData": {
                JsonNode data = msg.get("data");
                if (data != null) {
                    JsonNode coinNode = data.get("coin");
                    JsonNode userNode = data.get("user");
                    String coinKey = extractCoinIdentifier(coinNode);
                    String user = (userNode != null && userNode.isTextual())
                            ? userNode.asText().toLowerCase(Locale.ROOT)
                            : null;
                    if (coinKey != null && user != null)
                        return buildCachedIdentifier(type, coinKey + "," + user);
                }
                return type;
            }
            default:
                return type;
        }
    }

    /**
     * Get a copy of all current subscriptions.
     *
     * @return A map of subscription identifiers to lists of active subscriptions
     */
    public Map<String, List<ActiveSubscription>> getSubscriptions() {
        Map<String, List<ActiveSubscription>> copy = new HashMap<>();
        for (Map.Entry<String, List<ActiveSubscription>> entry : subscriptions.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Get subscriptions by identifier.
     *
     * @param identifier The subscription identifier
     * @return A list of active subscriptions for the given identifier, or empty list if none
     */
    public List<ActiveSubscription> getSubscriptionsByIdentifier(String identifier) {
        List<ActiveSubscription> list = subscriptions.get(identifier);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    /**
     * Check if there are any active subscriptions.
     *
     * @return true if there are active subscriptions, false otherwise
     */
    public boolean hasSubscriptions() {
        return !subscriptions.isEmpty();
    }

    /**
     * Get the count of active subscription identifiers.
     *
     * @return The number of unique subscription identifiers
     */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }
}
