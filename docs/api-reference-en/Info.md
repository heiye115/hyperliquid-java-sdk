# Info

**Class:** `io.github.hyperliquid.sdk.apis.Info`

Maps to Hyperliquid **`POST /info`**, plus WebSocket subscription helpers when **`skipWs` is false**. Uses Caffeine for `meta`, `spotMeta`, `allMids`, etc.

## Constructors

| Constructor | Description |
|-------------|-------------|
| `Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs)` | Default `CacheConfig` |
| `Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs, CacheConfig cacheConfig)` | Custom cache config |
| `Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs, CacheConfig cacheConfig, List<String> perpDexs)` | Preload builder-deployed perp DEX metadata |

Applications normally obtain `Info` via `HyperliquidClient.builder()...build()`, not `new Info(...)`.

## Low-level request

| Method | Description |
|--------|-------------|
| `JsonNode postInfo(Object payload)` | Raw `POST /info` with `Map` or POJO body; returns raw JSON |

## Symbols and asset IDs

| Method | Description |
|--------|-------------|
| `Integer nameToAsset(String coinName)` | Resolves to **global asset id** (default perp universe, spot `10000 + index`, or `dex:COIN` perp DEX); throws `HypeError` on failure |
| `Meta.Universe getMetaUniverse(String coinName)` | Uses `nameToAsset` + default `meta` universe |
| `Integer getSzDecimals(String coinName)` | Size decimals for order formatting; from cache or meta/spot meta |

Multi-DEX perp: `dex:COIN` uses that DEX’s `meta` and offset rules (offset related to `perpDexs` index; see source comments).

## Market data and metadata

| Method | Description |
|--------|-------------|
| `Map<String, String> allMids()` | `type: allMids`, default DEX |
| `Map<String, String> allMids(String dex)` | Optional perp DEX; `dex` may be null |
| `Map<String, String> getCachedAllMids()` | Cached ~**1 second** TTL |
| `Map<String, String> getCachedAllMids(String dex)` | Same with DEX |
| `Meta meta()` | `type: meta`, default DEX |
| `Meta meta(String dex)` | Named DEX |
| `JsonNode metaAndAssetCtxs()` | Raw JSON |
| `MetaAndAssetCtxs metaAndAssetCtxsTyped()` | Typed |
| `SpotMeta spotMeta()` | `type: spotMeta` |
| `JsonNode spotMetaAndAssetCtxs()` | Raw JSON |
| `SpotMetaAndAssetCtxs spotMetaAndAssetCtxsTyped()` | Typed |
| `JsonNode perpDexs()` | `type: perpDexs` |
| `List<Map<String, Object>> perpDexsTyped()` | Elements may be null or maps |
| `JsonNode perpDexStatus(String dex)` | `type: perpDexStatus`; empty string = first perp DEX |
| `PerpDexStatus perpDexStatusTyped(String dex)` | Typed |

## Meta / SpotMeta cache helpers

| Method | Description |
|--------|-------------|
| `Meta loadMetaCache()` | Default DEX; loads and builds coin map on miss |
| `Meta loadMetaCache(String dex)` | Named DEX |
| `Meta refreshMetaCache(String dex)` | Invalidates and reloads; clears in-memory coin/precision maps first |
| `Meta refreshMetaCache()` | Default DEX |
| `void clearMetaCache()` | Clears meta cache and mappings |
| `CacheStats getMetaCacheStats()` | Caffeine stats if enabled |
| `SpotMeta loadSpotMetaCache()` | Loads spot meta cache |
| `SpotMeta refreshSpotMetaCache()` | Force refresh + rebuild spot mappings |
| `void clearSpotMetaCache()` | Clears spot cache and spot-derived mappings |
| `CacheStats getSpotMetaCacheStats()` | Spot cache stats |
| `void warmUpCache()` | Warm default meta + spotMeta + mappings |
| `void warmUpCache(List<String> dexList)` | Load meta for each DEX + spotMeta |

## Order book and candles

| Method | Description |
|--------|-------------|
| `L2Book l2Book(String coin, Integer nSigFigs, Integer mantissa)` | `type: l2Book`; `nSigFigs` 2–5; `mantissa` only if `nSigFigs==5` (1/2/5) |
| `L2Book l2Book(String coin)` | Full precision |
| `List<Candle> candleSnapshot(String coin, CandleInterval interval, Long startTime, Long endTime)` | `type: candleSnapshot`; **~5000 bars max**; ms timestamps; `coin` may be `@107` style |
| `Candle candleSnapshotLatest(String coin, CandleInterval interval)` | Last candle in a short window (may be in-progress; server-dependent) |
| `List<Candle> candleSnapshotByCount(String coin, CandleInterval interval, int count)` | `count` 1–5000; ascending; may trim to last `count` |
| `List<Candle> candleSnapshotByDays(String coin, CandleInterval interval, int days)` | `days > 0`, rolling window from now |
| `List<Candle> candleSnapshotByDate(String coin, CandleInterval interval, int year, int month, int day)` | **UTC** calendar day |
| `List<Candle> candleSnapshotCurrent(String coin, CandleInterval interval)` | Last bar in window (usually the forming candle) |

`CandleInterval`: enum in `io.github.hyperliquid.sdk.model.info.CandleInterval` (`getCode()`, `toMillis()`, etc.).

## User and order queries

| Method | Description |
|--------|-------------|
| `List<OpenOrder> openOrders(String address)` | `type: openOrders`, default DEX |
| `List<OpenOrder> openOrders(String address, String dex)` | Optional DEX |
| `List<FrontendOpenOrder> frontendOpenOrders(String address)` | `type: frontendOpenOrders` |
| `List<FrontendOpenOrder> frontendOpenOrders(String address, String dex)` | Optional DEX |
| `List<UserFill> userFills(String address)` | `type: userFills`, up to ~2000 rows |
| `List<UserFill> userFills(String address, Boolean aggregateByTime)` | Optional aggregation |
| `List<UserFill> userFillsByTime(String address, Long startTime)` | Optional end/aggregate null |
| `List<UserFill> userFillsByTime(String address, Long startTime, Long endTime)` | |
| `List<UserFill> userFillsByTime(String address, Long startTime, Boolean aggregateByTime)` | |
| `List<UserFill> userFillsByTime(String address, Long startTime, Long endTime, Boolean aggregateByTime)` | ~2000 per call; server retains ~10000 recent fills window |
| `UserFees userFees(String address)` | `type: userFees` |
| `List<FundingHistory> fundingHistory(String coin, long startMs, long endMs)` | Funding history for coin |
| `List<FundingHistory> fundingHistory(String coin, long startMs, Long endMs)` | `endMs` nullable |
| `JsonNode userFundingHistory(String address, long startMs, Long endMs)` | `type: userFunding` |
| `JsonNode userFundingHistory(String address, int coin, long startMs, long endMs)` | Converts int using **default** `meta` universe index → `"@" + id`; only valid for assets in that universe |
| `JsonNode userFundingHistory(String address, String coin, long startMs, long endMs)` | Name or `@id` |
| `JsonNode userNonFundingLedgerUpdates(String address, long startMs, long endMs)` | Non-funding ledger |
| `JsonNode userNonFundingLedgerUpdates(String address, long startMs, Long endMs)` | Nullable end |
| `JsonNode historicalOrders(String address, long startMs, long endMs)` | Historical orders |
| `JsonNode historicalOrders(String address)` | Latest window (server default) |
| `JsonNode userTwapSliceFills(String address, long startMs, long endMs)` | TWAP slice fills |
| `JsonNode userTwapSliceFills(String address)` | Latest window |
| `OrderStatus orderStatus(String address, Long oid)` | `type: orderStatus` |
| `OrderStatus orderStatusByCloid(String address, Cloid cloid)` | Query by cloid (Python `query_order_by_cloid` alignment) |
| `OrderStatus orderStatusByCloid(String address, String cloid)` | Parses string to `Cloid` |
| `ClearinghouseState clearinghouseState(String address)` | `type: clearinghouseState`, default DEX |
| `ClearinghouseState clearinghouseState(String address, String dex)` | Optional DEX |
| `ClearinghouseState userState(String address)` | Same as `clearinghouseState(address, null)` |
| `SpotClearinghouseState spotClearinghouseState(String user)` | `type: spotClearinghouseState` |
| `UserRateLimit userRateLimit(String address)` | `type: userRateLimit` |
| `JsonNode portfolio(String address)` | `type: portfolio` |
| `JsonNode userRole(String address)` | `type: userRole` |
| `JsonNode vaultDetails(String vaultAddress, String user)` | `type: vaultDetails`; `user` optional |
| `JsonNode spotDeployState(String address)` | `type: spotDeployState` |
| `JsonNode queryReferralState(String address)` | `type: referral` |
| `JsonNode querySubAccounts(String address)` | `type: subAccounts` |
| `JsonNode queryUserToMultiSigSigners(String address)` | `type: userToMultiSigSigners` |
| `JsonNode queryPerpDeployAuctionStatus()` | `type: perpDeployAuctionStatus` |
| `JsonNode querySpotDeployAuctionStatus(String address)` | **Same as** `spotDeployState(address)` |
| `@Deprecated JsonNode queryUserDexAbstractionState(String address)` | Prefer `userAbstraction` / Exchange abstraction APIs |
| `String userAbstraction(String user)` | `type: userAbstraction`, plain-text status |
| `JsonNode userVaultEquities(String address)` | `type: userVaultEquities` |
| `JsonNode extraAgents(String address)` | `type: extraAgents` |
| `JsonNode userStakingSummary(String address)` | `type: delegatorSummary` |
| `JsonNode userStakingDelegations(String address)` | `type: delegations` |
| `JsonNode userStakingRewards(String address)` | `type: delegatorRewards` |
| `JsonNode delegatorHistory(String user)` | `type: delegatorHistory` |
| `List<String> approvedBuilders(String user)` | `type: approvedBuilders` |
| `Long maxBuilderFee(String user, String builder)` | `type: maxBuilderFee`, parsed from text |

## WebSocket (when `skipWs == false`)

If the client was built with `skipWs(true)`, these throw `HypeError("WebSocket disabled by skipWs")` or no-op per method.

**Note:** For builder-deployed perp DEX symbols (e.g., `xyz:SP500`), preload the DEX via `HyperliquidClient.builder().perpDexs(List.of("xyz"))` before subscribing.

| Method | Description |
|--------|-------------|
| `void subscribe(Subscription subscription, MessageCallback callback)` | Type-safe; **remaps** `coin` (spot aliases, `dex:COIN`, …) |
| `SubscriptionHandle subscribeWithHandle(Subscription subscription, MessageCallback callback)` | Returns cancellable handle |
| `void subscribe(JsonNode subscription, MessageCallback callback)` | Raw JSON subscription |
| `SubscriptionHandle subscribeWithHandle(JsonNode subscription, MessageCallback callback)` | |
| `Map<String, List<ActiveSubscription>> getSubscriptions()` | Snapshot of active subs |
| `void unsubscribe(Subscription subscription)` | Unsubscribe |
| `void unsubscribe(JsonNode subscription)` | |
| `boolean unsubscribe(SubscriptionHandle handle)` | By handle |
| `boolean unsubscribe(long subscriptionId)` | By id |
| `void closeWs()` | Closes WS |
| `WebsocketManager getWsManager()` | Underlying manager; null if `skipWs` |
| `void addConnectionListener(ConnectionListener listener)` | Connection/reconnect/network |
| `void removeConnectionListener(ConnectionListener listener)` | |
| `void setNetworkCheckIntervalSeconds(int seconds)` | Probe interval when disconnected (~5s default) |
| `void setReconnectBackoffMs(long initialMs, long maxMs)` | Exponential backoff |
| `void setNetworkProbeUrl(String url)` | Custom probe URL (default API base) |
| `void setNetworkProbeDisabled(boolean disabled)` | Skip HTTP probe if true |
| `void addCallbackErrorListener(CallbackErrorListener listener)` | User callback errors |
| `void removeCallbackErrorListener(CallbackErrorListener listener)` | |

Subscription types live under `io.github.hyperliquid.sdk.model.subscription` (e.g. `TradesSubscription`, `L2BookSubscription`, `CandleSubscription`, `BboSubscription`).
