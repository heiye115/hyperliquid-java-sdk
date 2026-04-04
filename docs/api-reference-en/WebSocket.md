# WebSocket (`WebsocketManager`)

**Class:** `io.github.hyperliquid.sdk.websocket.WebsocketManager`

Created inside `Info` when **`skipWs` is false**. URL rule: `http`→`ws`, `https`→`wss`, append **`/ws`** (e.g. `https://api.hyperliquid.xyz` → `wss://api.hyperliquid.xyz/ws`).

Prefer **`Info.subscribe(...)`** so **`coin` is remapped** consistently with `meta`/`spotMeta` (`remapCoinInSubscription`).

## Construction and lifecycle

| Method | Description |
|--------|-------------|
| `WebsocketManager(String baseUrl)` | Calls `connect()` immediately; starts ~**20s** `ping` timer |

| Method | Description |
|--------|-------------|
| `void stop()` | Stops reconnect, closes socket, shuts scheduler and OkHttp; further subscribe throws `IllegalStateException` |

## Subscribe and unsubscribe

| Method | Description |
|--------|-------------|
| `void subscribe(Subscription subscription, MessageCallback callback)` | Same as `subscribeWithHandle`, discards handle |
| `SubscriptionHandle subscribeWithHandle(Subscription subscription, MessageCallback callback)` | Type-safe; converted to JSON internally |
| `void subscribe(JsonNode subscription, MessageCallback callback)` | Raw JSON |
| `SubscriptionHandle subscribeWithHandle(JsonNode subscription, MessageCallback callback)` | |

Multiple callbacks may share the same subscription JSON (bucketed by identifier). Duplicate subscription + same callback returns the existing handle.

| Method | Description |
|--------|-------------|
| `void unsubscribe(Subscription subscription)` | Removes one matching subscription by equality; sends server `unsubscribe` if no identical body remains |
| `void unsubscribe(JsonNode subscription)` | Same |
| `boolean unsubscribe(SubscriptionHandle handle)` | Removes one entry by id; server `unsubscribe` only if no duplicate subscription JSON remains |
| `boolean unsubscribe(long subscriptionId)` | Same |

| Method | Description |
|--------|-------------|
| `Map<String, List<ActiveSubscription>> getSubscriptions()` | Copy of active subscriptions |
| `List<ActiveSubscription> getSubscriptionsByIdentifier(String identifier)` | By routing id |
| `boolean hasSubscriptions()` | |
| `int getSubscriptionCount()` | Number of distinct identifiers |

## Callbacks and listeners

| Interface | Role |
|-----------|------|
| `MessageCallback` | `void onMessage(JsonNode msg)` |
| `ConnectionListener` | `onConnecting` / `onConnected` / `onDisconnected` / `onReconnecting` / `onReconnectFailed` / `onNetworkUnavailable` / `onNetworkAvailable` |
| `CallbackErrorListener` | Fired when user callback throws; other subscriptions continue |

## Connection and network

- On disconnect: **exponential backoff** reconnect (~1s initial + jitter, capped by `setReconnectBackoffMs` and an internal cap), **retries until** `stop()`.
- While disconnected, optional **HEAD** probe to `baseUrl` or `setNetworkProbeUrl`; on success, reconnect ASAP.

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
- `userFills:{user}`, `userFundings:{user}`, `userNonFundingLedgerUpdates:{user}`, `webData2:{user}`
- `activeAssetCtx:{coin}`, `activeAssetData:{coin},{user}`

`coin` in identifiers is usually lowercase; see `wsMsgToIdentifier` in source for details.

## Info layer

- `Info.closeWs()` → `wsManager.stop()`.
- If `skipWs == true`: `subscribe` / `getSubscriptions` throw `HypeError`; some `unsubscribe` paths no-op; listener setters return without effect.
