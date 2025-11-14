package io.github.hyperliquid.sdk.apis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.hyperliquid.sdk.model.info.*;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.HypeHttpClient;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Info 客户端，提供行情、订单簿、用户状态等查询。
 */
public class Info {

    private final boolean skipWs;

    private WebsocketManager wsManager;

    private final HypeHttpClient hypeHttpClient;

    private final Cache<String, Meta> metaCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /**
     * 构造 InfoClient 客户端。
     *
     * @param baseUrl        API 根地址
     * @param hypeHttpClient HTTP客户端实例
     * @param skipWs         是否跳过创建 WebSocket 连接（用于测试）
     */
    public Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs) {
        this.hypeHttpClient = hypeHttpClient;
        this.skipWs = skipWs;
        if (!skipWs) {
            this.wsManager = new WebsocketManager(baseUrl);
        }
    }

    /**
     * 将币种名称映射为资产 ID（根据 meta.universe）。
     *
     * @param coinName 币种名称（大小写不敏感）
     * @return 资产 ID（从 0 开始）
     * @throws HypeError 当名称无法映射时抛出
     */
    public Integer nameToAsset(String coinName) {
        String normalizedName = coinName.trim().toUpperCase();
        List<Meta.Universe> universe = loadMetaCache().getUniverse();
        for (int assetId = 0; assetId < universe.size(); assetId++) {
            Meta.Universe u = universe.get(assetId);
            if (u.getName().equalsIgnoreCase(normalizedName)) {
                return assetId;
            }
        }
        throw new HypeError("Unknown currency name:" + normalizedName);
    }

    /**
     * 发送 /info 请求的内部封装。
     *
     * @param payload 请求体对象（Map 或 POJO）
     * @return JSON 响应
     */
    public JsonNode postInfo(Object payload) {
        return hypeHttpClient.post("/info", payload);
    }

    /**
     * 查询所有中间价（allMids），类型化返回，可指定 perp dex 名称。
     *
     * @param dex perp dex 名称（可为空或空字符串）
     * @return 币种到中间价的映射
     */
    public Map<String, String> allMids(String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "allMids");
        if (dex != null) {
            payload.put("dex", dex);
        }
        JsonNode node = postInfo(payload);
        return JSONUtil.convertValue(node,
                TypeFactory.defaultInstance().constructMapType(Map.class, String.class, String.class));
    }

    /**
     * 查询所有中间价（allMids）。
     *
     * @return 币种到中间价的映射
     */
    public Map<String, String> allMids() {
        return allMids(null);
    }

    /**
     * 查询 perp 元数据（meta）。
     *
     * @return 类型化元数据对象 Meta
     */
    public Meta meta() {
        return meta(null);
    }

    /**
     * 获取/刷新本地缓存的 meta。
     *
     * @return 缓存中的 Meta
     */
    public Meta loadMetaCache() {
        return metaCache.get("meta", key -> meta());
    }

    /**
     * 根据币种名称获取 meta 中的 universe 元素。
     *
     * @param coinName 币种名称
     * @return 对应的 Universe 元素
     * @throws HypeError 当名称不存在时抛出
     */
    public Meta.Universe getMetaUniverse(String coinName) {
        Meta meta = loadMetaCache();
        List<Meta.Universe> universe = meta.getUniverse();
        for (Meta.Universe u : universe) {
            if (u.getName().equalsIgnoreCase(coinName)) {
                return u;
            }
        }
        throw new HypeError("Unknown currency name:" + coinName);
    }

    /**
     * 查询 perp 元数据（可指定 dex）。
     *
     * @param dex perp dex 名称（可为空）
     * @return 类型化元数据对象 Meta
     */
    public Meta meta(String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "meta");
        if (dex != null) {
            payload.put("dex", dex);
        }
        return JSONUtil.convertValue(postInfo(payload), Meta.class);
    }

    /**
     * 获取永续资产相关信息（包括标价、当前资金、未平仓合约等）
     */
    public JsonNode metaAndAssetCtxs() {
        Map<String, Object> payload = Map.of("type", "metaAndAssetCtxs");
        return postInfo(payload);
    }

    /**
     * 获取 perp 元数据与资产上下文（类型化返回）。
     *
     * @return 类型化模型 MetaAndAssetCtxs
     */
    public MetaAndAssetCtxs metaAndAssetCtxsTyped() {
        JsonNode node = metaAndAssetCtxs();
        return JSONUtil.convertValue(node, MetaAndAssetCtxs.class);
    }

    /**
     * 查询现货元数据（spotMeta）。
     *
     * @return 类型化模型 SpotMeta
     */
    public SpotMeta spotMeta() {
        Map<String, Object> payload = Map.of("type", "spotMeta");
        return JSONUtil.convertValue(postInfo(payload), SpotMeta.class);
    }

    /**
     * 查询现货元数据与资产上下文（spotMetaAndAssetCtxs）。
     *
     * @return JSON 响应
     */
    public JsonNode spotMetaAndAssetCtxs() {
        Map<String, Object> payload = Map.of("type", "spotMetaAndAssetCtxs");
        return postInfo(payload);
    }

    /**
     * L2 订单簿快照。
     * 可选聚合参数用于控制有效数字与尾数（仅当 nSigFigs 为 5 时允许设置 mantissa 为 1/2/5）。
     *
     * @param coin     币种名称
     * @param nSigFigs 聚合到指定有效数字（可选：2、3、4、5 或 null）
     * @param mantissa 尾数聚合（仅当 nSigFigs=5 时允许，取值 1/2/5）
     * @return 类型化模型 L2Book
     */
    public L2Book l2Book(String coin, Integer nSigFigs, Integer mantissa) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "l2Book");
        String effectiveCoin = coin;
        if (effectiveCoin != null && !effectiveCoin.startsWith("@")) {
            try {
                Integer assetId = nameToAsset(effectiveCoin);
                effectiveCoin = coinIdToInfoCoinString(assetId);
            } catch (Exception ignored) {
            }
        }
        payload.put("coin", effectiveCoin);
        if (nSigFigs != null) {
            payload.put("nSigFigs", nSigFigs);
        }
        if (mantissa != null) {
            payload.put("mantissa", mantissa);
        }
        return JSONUtil.convertValue(postInfo(payload), L2Book.class);
    }

    /**
     * L2 订单簿快照（默认全精度）。
     *
     * @param coin 币种名称
     * @return 类型化模型 L2Book
     */
    public L2Book l2Book(String coin) {
        return l2Book(coin, null, null);
    }

    /**
     * Candle snapshot
     * Only the most recent 5000 candles are available
     * <p>
     * Supported intervals: "1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "8h",
     * "12h", "1d", "3d", "1w", "1M"
     * K 线快照（类型化返回，支持以币种名称传参）。
     *
     * <p>
     * 某些环境下服务端可能要求请求体中 coin 字段为字符串（如 "BTC" 或 "@107"）。
     * 为提升兼容性，提供该重载方法，使用币种名称直接发起请求。
     * </p>
     *
     * @param coin      币种名称（如 "BTC"，或形如 "@107" 的内部标识）
     * @param interval  间隔字符串（如 "1m"、"15m"、"1h"、"1d" 等）
     * @param startTime 起始毫秒
     * @param endTime   结束毫秒
     * @return Candle 列表
     */
    public List<Candle> candleSnapshot(String coin, CandleInterval interval, Long startTime, Long endTime) {
        Map<String, Object> req = new LinkedHashMap<>();
        String effectiveCoin = coin;
        if (effectiveCoin != null && !effectiveCoin.startsWith("@")) {
            try {
                Integer assetId = nameToAsset(effectiveCoin);
                effectiveCoin = coinIdToInfoCoinString(assetId);
            } catch (Exception ignored) {
            }
        }
        req.put("coin", effectiveCoin);
        req.put("interval", interval.getCode());
        req.put("startTime", startTime);
        req.put("endTime", endTime);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "candleSnapshot");
        payload.put("req", req);
        return JSONUtil.toList(postInfo(payload), Candle.class);
    }

    /**
     * 获取最近一个间隔周期的最新 K 线。
     *
     * @param coin     币种名称
     * @param interval 间隔枚举
     * @return 最近一根 K 线；若无数据返回 null
     */
    public Candle candleSnapshotLatest(String coin, CandleInterval interval) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - interval.toMillis();
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);
        return !candles.isEmpty() ? candles.getLast() : null;
    }

    /**
     * 按数量获取最近的 K 线列表。
     * 仅提供最新的 5000 根 K 线。
     *
     * @param coin     币种名称
     * @param interval 间隔枚举
     * @param count    需要的数量（>0）
     * @return 最近的 K 线列表
     */
    public List<Candle> candleSnapshotByCount(String coin, CandleInterval interval, int count) {
        if (count <= 0) {
            throw new HypeError("count必须大于0");
        }
        long endTime = System.currentTimeMillis();
        long startTime = endTime - interval.toMillis() * count;
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);
        return candles.size() > count ? candles.subList(candles.size() - count, candles.size()) : candles;
    }

    /**
     * 查询用户未成交订单（默认 perp dex）。
     *
     * @param address 用户地址
     * @return 未成交订单列表
     */
    public List<OpenOrder> openOrders(String address) {
        return openOrders(address, null);
    }

    /**
     * 查询用户未成交订单（可指定 perp dex）。
     *
     * @param address 用户地址
     * @param dex     perp dex 名称（可为空）
     * @return 未成交订单列表
     */
    public List<OpenOrder> openOrders(String address, String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "openOrders");
        payload.put("user", address);
        if (dex != null) {
            payload.put("dex", dex);
        }
        return JSONUtil.toList(postInfo(payload), OpenOrder.class);
    }

    /**
     * 查询用户成交（按时间范围）。
     * 返回最多 2000 条；仅保留最近的 10000 条可用。
     *
     * @param address         用户地址
     * @param startTime       起始毫秒
     * @param endTime         结束毫秒（可选）
     * @param aggregateByTime 是否按时间聚合（可选）
     * @return 成交列表
     */
    public List<UserFill> userFillsByTime(String address, Long startTime, Long endTime, Boolean aggregateByTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFillsByTime");
        payload.put("user", address);
        payload.put("startTime", startTime);
        if (endTime != null) {
            payload.put("endTime", endTime);
        }
        if (aggregateByTime != null) {
            payload.put("aggregateByTime", aggregateByTime);
        }
        return JSONUtil.toList(postInfo(payload), UserFill.class);
    }

    public List<UserFill> userFillsByTime(String address, Long startTime) {
        return userFillsByTime(address, startTime, null, null);
    }

    public List<UserFill> userFillsByTime(String address, Long startTime, Long endTime) {
        return userFillsByTime(address, startTime, endTime, null);
    }

    public List<UserFill> userFillsByTime(String address, Long startTime, Boolean aggregateByTime) {
        return userFillsByTime(address, startTime, null, aggregateByTime);
    }

    /**
     * 查询用户费用（返现/手续费）。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode userFees(String address) {
        Map<String, Object> payload = Map.of("type", "userFees", "user", address);
        return postInfo(payload);
    }

    /**
     * 查询资金费率历史（按资产 ID）。
     *
     * @param coin    资产 ID
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return JSON 响应
     */
    public JsonNode fundingHistory(int coin, long startMs, long endMs) {
        return this.fundingHistory(this.coinIdToInfoCoinString(coin), startMs, endMs);
    }

    /**
     * 查询资金费率历史（按币种名称）。
     *
     * @param coin    币种名称或内部标识（如 "BTC" 或 "@107"）
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return JSON 响应
     */
    public JsonNode fundingHistory(String coin, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "fundingHistory");
        String effectiveCoin = coin;
        if (effectiveCoin != null && !effectiveCoin.startsWith("@")) {
            try {
                Integer assetId = nameToAsset(effectiveCoin);
                effectiveCoin = coinIdToInfoCoinString(assetId);
            } catch (Exception ignored) {
            }
        }
        payload.put("coin", effectiveCoin);
        payload.put("startTime", startMs);
        payload.put("endTime", endMs);
        return postInfo(payload);
    }

    /**
     * 查询用户资金费率历史（按资产 ID）。
     *
     * @param address 用户地址
     * @param coin    资产 ID
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return JSON 响应
     */
    public JsonNode userFundingHistory(String address, int coin, long startMs, long endMs) {
        return this.userFundingHistory(address, this.coinIdToInfoCoinString(coin), startMs, endMs);
    }

    /**
     * 将资产 ID 转换为 /info API coin 字段格式（形如 "@107"）。
     *
     * @param coinId 资产 ID
     * @return 形如 "@<id>" 的字符串
     * @throws HypeError 当 ID 不合法时抛出
     */
    private String coinIdToInfoCoinString(int coinId) {
        Meta meta = loadMetaCache();
        List<Meta.Universe> universe = meta.getUniverse();
        if (coinId < 0 || coinId >= universe.size()) {
            throw new HypeError("Unknown asset id:" + coinId);
        }
        return "@" + coinId;
    }

    /**
     * 查询用户资金费率历史（按币种名称）。
     *
     * @param address 用户地址
     * @param coin    币种名称或内部标识（如 "BTC" 或 "@107"）
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return JSON 响应
     */
    public JsonNode userFundingHistory(String address, String coin, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFunding");
        payload.put("user", address);
        payload.put("startTime", startMs);
        payload.put("endTime", endMs);
        return postInfo(payload);
    }

    public JsonNode userFundingHistory(String address, long startMs, Long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFunding");
        payload.put("user", address);
        payload.put("startTime", startMs);
        if (endMs != null) {
            payload.put("endTime", endMs);
        }
        return postInfo(payload);
    }

    /**
     * 用户非资金费率账本更新（不含 funding）。
     *
     * @param address 用户地址
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return JSON 响应
     */
    public JsonNode userNonFundingLedgerUpdates(String address, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userNonFundingLedgerUpdates");
        payload.put("user", address);
        payload.put("startTime", startMs);
        payload.put("endTime", endMs);
        return postInfo(payload);
    }

    /**
     * 历史订单查询。
     *
     * @param address 用户地址
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return JSON 响应
     */
    public JsonNode historicalOrders(String address, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "historicalOrders");
        payload.put("user", address);
        payload.put("startTime", startMs);
        payload.put("endTime", endMs);
        return postInfo(payload);
    }

    /**
     * 用户 TWAP 切片成交查询。
     *
     * @param address 用户地址
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return JSON 响应
     */
    public JsonNode userTwapSliceFills(String address, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userTwapSliceFills");
        payload.put("user", address);
        payload.put("startTime", startMs);
        payload.put("endTime", endMs);
        return postInfo(payload);
    }

    /**
     * 前端附加信息的未成交订单（frontendOpenOrders）。
     *
     * @param address 用户地址
     * @param dex     perp dex 名称（可为空）
     * @return 前端未成交订单列表
     */
    public List<FrontendOpenOrder> frontendOpenOrders(String address, String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "frontendOpenOrders");
        payload.put("user", address);
        if (dex != null) {
            payload.put("dex", dex);
        }
        return JSONUtil.toList(postInfo(payload), FrontendOpenOrder.class);
    }

    /**
     * 前端附加信息的未成交订单（默认 perp dex）。
     *
     * @param address 用户地址
     * @return 前端未成交订单列表
     */
    public List<FrontendOpenOrder> frontendOpenOrders(String address) {
        return frontendOpenOrders(address, null);
    }

    /**
     * 用户最近成交（最多 2000 条）。
     *
     * @param address         用户地址
     * @param aggregateByTime 是否按时间聚合（可选）
     * @return 成交列表
     */
    public List<UserFill> userFills(String address, Boolean aggregateByTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFills");
        payload.put("user", address);
        if (aggregateByTime != null) {
            payload.put("aggregateByTime", aggregateByTime);
        }
        return JSONUtil.toList(postInfo(payload), UserFill.class);
    }

    public List<UserFill> userFills(String address) {
        return userFills(address, null);
    }

    /**
     * 查询所有 perpetual dexs（perpDexs）。
     *
     * @return JSON 数组
     */
    public JsonNode perpDexs() {
        Map<String, Object> payload = Map.of("type", "perpDexs");
        return postInfo(payload);
    }

    /**
     * 查询所有 perpetual dexs（类型化返回）。
     * 元素可能为 null 或对象，使用 Map 接收以适配字段变动。
     *
     * @return perp dex 列表（元素为 Map 或 null）
     */
    public List<Map<String, Object>> perpDexsTyped() {
        JsonNode node = perpDexs();
        return JSONUtil.convertValue(node,
                TypeFactory.defaultInstance().constructCollectionType(List.class,
                        TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)));
    }

    /**
     * 永续清算所状态（用户账户摘要）。
     *
     * @param address 用户地址
     * @param dex     可选 perp dex 名称
     * @return 类型化模型 ClearinghouseState
     */
    public ClearinghouseState clearinghouseState(String address, String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "clearinghouseState");
        payload.put("user", address);
        if (dex != null && !dex.isEmpty()) {
            payload.put("dex", dex);
        }
        return JSONUtil.convertValue(postInfo(payload), ClearinghouseState.class);
    }

    /**
     * 永续清算所状态（默认 perp dex）。
     *
     * @param address 用户地址
     * @return 类型化模型 ClearinghouseState
     */
    public ClearinghouseState clearinghouseState(String address) {
        return clearinghouseState(address, null);
    }

    /**
     * 用户状态（同 clearinghouseState）。
     *
     * @param address 用户地址
     * @return 类型化模型 ClearinghouseState
     */
    public ClearinghouseState userState(String address) {
        return clearinghouseState(address, null);
    }

    /**
     * 获取用户的代币余额（现货清算所状态）。
     *
     * @param address 用户地址
     * @return 类型化模型 SpotClearinghouseState
     */
    public SpotClearinghouseState spotClearinghouseState(String address) {
        Map<String, Object> payload = Map.of("type", "spotClearinghouseState", "user", address);
        JsonNode node = postInfo(payload);
        return JSONUtil.convertValue(node, SpotClearinghouseState.class);
    }

    /**
     * 查询 Vault 详情。
     *
     * @param vaultAddress Vault 地址
     * @param user         用户地址（可选）
     * @return JSON 响应
     */
    public JsonNode vaultDetails(String vaultAddress, String user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "vaultDetails");
        payload.put("vaultAddress", vaultAddress);
        if (user != null) {
            payload.put("user", user);
        }
        return postInfo(payload);
    }

    /**
     * Spot Deploy Auction 状态。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode spotDeployState(String address) {
        Map<String, Object> payload = Map.of("type", "spotDeployState", "user", address);
        return postInfo(payload);
    }

    /**
     * 用户组合（portfolio）。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode portfolio(String address) {
        Map<String, Object> payload = Map.of("type", "portfolio", "user", address);
        return postInfo(payload);
    }

    /**
     * 用户仓位费率与等级（userRole）。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode userRole(String address) {
        Map<String, Object> payload = Map.of("type", "userRole", "user", address);
        return postInfo(payload);
    }

    /**
     * 用户速率限制（userRateLimit）。
     *
     * @param address 用户地址
     * @return 类型化模型 UserRateLimit
     */
    public UserRateLimit userRateLimit(String address) {
        Map<String, Object> payload = Map.of("type", "userRateLimit", "user", address);
        return JSONUtil.convertValue(postInfo(payload), UserRateLimit.class);
    }

    /**
     * 订单状态查询（按 OID）。
     *
     * @param address 用户地址
     * @param oid     订单 OID
     * @return 类型化模型 OrderStatus
     */
    public OrderStatus orderStatus(String address, Long oid) {
        Map<String, Object> payload = Map.of("type", "orderStatus", "user", address, "oid", oid);
        return JSONUtil.convertValue(postInfo(payload), OrderStatus.class);
    }

    /**
     * 查询推荐人状态（queryReferralState）。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode queryReferralState(String address) {
        Map<String, Object> payload = Map.of("type", "referral", "user", address);
        return postInfo(payload);
    }

    /**
     * 查询子账户列表。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode querySubAccounts(String address) {
        Map<String, Object> payload = Map.of("type", "subAccounts", "user", address);
        return postInfo(payload);
    }

    /**
     * 查询用户到多签签名者映射。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode queryUserToMultiSigSigners(String address) {
        Map<String, Object> payload = Map.of("type", "userToMultiSigSigners", "user", address);
        return postInfo(payload);
    }

    /**
     * 永续部署拍卖状态。
     *
     * @return JSON 响应
     */
    public JsonNode queryPerpDeployAuctionStatus() {
        Map<String, Object> payload = Map.of("type", "perpDeployAuctionStatus");
        return postInfo(payload);
    }

    /**
     * 现货部署拍卖状态。
     *
     * @return JSON 响应
     */
    public JsonNode querySpotDeployAuctionStatus() {
        // 该接口在 Python SDK 中对应 spotDeployState(user)，Java SDK 已提供
        // spotDeployState(address)
        // 保持方法以避免破坏现有调用，但服务器不支持无用户的 spotDeploy 查询，返回空对象以避免 4xx
        try {
            return JSONUtil.readTree("{}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse empty JSON", e);
        }
    }

    /**
     * 获取 Perp 市场状态（perpDexStatus）。
     *
     * @param dex perp dex 名称；空字符串代表第一个 perp dex
     * @return JSON 对象
     */
    public JsonNode perpDexStatus(String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "perpDexStatus");
        payload.put("dex", dex == null ? "" : dex);
        return postInfo(payload);
    }

    /**
     * 获取 Perp 市场状态（类型化返回）。
     *
     * @param dex perp dex 名称；空字符串代表第一个 perp dex
     * @return 类型化模型 PerpDexStatus
     */
    public PerpDexStatus perpDexStatusTyped(String dex) {
        JsonNode node = perpDexStatus(dex);
        return JSONUtil.convertValue(node, PerpDexStatus.class);
    }

    /**
     * 查询用户 DEX 抽象状态。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode queryUserDexAbstractionState(String address) {
        Map<String, Object> payload = Map.of("type", "userDexAbstraction", "user", address);
        return postInfo(payload);
    }

    /**
     * 用户仓库（vault）权益。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode userVaultEquities(String address) {
        Map<String, Object> payload = Map.of("type", "userVaultEquities", "user", address);
        return postInfo(payload);
    }

    /**
     * 用户的额外代理（extraAgents）。
     *
     * @param address 用户地址
     * @return JSON 响应
     */
    public JsonNode extraAgents(String address) {
        Map<String, Object> payload = Map.of("type", "extraAgents", "user", address);
        return postInfo(payload);
    }

    /**
     * 订阅 WebSocket。
     *
     * @param subscription 订阅对象
     * @param callback     消息回调
     */
    public void subscribe(JsonNode subscription, WebsocketManager.MessageCallback callback) {
        if (skipWs)
            throw new HypeError("WebSocket disabled by skipWs");
        wsManager.subscribe(subscription, callback);
    }

    /**
     * 取消订阅。
     *
     * @param subscription 订阅对象
     */
    public void unsubscribe(JsonNode subscription) {
        if (skipWs)
            return;
        wsManager.unsubscribe(subscription);
    }

    /**
     * 关闭 WebSocket 连接。
     */
    public void closeWs() {
        if (wsManager != null)
            wsManager.stop();
    }

    /**
     * 添加连接状态监听器（连接/断开/重连/网络状态变化）。
     *
     * @param listener 监听器实现
     */
    public void addConnectionListener(WebsocketManager.ConnectionListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.addConnectionListener(listener);
    }

    /**
     * 移除连接状态监听器。
     *
     * @param listener 监听器实现
     */
    public void removeConnectionListener(WebsocketManager.ConnectionListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.removeConnectionListener(listener);
    }

    /**
     * 设置最大重连尝试次数（默认 5）。
     *
     * @param max 最大次数（建议 5~10）
     */
    public void setMaxReconnectAttempts(int max) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setMaxReconnectAttempts(max);
    }

    /**
     * 设置网络监控的检查间隔（秒）。
     *
     * @param seconds 间隔秒数（默认 5，建议 3~10）
     */
    public void setNetworkCheckIntervalSeconds(int seconds) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setNetworkCheckIntervalSeconds(seconds);
    }

    /**
     * 设置重连指数退避参数。
     *
     * @param initialMs 初始重连延迟毫秒（建议 500~2000）
     * @param maxMs     最大重连延迟毫秒（建议不超过 5000~30000）
     */
    public void setReconnectBackoffMs(long initialMs, long maxMs) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setReconnectBackoffMs(initialMs, maxMs);
    }

    /**
     * 添加回调异常监听器。
     *
     * @param listener 监听器实现
     */
    public void addCallbackErrorListener(WebsocketManager.CallbackErrorListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.addCallbackErrorListener(listener);
    }

    /**
     * 移除回调异常监听器。
     *
     * @param listener 监听器实现
     */
    public void removeCallbackErrorListener(WebsocketManager.CallbackErrorListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.removeCallbackErrorListener(listener);
    }
}
