package io.github.hyperliquid.sdk.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import lombok.Getter;
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
 * WebSocket 管理器。
 * 管理连接、订阅、心跳与消息分发。
 */
@Getter
public class WebsocketManager {

    /**
     * 日志记录器
     */
    private static final Logger LOG = Logger.getLogger(WebsocketManager.class.getName());

    /**
     * 原始 API 根地址（http/https），用于网络可用性检测
     */
    private final String baseUrl;
    private final String wsUrl;
    private String probeUrl;
    private boolean probeDisabled = false;
    private final OkHttpClient client;
    private WebSocket webSocket;
    private volatile boolean stopped = false;
    private volatile boolean connected = false;
    private volatile boolean reconnecting = false;

    // 重连控制参数
    private int reconnectAttempts = 0;
    private int maxReconnectAttempts = 5; // 可配置，默认 5 次
    private long backoffMs = 1_000L; // 初始 1s（可配置）
    private long initialBackoffMs = backoffMs; // 记录初始值，便于连接成功后重置
    private final long maxBackoffMs = 30_000L; // 内部最大 30s 上限
    private long configMaxBackoffMs = maxBackoffMs; // 可配置最大上限，默认与内部一致
    private volatile ScheduledFuture<?> reconnectFuture;

    // 网络状态监控
    private volatile boolean networkAvailable = true;
    private int networkCheckIntervalSeconds = 5;
    private volatile ScheduledFuture<?> networkMonitorFuture;
    private final OkHttpClient networkClient;

    private final Map<String, List<ActiveSubscription>> subscriptions = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

    /**
     * 连接状态监听器：用于通知连接、断开、重连与网络状态变化。
     */
    public interface ConnectionListener {
        /**
         * 正在建立连接（包含重连过程）
         */
        void onConnecting(String url);

        /**
         * 连接已建立
         */
        void onConnected(String url);

        /**
         * 连接断开（code/reason/cause 可能为空）
         */
        void onDisconnected(String url, int code, String reason, Throwable cause);

        /**
         * 进入重连：attempt 为本次尝试序号（从 1 开始），nextDelayMs 为下一次尝试的延迟毫秒数
         */
        void onReconnecting(String url, int attempt, long nextDelayMs);

        /**
         * 重连失败：超过最大次数
         */
        void onReconnectFailed(String url, int attempted, Throwable lastError);

        /**
         * 网络不可用
         */
        void onNetworkUnavailable(String url);

        /**
         * 网络恢复可用
         */
        void onNetworkAvailable(String url);
    }

    /**
     * 连接监听器集合（线程安全）
     */
    private final List<ConnectionListener> connectionListeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * 回调接口
     */
    public interface MessageCallback {
        void onMessage(JsonNode msg);
    }

    /**
     * 回调异常监听接口。
     * 当用户回调抛出异常时，框架会捕获并通知此监听器。
     */
    public interface CallbackErrorListener {
        /**
         * 用户回调抛出异常时触发。
         *
         * @param url        当前 WebSocket URL
         * @param identifier 订阅标识符（由 subscriptionToIdentifier/wsMsgToIdentifier 生成）
         * @param message    导致异常的消息（原始 JSON）
         * @param error      异常对象
         */
        void onCallbackError(String url, String identifier, JsonNode message, Throwable error);
    }

    /**
     * 活跃订阅记录
     */
    public static class ActiveSubscription {
        public final JsonNode subscription;
        public final MessageCallback callback;

        public ActiveSubscription(JsonNode s, MessageCallback c) {
            this.subscription = s;
            this.callback = c;
        }
    }

    /**
     * 回调异常监听器集合（线程安全）
     */
    private final List<CallbackErrorListener> callbackErrorListeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * 构造 WebSocket 管理器。
     *
     * @param baseUrl API 根地址（http/https），会自动转换为 ws/wss
     */
    public WebsocketManager(String baseUrl) {
        this.baseUrl = baseUrl;
        String scheme = baseUrl.startsWith("https") ? "wss" : "ws";
        String tail = baseUrl.replaceFirst("https?", "");
        this.wsUrl = scheme + tail + "/ws";
        this.probeUrl = null;
        this.client = new OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(0)) // WebSocket 不设 readTimeout
                .build();
        // 用于网络连通性检查的轻量客户端（短超时）
        this.networkClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .callTimeout(Duration.ofSeconds(5))
                .build();
        connect();
        startPing();
    }

    /**
     * 建立（或重建）WebSocket 连接。
     * 会在 onOpen 中自动重新发送所有订阅。
     */
    private void connect() {
        notifyConnecting();
        Request request = new Request.Builder().url(wsUrl).build();
        this.webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                connected = true;
                reconnecting = false;
                reconnectAttempts = 0;
                backoffMs = initialBackoffMs;
                stopNetworkMonitor();
                notifyConnected();
                // 重新订阅
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
                                // 记录日志并触发异常监听器，不影响其他订阅回调
                                LOG.log(Level.WARNING, "WebSocket 回调异常，identifier=" + identifier, cbEx);
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
     * 发送 ping 消息
     */
    public void sendPing() {
        if (webSocket != null && connected) {
            Map<String, Object> payload = Map.of("method", "ping");
            try {
                webSocket.send(JSONUtil.writeValueAsString(payload));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 停止并关闭连接
     */
    public void stop() {
        stopped = true;
        scheduler.shutdownNow();
        if (reconnectFuture != null)
            reconnectFuture.cancel(false);
        if (networkMonitorFuture != null)
            networkMonitorFuture.cancel(false);
        if (webSocket != null) {
            webSocket.close(1000, "stop");
        }
    }

    /**
     * 安排一次重连尝试（带指数退避与最大次数限制）。
     * 初始 1s，最大 30s，默认最多 5 次；同时启动网络监控，当网络恢复时将立即触发一次重连。
     */
    private synchronized void scheduleReconnect(Throwable cause, Integer code, String reason) {
        if (stopped)
            return;
        // 保守地关闭旧连接资源
        if (webSocket != null) {
            try {
                webSocket.close(1001, "reconnect");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }

        // 达到最大次数：停止重连，等待网络监控触发或外部重新调用 connect
        if (reconnectAttempts >= maxReconnectAttempts) {
            notifyReconnectFailed(reconnectAttempts, cause);
            startNetworkMonitor();
            return;
        }

        long nextDelay = backoffMs + (long) (Math.random() * 250L); // 少量抖动
        notifyReconnecting(reconnectAttempts + 1, nextDelay);

        if (reconnectFuture != null)
            reconnectFuture.cancel(false);
        reconnectFuture = scheduler.schedule(() -> {
            if (!stopped) {
                reconnecting = true;
                connect();
            }
        }, nextDelay, TimeUnit.MILLISECONDS);

        reconnectAttempts++;
        // 退避增长受两层上限约束：内部上限与外部配置上限
        backoffMs = Math.min(Math.min(maxBackoffMs, configMaxBackoffMs), backoffMs * 2);

        startNetworkMonitor();
    }

    /**
     * 启动网络状态监控（仅在断线时运行）
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
                // 网络可用且当前未连接：尝试快速重连（重置退避与次数）
                if (!connected && !stopped) {
                    backoffMs = initialBackoffMs;
                    reconnectAttempts = Math.min(reconnectAttempts, maxReconnectAttempts); // 保持统计
                    if (reconnectFuture != null)
                        reconnectFuture.cancel(false);
                    notifyReconnecting(reconnectAttempts + 1, 0);
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
     * 停止网络状态监控
     */
    private synchronized void stopNetworkMonitor() {
        if (networkMonitorFuture != null) {
            networkMonitorFuture.cancel(false);
            networkMonitorFuture = null;
        }
        networkAvailable = true; // 停止监控时默认视为可用
    }

    /**
     * 简单的网络可用性探测：HEAD 请求 baseUrl，允许 2xx/3xx
     */
    private boolean isNetworkAvailable() {
        if (probeDisabled) {
            return true;
        }
        String url = probeUrl != null ? probeUrl : baseUrl;
        try {
            Request req = new Request.Builder().url(url).head().build();
            try (Response resp = networkClient.newCall(req).execute()) {
                return resp.code() < 400;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void setNetworkProbeUrl(String url) {
        this.probeUrl = url;
    }

    public void setNetworkProbeDisabled(boolean disabled) {
        this.probeDisabled = disabled;
    }

    /**
     * 添加连接状态监听器
     */
    public void addConnectionListener(ConnectionListener l) {
        if (l != null)
            connectionListeners.add(l);
    }

    /**
     * 移除连接状态监听器
     */
    public void removeConnectionListener(ConnectionListener l) {
        if (l != null)
            connectionListeners.remove(l);
    }

    /**
     * 添加回调异常监听器
     */
    public void addCallbackErrorListener(CallbackErrorListener l) {
        if (l != null)
            callbackErrorListeners.add(l);
    }

    /**
     * 移除回调异常监听器
     */
    public void removeCallbackErrorListener(CallbackErrorListener l) {
        if (l != null)
            callbackErrorListeners.remove(l);
    }

    /**
     * 设置最大重连次数（默认 5）
     */
    public void setMaxReconnectAttempts(int max) {
        this.maxReconnectAttempts = Math.max(0, max);
    }

    /**
     * 设置网络监控检查间隔秒数（默认 5）
     */
    public void setNetworkCheckIntervalSeconds(int seconds) {
        this.networkCheckIntervalSeconds = Math.max(1, seconds);
    }

    /**
     * 设置重连指数退避参数。
     *
     * @param initialMs 初始重连延迟毫秒（建议 500ms~2000ms）
     * @param maxMs     最大重连延迟毫秒（建议不超过 30000ms）
     */
    public void setReconnectBackoffMs(long initialMs, long maxMs) {
        long init = Math.max(100, initialMs);
        long max = Math.max(init, maxMs);
        this.initialBackoffMs = init;
        this.backoffMs = init;
        this.configMaxBackoffMs = Math.min(maxBackoffMs, max);
    }

    // 监听器通知封装（防御式，单个监听异常不影响其它监听）
    private void notifyConnecting() {
        synchronized (connectionListeners) {
            for (ConnectionListener l : connectionListeners) {
                try {
                    l.onConnecting(wsUrl);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyConnected() {
        synchronized (connectionListeners) {
            for (ConnectionListener l : connectionListeners) {
                try {
                    l.onConnected(wsUrl);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyDisconnected(int code, String reason, Throwable cause) {
        synchronized (connectionListeners) {
            for (ConnectionListener l : connectionListeners) {
                try {
                    l.onDisconnected(wsUrl, code, reason, cause);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyReconnecting(int attempt, long nextDelayMs) {
        synchronized (connectionListeners) {
            for (ConnectionListener l : connectionListeners) {
                try {
                    l.onReconnecting(wsUrl, attempt, nextDelayMs);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyReconnectFailed(int attempted, Throwable lastError) {
        synchronized (connectionListeners) {
            for (ConnectionListener l : connectionListeners) {
                try {
                    l.onReconnectFailed(wsUrl, attempted, lastError);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyNetworkUnavailable() {
        synchronized (connectionListeners) {
            for (ConnectionListener l : connectionListeners) {
                try {
                    l.onNetworkUnavailable(wsUrl);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void notifyNetworkAvailable() {
        synchronized (connectionListeners) {
            for (ConnectionListener l : connectionListeners) {
                try {
                    l.onNetworkAvailable(wsUrl);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 通知：用户回调异常（防御式，单个监听异常不影响其它监听）
     */
    private void notifyCallbackError(String identifier, JsonNode msg, Throwable error) {
        synchronized (callbackErrorListeners) {
            for (CallbackErrorListener l : callbackErrorListeners) {
                try {
                    l.onCallbackError(wsUrl, identifier, msg, error);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 订阅消息。
     *
     * @param subscription 订阅对象
     * @param callback     回调
     */
    public void subscribe(JsonNode subscription, MessageCallback callback) {
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", "subscribe");
        payload.put("subscription", subscription);
        try {
            webSocket.send(JSONUtil.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
    }

    /**
     * 取消订阅。
     *
     * @param subscription 订阅对象
     */
    public void unsubscribe(JsonNode subscription) {
        String identifier = subscriptionToIdentifier(subscription);
        List<ActiveSubscription> list = subscriptions.get(identifier);
        if (list != null) {
            list.removeIf(s -> s.subscription.equals(subscription));
            if (list.isEmpty())
                subscriptions.remove(identifier);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", "unsubscribe");
        payload.put("subscription", subscription);
        try {
            webSocket.send(JSONUtil.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
    }

    /**
     * 将订阅对象转换为标识符（用于多订阅去重与路由）。
     * 频道标识符约定：
     * - l2Book:{coin}，trades:{coin}，bbo:{coin}，candle:{coin},{interval}
     * - userEvents，orderUpdates，allMids
     * -
     * userFills:{user}，userFundings:{user}，userNonFundingLedgerUpdates:{user}，webData2:{user}
     * - activeAssetCtx:{coin}，activeAssetData:{coin},{user}
     * - coin 可为字符串或整数；字符串统一转换为小写。
     */
    public String subscriptionToIdentifier(JsonNode subscription) {
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
                String coinKey = coinNode == null ? null
                        : (coinNode.isNumber() ? String.valueOf(coinNode.asInt())
                        : coinNode.asText().toLowerCase(Locale.ROOT));
                return coinKey == null ? type : type + ":" + coinKey;
            }
            case "trades":
            case "bbo":
            case "activeAssetCtx": {
                JsonNode coinNode = subscription.get("coin");
                String coinKey = coinNode == null ? null
                        : (coinNode.isNumber() ? String.valueOf(coinNode.asInt())
                        : coinNode.asText().toLowerCase(Locale.ROOT));
                return coinKey == null ? type : type + ":" + coinKey;
            }
            case "candle": {
                JsonNode coinNode = subscription.get("coin");
                JsonNode iNode = subscription.get("interval");
                String coinKey = coinNode == null ? null
                        : (coinNode.isNumber() ? String.valueOf(coinNode.asInt())
                        : coinNode.asText().toLowerCase(Locale.ROOT));
                String interval = iNode == null ? null : iNode.asText();
                if (coinKey != null && interval != null)
                    return type + ":" + coinKey + "," + interval;
                return type;
            }
            case "userFills":
            case "userFundings":
            case "userNonFundingLedgerUpdates":
            case "webData2": {
                JsonNode userNode = subscription.get("user");
                String user = userNode == null ? null : userNode.asText().toLowerCase(Locale.ROOT);
                return user == null ? type : type + ":" + user;
            }
            case "activeAssetData": {
                JsonNode coinNode = subscription.get("coin");
                JsonNode userNode = subscription.get("user");
                String coinKey = coinNode == null ? null
                        : (coinNode.isNumber() ? String.valueOf(coinNode.asInt())
                        : coinNode.asText().toLowerCase(Locale.ROOT));
                String user = userNode == null ? null : userNode.asText().toLowerCase(Locale.ROOT);
                if (coinKey != null && user != null)
                    return type + ":" + coinKey + "," + user;
                return type;
            }
            default:
                return type;
        }
    }

    /**
     * 从消息中提取标识符，与 subscriptionToIdentifier 对应。
     * 兼容两种消息格式：channel 为字符串或对象（{type: ..., ...}）。
     */
    public String wsMsgToIdentifier(JsonNode msg) {
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
                String coinKey = coinNode.isNumber() ? String.valueOf(coinNode.asInt())
                        : (coinNode.isTextual() ? coinNode.asText().toLowerCase(Locale.ROOT) : null);
                return coinKey != null ? type + ":" + coinKey : type;
            }
            case "trades": {
                JsonNode trades = msg.get("data");
                String coinKey = null;
                if (trades != null && trades.isArray() && !trades.isEmpty()) {
                    JsonNode first = trades.get(0);
                    JsonNode coinNode = first.get("coin");
                    if (coinNode != null) {
                        coinKey = coinNode.isTextual() ? coinNode.asText().toLowerCase(Locale.ROOT)
                                : (coinNode.isNumber() ? String.valueOf(coinNode.asInt()) : null);
                    }
                }
                return coinKey != null ? type + ":" + coinKey : type;
            }
            case "candle": {
                JsonNode data = msg.get("data");
                if (data != null) {
                    String s = data.path("s").asText(null);
                    String i = data.path("i").asText(null);
                    if (s != null && i != null)
                        return type + ":" + s.toLowerCase(Locale.ROOT) + "," + i;
                }
                return type;
            }
            case "bbo": {
                JsonNode coinNode = msg.path("data").path("coin");
                String coinKey = coinNode.isTextual() ? coinNode.asText().toLowerCase(Locale.ROOT)
                        : (coinNode.isNumber() ? String.valueOf(coinNode.asInt()) : null);
                return coinKey != null ? type + ":" + coinKey : type;
            }
            case "userFills":
            case "userFundings":
            case "userNonFundingLedgerUpdates":
            case "webData2": {
                JsonNode userNode = msg.path("data").path("user");
                String user = (userNode != null && userNode.isTextual()) ? userNode.asText().toLowerCase(Locale.ROOT)
                        : null;
                return user != null ? type + ":" + user : type;
            }
            case "activeAssetCtx":
            case "activeSpotAssetCtx": {
                JsonNode coinNode = msg.path("data").path("coin");
                String coinKey = coinNode.isTextual() ? coinNode.asText().toLowerCase(Locale.ROOT)
                        : (coinNode.isNumber() ? String.valueOf(coinNode.asInt()) : null);
                return type.equals("activeSpotAssetCtx") ? "activeAssetCtx:" + (coinKey != null ? coinKey : "unknown")
                        : (coinKey != null ? type + ":" + coinKey : type);
            }
            case "activeAssetData": {
                JsonNode data = msg.get("data");
                if (data != null) {
                    JsonNode coinNode = data.get("coin");
                    JsonNode userNode = data.get("user");
                    String coinKey = coinNode != null
                            ? (coinNode.isTextual() ? coinNode.asText().toLowerCase(Locale.ROOT)
                            : (coinNode.isNumber() ? String.valueOf(coinNode.asInt()) : null))
                            : null;
                    String user = (userNode != null && userNode.isTextual())
                            ? userNode.asText().toLowerCase(Locale.ROOT)
                            : null;
                    if (coinKey != null && user != null)
                        return type + ":" + coinKey + "," + user;
                }
                return type;
            }
            default:
                return type;
        }
    }
}
