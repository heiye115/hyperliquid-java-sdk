# WebSocket（WebsocketManager）

**类**：`io.github.hyperliquid.sdk.websocket.WebsocketManager`

由 `Info` 在 **`skipWs == false`** 时内部创建。URL 规则：将 API `baseUrl` 的 `http`→`ws`、`https`→`wss`，并追加路径 **`/ws`**（例如 `https://api.hyperliquid.xyz` → `wss://api.hyperliquid.xyz/ws`）。

应用侧推荐通过 **`Info.subscribe(...)`** 使用，以便与 `meta`/`spotMeta` 一致的 **coin 名称规范化**（`remapCoinInSubscription`）。

**Builder-deployed 永续 DEX 符号**（如 `xyz:SP500`）需在订阅前通过 `HyperliquidClient.builder().perpDexs(List.of("xyz"))` 预加载该 DEX 元数据。

## 架构概览

- **调度器（平台线程）**负责**时间**：ping、重连、网络探测、专属连接延迟关闭。所有调度任务均为非阻塞触发器。
- **回调执行器（JDK 21 虚拟线程）**负责**执行**：用户 `MessageCallback.onMessage()` 调用被派发到虚拟线程，确保缓慢/阻塞的用户回调不会拖慢 OkHttp WS 读取线程。
- 这两个维度**不可互换**：虚拟线程不支持 `scheduleAtFixedRate`；调度器不是为并发执行而设计的。

## 构造与生命周期

| 方法 | 说明 |
|------|------|
| `WebsocketManager(String baseUrl)` | 立即 `connect()`，并启动约 **20s** 一次的 `ping` 定时任务 |

| 方法 | 说明 |
|------|------|
| `void stop()` | 停止重连、关闭连接、关闭回调执行器和调度器、释放 OkHttp 资源；之后不可再订阅（再订阅会抛 `IllegalStateException`） |

## 订阅与取消

| 方法 | 说明 |
|------|------|
| `void subscribe(Subscription subscription, MessageCallback callback)` | 等价于 `subscribeWithHandle` 但忽略返回值 |
| `ActiveSubscription subscribeWithHandle(Subscription subscription, MessageCallback callback)` | 类型安全；转为 JSON 后走 JsonNode 流程 |
| `void subscribe(JsonNode subscription, MessageCallback callback)` | 原始 JSON |
| `ActiveSubscription subscribeWithHandle(JsonNode subscription, MessageCallback callback)` | 返回 ActiveSubscription 用于定向取消订阅 |

完全相同订阅 + 相同 callback 会返回已有 ActiveSubscription。

**注意：** `ActiveSubscription` 位于包 `io.github.hyperliquid.sdk.model.subscription`。

| 方法 | 说明 |
|------|------|
| `void unsubscribe(Subscription subscription)` | 按**内容相等**移除一条；若该 identifier 下无完全相同订阅体则发 `unsubscribe` |
| `void unsubscribe(JsonNode subscription)` | 同上 |
| `boolean unsubscribe(ActiveSubscription activeSub)` | 按 id 移除一条 |
| `boolean unsubscribe(long subscriptionId)` | 同上 |

| 方法 | 说明 |
|------|------|
| `Map<String, ActiveSubscription> getSubscriptions()` | 当前活跃订阅副本（含专属连接） |
| `ActiveSubscription getSubscriptionsByIdentifier(String identifier)` | 按路由 identifier |
| `boolean hasSubscriptions()` | |
| `int getSubscriptionCount()` | 不同 identifier 个数 |

## 回调与监听

| 接口 | 说明 |
|------|------|
| `MessageCallback` | `void onMessage(JsonNode msg)` — 在虚拟线程上派发执行 |
| `ConnectionListener` | `onConnecting` / `onConnected` / `onDisconnected` / `onReconnecting` / `onReconnectFailed` / `onNetworkUnavailable` / `onNetworkAvailable` |
| `CallbackErrorListener` | 用户 `onMessage` 抛错时回调，不影响其他订阅 |

## 多用户隔离（专属连接）

`orderUpdates` 和 `userEvents` 的服务端消息**不包含 `user` 字段**，无法在共享连接上路由到正确的回调。SDK 通过**按用户分配专属 WebSocket 连接**解决此问题：

- 当 `orderUpdates` 或 `userEvents` 订阅包含 `user` 字段时，主管理器将其路由到以 `"user:{address}"` 为 key 的专属连接。
- 同一钱包的 `orderUpdates` 和 `userEvents` **共享一个**专属连接（减少连接数）。
- 专属连接继承主管理器的退避、探测和监听器配置。
- 引用计数生命周期：当某用户的所有订阅被移除后，专属连接在 30 秒空闲延迟后自动关闭。

## 连接与网络

- 断线后：**指数退避**重连（初始约 1s + 抖动，上限受 `setReconnectBackoffMs` 与内部上限约束），**无限重试**直至 `stop()`。
- 断线期间可启动**网络探测**：对 `baseUrl` 或 `setNetworkProbeUrl` 发 **HEAD**，成功则尽快重连。
- `onFailure` / `onClosed` 由 `disconnectClaimed` 守卫，防止双重重连并覆盖首次连接失败场景。

| 方法 | 说明 |
|------|------|
| `void setNetworkProbeUrl(String url)` | 自定义探测 URL |
| `void setNetworkProbeDisabled(boolean disabled)` | `true` 时探测始终视为可用 |
| `void setNetworkCheckIntervalSeconds(int seconds)` | 探测周期，至少 1 秒 |
| `void setReconnectBackoffMs(long initialMs, long maxMs)` | 重连退避初值与上限（上限不超过内部 30s 硬顶） |

## 消息路由 identifier（节选）

与订阅 JSON 的 `subscriptionToIdentifier` 一致，用于将服务端推送路由到对应 callback：

- `l2Book:{coin}`、`trades:{coin}`、`bbo:{coin}`、`candle:{coin},{interval}`
- `allMids`、`userEvents`、`orderUpdates`
- `userFills:{user}`、`userFundings:{user}`、`userNonFundingLedgerUpdates:{user}`、`webData2`
- `activeAssetCtx:{coin}`、`activeAssetData:{coin},{user}`、`openOrders:{user}`、`clearinghouseState:{user}`、`spotState:{user}`

**channel 映射说明：** 服务端对 `userEvents` 订阅发送 channel `"user"`，SDK 自动完成映射，用户无需额外操作。

`coin` 在 identifier 中通常为小写；具体匹配逻辑见源码。

## Info 层补充

- `Info.closeWs()` → `wsManager.stop()`。
- `skipWs == true` 时：`subscribe` / `getSubscriptions` 等会抛 `HypeError`；`unsubscribe` 部分为 no-op；监听器设置直接返回。
