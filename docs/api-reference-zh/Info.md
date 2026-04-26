# Info

**类**：`io.github.hyperliquid.sdk.apis.Info`

对应 Hyperliquid **`POST /info`** 与（在未 `skipWs` 时）WebSocket 订阅管理。内部使用 Caffeine 缓存 `meta`、`spotMeta`、`allMids` 等。

## 构造

| 构造器 | 说明 |
|--------|------|
| `Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs)` | 默认 `CacheConfig` |
| `Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs, CacheConfig cacheConfig)` | 自定义缓存配置 |
| `Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs, CacheConfig cacheConfig, List<String> perpDexs)` | 预加载 builder-deployed 永续 DEX 元数据 |

应用代码一般通过 `HyperliquidClient.builder()...build()` 获得 `Info`，无需直接 `new Info(...)`。

## 底层请求

| 方法 | 说明 |
|------|------|
| `JsonNode postInfo(Object payload)` | 直接发送 `POST /info`，`payload` 为 `Map` 或 POJO；返回原始 JSON |

## 符号与资产 ID

| 方法 | 说明 |
|------|------|
| `Integer nameToAsset(String coinName)` | 将币种名称解析为**全局 asset id**（永续默认盘、现货 `10000+index`，或 `dex:COIN` 形式 perp dex）；失败抛 `HypeError` |
| `Meta.Universe getMetaUniverse(String coinName)` | 通过 `nameToAsset` + 默认盘 `meta` 取 universe 元素 |
| `Integer getSzDecimals(String coinName)` | 数量精度 `szDecimals`，用于下单格式化；来自缓存或 meta/spot meta |

永续多 dex：`dex:COIN` 会按该 dex 的 `meta` 与 offset 规则解析全局 id（offset 与 `perpDexs` 中索引相关，见源码注释）。

## 行情与元数据

| 方法 | 说明 |
|------|------|
| `Map<String, String> allMids()` | `type: allMids`，默认 dex |
| `Map<String, String> allMids(String dex)` | 可指定 perp dex；`dex` 可为 `null` |
| `Map<String, String> getCachedAllMids()` | 缓存约 **1 秒** TTL |
| `Map<String, String> getCachedAllMids(String dex)` | 同上，可指定 dex |
| `Meta meta()` | `type: meta`，默认 dex |
| `Meta meta(String dex)` | 指定 dex |
| `JsonNode metaAndAssetCtxs()` | 原始 JSON |
| `MetaAndAssetCtxs metaAndAssetCtxsTyped()` | 类型化结果 |
| `SpotMeta spotMeta()` | `type: spotMeta` |
| `JsonNode spotMetaAndAssetCtxs()` | 原始 JSON |
| `SpotMetaAndAssetCtxs spotMetaAndAssetCtxsTyped()` | 类型化结果 |
| `JsonNode perpDexs()` | `type: perpDexs` |
| `List<Map<String, Object>> perpDexsTyped()` | 列表元素可能为 `null` 或 `Map` |
| `JsonNode perpDexStatus(String dex)` | `type: perpDexStatus`；空字符串表示第一个 perp dex |
| `PerpDexStatus perpDexStatusTyped(String dex)` | 类型化 |

## Meta / SpotMeta 缓存 API

| 方法 | 说明 |
|------|------|
| `Meta loadMetaCache()` | 默认 dex，未命中则拉取并构建 coin 映射 |
| `Meta loadMetaCache(String dex)` | 指定 dex |
| `Meta refreshMetaCache(String dex)` | 失效并强制重载；会清空 coin/精度等内存映射后重建 |
| `Meta refreshMetaCache()` | 默认 dex |
| `void clearMetaCache()` | 清空 meta 缓存与相关映射 |
| `CacheStats getMetaCacheStats()` | Caffeine 统计（若启用 recordStats） |
| `SpotMeta loadSpotMetaCache()` | 加载 spotMeta 缓存 |
| `SpotMeta refreshSpotMetaCache()` | 强制刷新 spot，并重建 spot 映射 |
| `void clearSpotMetaCache()` | 清空 spot 缓存与 spot 派生映射 |
| `CacheStats getSpotMetaCacheStats()` | spot 缓存统计 |
| `void warmUpCache()` | 预热：默认 meta + spotMeta + 映射 |
| `void warmUpCache(List<String> dexList)` | 为列表中每个 dex 加载 meta，并加载 spotMeta |

## 订单簿与 K 线

| 方法 | 说明 |
|------|------|
| `L2Book l2Book(String coin, Integer nSigFigs, Integer mantissa)` | `type: l2Book`；`nSigFigs` 可选 2–5；`mantissa` 仅在 `nSigFigs==5` 时可为 1/2/5 |
| `L2Book l2Book(String coin)` | 全精度，无聚合参数 |
| `List<Candle> candleSnapshot(String coin, CandleInterval interval, Long startTime, Long endTime)` | `type: candleSnapshot`；**最多约 5000 根**；时间毫秒；`coin` 可为 `@107` 等形式 |
| `Candle candleSnapshotLatest(String coin, CandleInterval interval)` | 取最近两根范围内**最后一根**（可能为当前未完成 K 线，取决于服务端） |
| `List<Candle> candleSnapshotByCount(String coin, CandleInterval interval, int count)` | `count` 1–5000；按时间升序，可能截断保留最后 `count` 根 |
| `List<Candle> candleSnapshotByDays(String coin, CandleInterval interval, int days)` | `days > 0`，按自然日毫秒窗口查询 |
| `List<Candle> candleSnapshotByDate(String coin, CandleInterval interval, int year, int month, int day)` | **UTC** 当日 00:00:00–23:59:59.999 |
| `List<Candle> candleSnapshotCurrent(String coin, CandleInterval interval)` | 最近时间窗口内**最后一根**（通常为当前正在形成的 K 线） |

`CandleInterval`：`io.github.hyperliquid.sdk.model.info.CandleInterval` 枚举，含 `getCode()`、`toMillis()` 等。

## 用户与订单（查询）

| 方法 | 说明 |
|------|------|
| `List<OpenOrder> openOrders(String address)` | `type: openOrders`，默认 dex |
| `List<OpenOrder> openOrders(String address, String dex)` | 可指定 dex |
| `List<FrontendOpenOrder> frontendOpenOrders(String address)` | `type: frontendOpenOrders` |
| `List<FrontendOpenOrder> frontendOpenOrders(String address, String dex)` | 可指定 dex |
| `List<UserFill> userFills(String address)` | `type: userFills`，最多约 2000 条 |
| `List<UserFill> userFills(String address, Boolean aggregateByTime)` | 可选按时间聚合 |
| `List<UserFill> userFillsByTime(String address, Long startTime)` | 时间范围成交；`endTime`/`aggregateByTime` 为 null |
| `List<UserFill> userFillsByTime(String address, Long startTime, Long endTime)` | |
| `List<UserFill> userFillsByTime(String address, Long startTime, Boolean aggregateByTime)` | |
| `List<UserFill> userFillsByTime(String address, Long startTime, Long endTime, Boolean aggregateByTime)` | 单次最多约 2000；服务端仅保留约最近 10000 条可查窗口（与官方一致） |
| `UserFees userFees(String address)` | `type: userFees` |
| `List<FundingHistory> fundingHistory(String coin, long startMs, long endMs)` | 币种资金费率历史 |
| `List<FundingHistory> fundingHistory(String coin, long startMs, Long endMs)` | `endMs` 可 null |
| `JsonNode userFundingHistory(String address, long startMs, Long endMs)` | `type: userFunding`；`endMs` 可 null |
| `JsonNode userFundingHistory(String address, int coin, long startMs, long endMs)` | 使用 **默认盘 meta 的 universe 下标** 转为 `@" + id`；仅适用于该 universe 内的 perp asset id |
| `JsonNode userFundingHistory(String address, String coin, long startMs, long endMs)` | `coin` 可为名称或 `@id` 形式 |
| `JsonNode userNonFundingLedgerUpdates(String address, long startMs, long endMs)` | 非资金费账本 |
| `JsonNode userNonFundingLedgerUpdates(String address, long startMs, Long endMs)` | `endMs` 可 null |
| `JsonNode historicalOrders(String address, long startMs, long endMs)` | 历史订单 |
| `JsonNode historicalOrders(String address)` | 最新一批（服务端默认窗口） |
| `JsonNode userTwapSliceFills(String address, long startMs, long endMs)` | TWAP 成交切片 |
| `JsonNode userTwapSliceFills(String address)` | 最新（服务端默认窗口） |
| `OrderStatus orderStatus(String address, Long oid)` | `type: orderStatus` |
| `OrderStatus orderStatusByCloid(String address, Cloid cloid)` | 用 cloid 作为查询字段（与 Python `query_order_by_cloid` 对齐） |
| `OrderStatus orderStatusByCloid(String address, String cloid)` | 字符串解析为 `Cloid` |
| `ClearinghouseState clearinghouseState(String address)` | `type: clearinghouseState`，默认 dex |
| `ClearinghouseState clearinghouseState(String address, String dex)` | 可指定 dex |
| `ClearinghouseState userState(String address)` | 与 `clearinghouseState(address, null)` 相同 |
| `SpotClearinghouseState spotClearinghouseState(String user)` | `type: spotClearinghouseState` |
| `UserRateLimit userRateLimit(String address)` | `type: userRateLimit` |
| `JsonNode portfolio(String address)` | `type: portfolio` |
| `JsonNode userRole(String address)` | `type: userRole` |
| `JsonNode vaultDetails(String vaultAddress, String user)` | `type: vaultDetails`；`user` 可 null |
| `JsonNode spotDeployState(String address)` | `type: spotDeployState` |
| `JsonNode queryReferralState(String address)` | `type: referral` |
| `JsonNode querySubAccounts(String address)` | `type: subAccounts` |
| `JsonNode queryUserToMultiSigSigners(String address)` | `type: userToMultiSigSigners` |
| `JsonNode queryPerpDeployAuctionStatus()` | `type: perpDeployAuctionStatus` |
| `JsonNode querySpotDeployAuctionStatus(String address)` | **等价于** `spotDeployState(address)` |
| `@Deprecated JsonNode queryUserDexAbstractionState(String address)` | `type: userDexAbstraction`；建议改用 `userAbstraction` / Exchange 侧抽象设置 |
| `String userAbstraction(String user)` | `type: userAbstraction`，返回纯文本状态 |
| `JsonNode userVaultEquities(String address)` | `type: userVaultEquities` |
| `JsonNode extraAgents(String address)` | `type: extraAgents` |
| `JsonNode userStakingSummary(String address)` | `type: delegatorSummary` |
| `JsonNode userStakingDelegations(String address)` | `type: delegations` |
| `JsonNode userStakingRewards(String address)` | `type: delegatorRewards` |
| `JsonNode delegatorHistory(String user)` | `type: delegatorHistory` |
| `List<String> approvedBuilders(String user)` | `type: approvedBuilders` |
| `Long maxBuilderFee(String user, String builder)` | `type: maxBuilderFee`，返回数值（文本解析为 `Long`） |

## WebSocket（`skipWs == false` 时可用）

若构建客户端时 `skipWs(true)`，下列方法会抛 `HypeError("WebSocket disabled by skipWs")` 或静默 no-op（见各方法）。

**注意：** 订阅 builder-deployed 永续 DEX 符号（如 `xyz:SP500`）前，需通过 `HyperliquidClient.builder().perpDexs(List.of("xyz"))` 预加载该 DEX 元数据。

| 方法 | 说明 |
|------|------|
| `void subscribe(Subscription subscription, MessageCallback callback)` | 类型安全订阅；内部会 **remap** `coin`（现货别名、`dex:COIN` 等） |
| `ActiveSubscription subscribeWithHandle(Subscription subscription, MessageCallback callback)` | 同上，返回可单独取消的 ActiveSubscription |
| `void subscribe(JsonNode subscription, MessageCallback callback)` | 原始 JSON 订阅 |
| `ActiveSubscription subscribeWithHandle(JsonNode subscription, MessageCallback callback)` | |
| `Map<String, ActiveSubscription> getSubscriptions()` | 当前订阅快照 |
| `void unsubscribe(Subscription subscription)` | 取消订阅 |
| `void unsubscribe(JsonNode subscription)` | |
| `boolean unsubscribe(ActiveSubscription activeSub)` | 按 ActiveSubscription 取消 |
| `boolean unsubscribe(long subscriptionId)` | 按句柄内 id 取消 |
| `void closeWs()` | 关闭 WS |
| `WebsocketManager getWsManager()` | 底层管理器；`skipWs` 时可能为 `null` |
| `void addConnectionListener(ConnectionListener listener)` | 连接/重连/网络监听 |
| `void removeConnectionListener(ConnectionListener listener)` | |
| `void setNetworkCheckIntervalSeconds(int seconds)` | 断线后网络检测周期，默认约 5 秒 |
| `void setReconnectBackoffMs(long initialMs, long maxMs)` | 重连指数退避 |
| `void setNetworkProbeUrl(String url)` | 自定义网络探测 URL（默认用 API baseUrl） |
| `void setNetworkProbeDisabled(boolean disabled)` | `true` 时不做 HTTP 探测 |
| `void addCallbackErrorListener(CallbackErrorListener listener)` | 用户回调异常 |
| `void removeCallbackErrorListener(CallbackErrorListener listener)` | |

订阅实体类在 `io.github.hyperliquid.sdk.model.subscription` 包（如 `TradesSubscription`、`L2BookSubscription`、`CandleSubscription`、`BboSubscription` 等）。
