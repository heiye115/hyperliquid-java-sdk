# WebSocket (`WebsocketManager`)

**Class:** `io.github.hyperliquid.sdk.websocket.WebsocketManager`

Created inside `Info` when **`skipWs` is false**. URL rule: `http`→`ws`, `https`→`wss`, append **`/ws`** (e.g. `https://api.hyperliquid.xyz` → `wss://api.hyperliquid.xyz/ws`).

Prefer **`Info.subscribe(...)`** so **`coin` is remapped** consistently with `meta`/`spotMeta` (`remapCoinInSubscription`).

**Builder-deployed perp DEX symbols** (e.g., `xyz:SP500`) require preloading the DEX metadata via `HyperliquidClient.builder().perpDexs(List.of("xyz"))` before subscribing.

## Architecture Overview

- **Scheduler (platform thread)** owns **time**: ping, reconnect, network monitor, dedicated-close delays. All scheduled tasks are non-blocking triggers.
- **Callback executor (JDK 21 virtual threads)** owns **execution**: user `MessageCallback.onMessage()` invocations are dispatched to virtual threads so that slow/blocking callbacks cannot stall the OkHttp WS reader thread.
- These two dimensions are **not interchangeable**: virtual threads do not support `scheduleAtFixedRate`; the scheduler is not designed for concurrent execution.

## Construction and lifecycle

| Method | Description |
|--------|-------------|
| `WebsocketManager(String baseUrl)` | Calls `connect()` immediately; starts ~**20s** `ping` timer |

| Method | Description |
|--------|-------------|
| `void stop()` | Stops reconnect, closes socket, shuts down callback executor and scheduler, releases OkHttp resources; further subscribe throws `IllegalStateException` |

## Subscribe and unsubscribe

| Method | Description |
|--------|-------------|
| `void subscribe(Subscription subscription, MessageCallback callback)` | Same as `subscribeWithHandle`, discards handle |
| `ActiveSubscription subscribeWithHandle(Subscription subscription, MessageCallback callback)` | Type-safe; converted to JSON internally |
| `void subscribe(JsonNode subscription, MessageCallback callback)` | Raw JSON |
| `ActiveSubscription subscribeWithHandle(JsonNode subscription, MessageCallback callback)` | Returns ActiveSubscription for targeted unsubscribe |

Duplicate subscription + same callback returns the existing ActiveSubscription.

**Note:** `ActiveSubscription` is in package `io.github.hyperliquid.sdk.model.subscription`.

| Method | Description |
|--------|-------------|
| `void unsubscribe(Subscription subscription)` | Removes one matching subscription by equality; sends server `unsubscribe` if no identical body remains |
| `void unsubscribe(JsonNode subscription)` | Same |
| `boolean unsubscribe(ActiveSubscription activeSub)` | Removes one entry by id |
| `boolean unsubscribe(long subscriptionId)` | Same |

| Method | Description |
|--------|-------------|
| `Map<String, ActiveSubscription> getSubscriptions()` | Copy of active subscriptions (includes dedicated connections) |
| `ActiveSubscription getSubscriptionsByIdentifier(String identifier)` | By routing id |
| `boolean hasSubscriptions()` | |
| `int getSubscriptionCount()` | Number of distinct identifiers |

## Callbacks and listeners

| Interface | Role |
|-----------|------|
| `MessageCallback` | `void onMessage(JsonNode msg)` — dispatched on a virtual thread |
| `ConnectionListener` | `onConnecting` / `onConnected` / `onDisconnected` / `onReconnecting` / `onReconnectFailed` / `onNetworkUnavailable` / `onNetworkAvailable` |
| `CallbackErrorListener` | Fired when user callback throws; other subscriptions continue |

## Multi-user isolation (dedicated connections)

Subscriptions for `orderUpdates` and `userEvents` receive server messages **without a `user` field**, making it impossible to route messages to the correct callback on a shared connection. The SDK solves this with **per-user dedicated WebSocket connections**:

- When a `orderUpdates` or `userEvents` subscription includes a `user` field, the main manager routes it to a dedicated connection keyed by `"user:{address}"`.
- Both `orderUpdates` and `userEvents` for the **same wallet** share **one** dedicated connection (reducing connection count).
- Dedicated connections inherit backoff, probe, and listener configuration from the main manager.
- Reference-counted lifecycle: when all subscriptions for a user are removed, the dedicated connection is closed after a 30-second idle delay.

## Connection and network

- On disconnect: **exponential backoff** reconnect (~1s initial + jitter, capped by `setReconnectBackoffMs` and an internal cap), **retries until** `stop()`.
- While disconnected, optional **HEAD** probe to `baseUrl` or `setNetworkProbeUrl`; on success, reconnect ASAP.
- `onFailure` / `onClosed` are guarded by `disconnectClaimed` to prevent double-reconnect and handle first-connection failures.

| Method | Description |
|--------|-------------|
| `void setNetworkProbeUrl(String url)` | Custom probe URL |
| `void setNetworkProbeDisabled(boolean disabled)` | If true, network always considered up |
| `void setNetworkCheckIntervalSeconds(int seconds)` | Probe interval, min 1s |
| `void setReconnectBackoffMs(long initialMs, long maxMs)` | Backoff bounds (capped by internal ~30s ceiling) |

## Message routing identifiers (partial)

Matches `subscriptionToIdentifier` for routing pushes to callbacks:

- `l2Book:{coin}`, `trades:{coin}`, `bbo:{coin}`, `candle:{coin},{interval}`
- `allMids`, `userEvents`, `orderUpdates`
- `userFills:{user}`, `userFundings:{user}`, `userNonFundingLedgerUpdates:{user}`, `webData2`
- `activeAssetCtx:{coin}`, `activeAssetData:{coin},{user}`, `openOrders:{user}`, `clearinghouseState:{user}`, `spotState:{user}`

**Channel mapping note:** The server sends channel `"user"` for `userEvents` subscriptions. The SDK maps this automatically — no user action required.

`coin` in identifiers is usually lowercase; see source for details.

## Info layer

- `Info.closeWs()` → `wsManager.stop()`.
- If `skipWs == true`: `subscribe` / `getSubscriptions` throw `HypeError`; some `unsubscribe` paths no-op; listener setters return without effect.
