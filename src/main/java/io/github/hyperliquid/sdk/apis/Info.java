package io.github.hyperliquid.sdk.apis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.hyperliquid.sdk.config.CacheConfig;
import io.github.hyperliquid.sdk.model.info.*;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.subscription.Subscription;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.HypeHttpClient;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Info 客户端，提供行情、订单簿、用户状态等查询。
 */
public class Info {

    private final boolean skipWs;

    private WebsocketManager wsManager;

    private final HypeHttpClient hypeHttpClient;

    /**
     * Meta 缓存（支持多 DEX）
     * Key 格式："meta:default" 或 "meta:dexName"
     */
    private final Cache<String, Meta> metaCache;

    /**
     * SpotMeta 缓存
     */
    private final Cache<String, SpotMeta> spotMetaCache;

    /**
     * 币种名称到资产 ID 的映射缓存
     * Key: 币种名称（大写），Value: 资产 ID
     */
    private final Map<String, Integer> coinToAssetCache = new ConcurrentHashMap<>();

    /**
     * 资产 ID 到数量精度的映射缓存
     * Key: 资产 ID，Value: szDecimals
     */
    private final Map<Integer, Integer> assetToSzDecimalsCache = new ConcurrentHashMap<>();

    /**
     * 构造 InfoClient 客户端（使用默认缓存配置）。
     *
     * @param baseUrl        API 根地址
     * @param hypeHttpClient HTTP客户端实例
     * @param skipWs         是否跳过创建 WebSocket 连接（用于测试）
     */
    public Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs) {
        this(baseUrl, hypeHttpClient, skipWs, CacheConfig.defaultConfig());
    }

    /**
     * 构造 InfoClient 客户端（支持自定义缓存配置）。
     *
     * @param baseUrl        API 根地址
     * @param hypeHttpClient HTTP客户端实例
     * @param skipWs         是否跳过创建 WebSocket 连接（用于测试）
     * @param cacheConfig    缓存配置
     */
    public Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs, CacheConfig cacheConfig) {
        this.hypeHttpClient = hypeHttpClient;
        this.skipWs = skipWs;
        if (!skipWs) {
            this.wsManager = new WebsocketManager(baseUrl);
        }
        // 根据配置初始化缓存
        Caffeine<Object, Object> metaCacheBuilder = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getMetaCacheMaxSize())
                .expireAfterWrite(cacheConfig.getExpireAfterWriteMinutes(), TimeUnit.MINUTES);
        if (cacheConfig.isRecordStats()) {
            metaCacheBuilder.recordStats();
        }
        this.metaCache = metaCacheBuilder.build();

        Caffeine<Object, Object> spotMetaCacheBuilder = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getSpotMetaCacheMaxSize())
                .expireAfterWrite(cacheConfig.getExpireAfterWriteMinutes(), TimeUnit.MINUTES);
        if (cacheConfig.isRecordStats()) {
            spotMetaCacheBuilder.recordStats();
        }
        this.spotMetaCache = spotMetaCacheBuilder.build();
    }

    /**
     * 将币种名称映射为资产 ID（根据 meta.universe）。
     * <p>
     * 优化：先查询内存映射缓存，未命中时从 meta 缓存加载并构建映射。
     * </p>
     *
     * @param coinName 币种名称（大小写不敏感）
     * @return 资产 ID（从 0 开始）
     * @throws HypeError 当名称无法映射时抛出
     */
    public Integer nameToAsset(String coinName) {
        String normalizedName = coinName.trim().toUpperCase();

        // 优先从映射缓存查询
        Integer assetId = coinToAssetCache.get(normalizedName);
        if (assetId != null) {
            return assetId;
        }

        // 缓存未命中，从 meta 加载并构建映射
        Meta meta = loadMetaCache();
        buildCoinMappingCache(meta);

        // 再次查询
        assetId = coinToAssetCache.get(normalizedName);
        if (assetId == null) {
            throw new HypeError("Unknown currency name:" + normalizedName);
        }
        return assetId;
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
        return JSONUtil.convertValue(node, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, String.class));
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
     * 获取/刷新本地缓存的 meta（默认 dex）。
     *
     * @return 缓存中的 Meta
     */
    public Meta loadMetaCache() {
        return loadMetaCache(null);
    }

    /**
     * 获取/刷新本地缓存的 meta（支持指定 dex）。
     * <p>
     * 改进：支持多 DEX 缓存，缓存键格式为 "meta:default" 或 "meta:dexName"。
     * </p>
     *
     * @param dex perp dex 名称（null 或空字符串表示默认 dex）
     * @return 缓存中的 Meta
     */
    public Meta loadMetaCache(String dex) {
        String cacheKey = buildMetaCacheKey(dex);
        return metaCache.get(cacheKey, key -> {
            Meta meta = meta(dex);
            // 加载 meta 后自动构建币种映射缓存
            buildCoinMappingCache(meta);
            return meta;
        });
    }

    /**
     * 手动刷新 meta 缓存（强制重新加载）。
     *
     * @param dex perp dex 名称（null 或空字符串表示默认 dex）
     * @return 最新的 Meta
     */
    public Meta refreshMetaCache(String dex) {
        String cacheKey = buildMetaCacheKey(dex);
        metaCache.invalidate(cacheKey);
        // 清空币种映射缓存，强制重建
        coinToAssetCache.clear();
        assetToSzDecimalsCache.clear();
        return loadMetaCache(dex);
    }

    /**
     * 手动刷新 meta 缓存（默认 dex）。
     *
     * @return 最新的 Meta
     */
    public Meta refreshMetaCache() {
        return refreshMetaCache(null);
    }

    /**
     * 清空所有 meta 缓存。
     */
    public void clearMetaCache() {
        metaCache.invalidateAll();
        coinToAssetCache.clear();
        assetToSzDecimalsCache.clear();
    }

    /**
     * 获取 meta 缓存统计信息。
     *
     * @return 缓存统计对象
     */
    public CacheStats getMetaCacheStats() {
        return metaCache.stats();
    }

    /**
     * 构建 meta 缓存键。
     *
     * @param dex perp dex 名称
     * @return 缓存键字符串
     */
    private String buildMetaCacheKey(String dex) {
        return (dex == null || dex.isEmpty()) ? "meta:default" : "meta:" + dex;
    }

    /**
     * 从 Meta 构建币种映射缓存（内部方法）。
     * <p>
     * 构建两个映射表：
     * 1. coinToAssetCache: 币种名称（大写） -> 资产 ID
     * 2. assetToSzDecimalsCache: 资产 ID -> szDecimals
     * </p>
     *
     * @param meta Meta 对象
     */
    private void buildCoinMappingCache(Meta meta) {
        if (meta == null || meta.getUniverse() == null) {
            return;
        }
        List<Meta.Universe> universe = meta.getUniverse();
        for (int assetId = 0; assetId < universe.size(); assetId++) {
            Meta.Universe u = universe.get(assetId);
            if (u.getName() != null) {
                String coinName = u.getName().toUpperCase();
                coinToAssetCache.put(coinName, assetId);
                if (u.getSzDecimals() != null) {
                    assetToSzDecimalsCache.put(assetId, u.getSzDecimals());
                }
            }
        }
    }

    /**
     * 根据币种名称获取 meta 中的 universe 元素。
     * <p>
     * 优化：优先从映射缓存查询资产 ID，再获取对应的 Universe 元素。
     * </p>
     *
     * @param coinName 币种名称
     * @return 对应的 Universe 元素
     * @throws HypeError 当名称不存在时抛出
     */
    public Meta.Universe getMetaUniverse(String coinName) {
        // 通过 nameToAsset 获取资产 ID（会自动利用缓存）
        Integer assetId = nameToAsset(coinName);
        List<Meta.Universe> universe = loadMetaCache().getUniverse();
        if (assetId >= 0 && assetId < universe.size()) {
            return universe.get(assetId);
        }
        throw new HypeError("Unknown currency name:" + coinName);
    }

    /**
     * 根据币种名称快速获取 szDecimals（数量精度）。
     * <p>
     * 优化：优先从 assetToSzDecimalsCache 缓存查询，避免每次都获取完整 Universe 对象。
     * 该方法主要用于订单格式化场景（formatOrderSize/formatOrderPrice）。
     * </p>
     *
     * @param coinName 币种名称
     * @return szDecimals 数量精度
     * @throws HypeError 当名称不存在或精度未定义时抛出
     */
    public Integer getSzDecimals(String coinName) {
        // 通过 nameToAsset 获取资产 ID（会自动利用缓存）
        Integer assetId = nameToAsset(coinName);

        // 优先从精度缓存查询
        Integer szDecimals = assetToSzDecimalsCache.get(assetId);
        if (szDecimals != null) {
            return szDecimals;
        }

        // 缓存未命中，从 meta 加载
        Meta.Universe universe = getMetaUniverse(coinName);
        szDecimals = universe.getSzDecimals();

        if (szDecimals == null) {
            throw new HypeError("szDecimals not defined for coin: " + coinName);
        }

        // 更新缓存
        assetToSzDecimalsCache.put(assetId, szDecimals);
        return szDecimals;
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
     * 获取/刷新本地缓存的 spotMeta。
     *
     * @return 缓存中的 SpotMeta
     */
    public SpotMeta loadSpotMetaCache() {
        return spotMetaCache.get("spotMeta", key -> spotMeta());
    }

    /**
     * 手动刷新 spotMeta 缓存（强制重新加载）。
     *
     * @return 最新的 SpotMeta
     */
    public SpotMeta refreshSpotMetaCache() {
        spotMetaCache.invalidate("spotMeta");
        return loadSpotMetaCache();
    }

    /**
     * 清空所有 spotMeta 缓存。
     */
    public void clearSpotMetaCache() {
        spotMetaCache.invalidateAll();
    }

    /**
     * 获取 spotMeta 缓存统计信息。
     *
     * @return 缓存统计对象
     */
    public CacheStats getSpotMetaCacheStats() {
        return spotMetaCache.stats();
    }

    // ==================== 缓存预热（Warm Up） ====================

    /**
     * 预热缓存（应用启动时调用，提前加载常用数据）。
     * <p>
     * 预加载：
     * 1. 默认 dex 的 meta
     * 2. spotMeta
     * 3. 币种映射表
     * </p>
     */
    public void warmUpCache() {
        loadMetaCache();       // 预加载默认 meta
        loadSpotMetaCache();   // 预加载 spotMeta
    }

    /**
     * 预热缓存（支持指定 dex 列表）。
     *
     * @param dexList 需要预加载的 dex 名称列表（null 或空列表表示只加载默认 dex）
     */
    public void warmUpCache(List<String> dexList) {
        if (dexList == null || dexList.isEmpty()) {
            warmUpCache();
            return;
        }

        // 预加载指定 dex 的 meta
        for (String dex : dexList) {
            loadMetaCache(dex);
        }

        // 预加载 spotMeta
        loadSpotMetaCache();
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
        payload.put("coin", coin);
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
     * @throws HypeError 当参数不合法时抛出
     */
    public List<Candle> candleSnapshot(String coin, CandleInterval interval, Long startTime, Long endTime) {
        // 参数校验
        if (coin == null || coin.trim().isEmpty()) {
            throw new HypeError("Coin name cannot be null or empty");
        }
        if (interval == null) {
            throw new HypeError("Interval cannot be null");
        }
        if (startTime == null || startTime < 0) {
            throw new HypeError("Invalid start time: " + startTime);
        }
        if (endTime == null || endTime < 0) {
            throw new HypeError("Invalid end time: " + endTime);
        }
        if (endTime < startTime) {
            throw new HypeError("End time cannot be earlier than start time");
        }
        
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("coin", coin);
        req.put("interval", interval.getCode());
        req.put("startTime", startTime);
        req.put("endTime", endTime);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "candleSnapshot");
        payload.put("req", req);
        return JSONUtil.toList(postInfo(payload), Candle.class);
    }

    /**
     * 获取最近一根已完成的 K 线。
     * <p>
     * 查询最近 2 个周期的时间范围，确保至少获取到上一根已完成的 K 线。
     * 如果当前 K 线尚未完成，返回上一根；如果当前 K 线已完成，返回当前根。
     * </p>
     *
     * @param coin     币种名称
     * @param interval 间隔枚举
     * @return 最近一根已完成的 K 线；若无数据返回 null
     */
    public Candle candleSnapshotLatest(String coin, CandleInterval interval) {
        long endTime = System.currentTimeMillis();
        // 查询最近 2 个周期，确保获取到已完成的 K 线
        long startTime = endTime - (interval.toMillis() * 2);
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);
        return !candles.isEmpty() ? candles.getLast() : null;
    }

    /**
     * 按数量获取最近的 K 线列表。
     * <p>
     * 查询时会增加额外的缓冲时间（count + 2 个周期），确保获取足够数据后再截取。
     * 注意：Hyperliquid API 仅提供最新的 5000 根 K 线。
     * </p>
     *
     * @param coin     币种名称
     * @param interval 间隔枚举
     * @param count    需要的数量（>0，建议 ≤5000）
     * @return 最近的 K 线列表（按时间升序，最后一根是最新的）
     * @throws HypeError 当 count <= 0 或 count > 5000 时抛出
     */
    public List<Candle> candleSnapshotByCount(String coin, CandleInterval interval, int count) {
        if (count <= 0) {
            throw new HypeError("count must be greater than 0");
        }
        if (count > 5000) {
            throw new HypeError("count cannot exceed 5000 (API limit)");
        }
        
        long endTime = System.currentTimeMillis();
        // 增加 2 个周期的缓冲时间，确保数据完整性
        long startTime = endTime - (interval.toMillis() * (count + 2));
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);
        
        // 如果返回的数据多于请求数量，截取最后 count 根
        if (candles.size() > count) {
            return candles.subList(candles.size() - count, candles.size());
        }
        return candles;
    }

    /**
     * 获取最近 N 天的 K 线数据。
     * <p>
     * 根据指定的周期和天数计算时间范围，查询该时间段内的所有 K 线。
     * 例如：查询最近 7 天的 1 小时 K 线，将返回约 168 根 K 线。
     * </p>
     *
     * @param coin     币种名称
     * @param interval 间隔枚举
     * @param days     天数（>0，建议 ≤30）
     * @return K 线列表（按时间升序）
     * @throws HypeError 当 days <= 0 时抛出
     */
    public List<Candle> candleSnapshotByDays(String coin, CandleInterval interval, int days) {
        if (days <= 0) {
            throw new HypeError("days must be greater than 0");
        }
        
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24 * 60 * 60 * 1000L);  // days * 毫秒数
        return candleSnapshot(coin, interval, startTime, endTime);
    }

    /**
     * 获取指定日期（UTC 时区）的所有 K 线数据。
     * <p>
     * 查询从指定日期 00:00:00 到 23:59:59 之间的所有 K 线。
     * 注意：时间基于 UTC 时区，如需其他时区请自行转换。
     * </p>
     *
     * @param coin     币种名称
     * @param interval 间隔枚举
     * @param year     年份（如 2024）
     * @param month    月份（1-12）
     * @param day      日期（1-31）
     * @return K 线列表（按时间升序）
     * @throws HypeError 当日期参数不合法时抛出
     */
    public List<Candle> candleSnapshotByDate(String coin, CandleInterval interval, int year, int month, int day) {
        if (year < 2000 || year > 2100) {
            throw new HypeError("Invalid year: " + year);
        }
        if (month < 1 || month > 12) {
            throw new HypeError("Invalid month: " + month);
        }
        if (day < 1 || day > 31) {
            throw new HypeError("Invalid day: " + day);
        }
        
        // 构造 UTC 时区的起始和结束时间
        long startTime = java.time.LocalDate.of(year, month, day)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        
        long endTime = java.time.LocalDate.of(year, month, day)
                .atTime(23, 59, 59, 999_999_999)
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
        
        return candleSnapshot(coin, interval, startTime, endTime);
    }

    /**
     * 获取当前正在生成的 K 线（未完成的 K 线）。
     * <p>
     * 查询当前周期的 K 线数据，该 K 线可能尚未完成（仍在实时更新）。
     * 例如：如果是 1 小时 K 线，当前时间 14:35，则返回 14:00-15:00 这根正在生成的 K 线。
     * </p>
     *
     * @param coin     币种名称
     * @param interval 间隔枚举
     * @return 当前正在生成的 K 线；若无数据返回 null
     */
    public Candle candleSnapshotCurrent(String coin, CandleInterval interval) {
        long endTime = System.currentTimeMillis();
        // 查询当前周期和上一个周期，确保能获取到数据
        long startTime = endTime - (interval.toMillis() * 2);
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);
        
        // 返回最后一根（当前正在生成的 K 线）
        return !candles.isEmpty() ? candles.getLast() : null;
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
     * 查询资金费率历史（按币种名称）。
     *
     * @param coin    币种名称（如 "BTC"）
     * @param startMs 起始毫秒
     * @param endMs   结束毫秒
     * @return List<FundingHistory> 响应
     */
    public List<FundingHistory> fundingHistory(String coin, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "fundingHistory");
        payload.put("coin", coin);
        payload.put("startTime", startMs);
        payload.put("endTime", endMs);
        return JSONUtil.toList(postInfo(payload), FundingHistory.class);
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
     * 订单状态查询（按 Cloid，与 Python query_order_by_cloid 对齐）。
     *
     * @param address 用户地址
     * @param cloid   客户端订单 ID
     * @return 类型化模型 OrderStatus
     */
    public OrderStatus orderStatusByCloid(String address, Cloid cloid) {
        if (cloid == null) {
            throw new HypeError("cloid cannot be null");
        }
        Map<String, Object> payload = Map.of("type", "orderStatus", "user", address, "oid", cloid.getRaw());
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
     * 订阅 WebSocket（类型安全版本，使用 Subscription 实体类）。
     * <p>
     * 推荐使用此方法，提供编译期类型检查和更好的代码可读性。
     * </p>
     *
     * @param subscription 订阅对象（Subscription 实体类）
     * @param callback     消息回调
     */
    public void subscribe(Subscription subscription, WebsocketManager.MessageCallback callback) {
        if (skipWs)
            throw new HypeError("WebSocket disabled by skipWs");
        wsManager.subscribe(subscription, callback);
    }

    /**
     * 订阅 WebSocket（兼容版本，使用 JsonNode）。
     * <p>
     * 为了更好的类型安全性，建议使用 {@link #subscribe(Subscription, WebsocketManager.MessageCallback)} 方法。
     * </p>
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
     * 取消订阅（类型安全版本，使用 Subscription 实体类）。
     *
     * @param subscription 订阅对象（Subscription 实体类）
     */
    public void unsubscribe(Subscription subscription) {
        if (skipWs)
            return;
        wsManager.unsubscribe(subscription);
    }

    /**
     * 取消订阅（兼容版本，使用 JsonNode）。
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
     * 设置自定义网络探测 URL。
     * <p>
     * 默认情况下，WebSocket 管理器使用 API baseUrl 进行网络可用性探测。
     * 在某些企业环境或特殊网络配置下，可能需要指定专门的探测地址。
     * </p>
     *
     * @param url 自定义网络探测 URL（如 "https://www.google.com"）
     */
    public void setNetworkProbeUrl(String url) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setNetworkProbeUrl(url);
    }

    /**
     * 启用或禁用网络探测功能。
     * <p>
     * 网络探测用于在 WebSocket 断线时周期性检查网络可用性，当网络恢复时自动触发重连。
     * 在某些场景下（如始终可用的内网环境），可以禁用探测以减少不必要的 HTTP 请求。
     * </p>
     *
     * @param disabled true=禁用网络探测，false=启用网络探测（默认启用）
     */
    public void setNetworkProbeDisabled(boolean disabled) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setNetworkProbeDisabled(disabled);
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

    // ==================== 质押功能模块（Staking） ====================

    /**
     * 查询用户质押摘要（delegatorSummary）。
     * <p>
     * POST /info
     * </p>
     *
     * @param address 用户地址（42 位十六进制格式）
     * @return JSON 响应，包含：
     * <ul>
     * <li>delegated - 已委托数量（float string）</li>
     * <li>undelegated - 未委托数量（float string）</li>
     * <li>totalPendingWithdrawal - 总待提取数量（float string）</li>
     * <li>nPendingWithdrawals - 待提取笔数（int）</li>
     * </ul>
     */
    public JsonNode userStakingSummary(String address) {
        Map<String, Object> payload = Map.of(
                "type", "delegatorSummary",
                "user", address
        );
        return postInfo(payload);
    }

    /**
     * 查询用户质押委托详情（delegations）。
     * <p>
     * POST /info
     * </p>
     *
     * @param address 用户地址（42 位十六进制格式）
     * @return JSON 响应数组，每个元素包含：
     * <ul>
     * <li>validator - 验证者地址（string）</li>
     * <li>amount - 委托数量（float string）</li>
     * <li>lockedUntilTimestamp - 锁定至时间戳（int）</li>
     * </ul>
     */
    public JsonNode userStakingDelegations(String address) {
        Map<String, Object> payload = Map.of(
                "type", "delegations",
                "user", address
        );
        return postInfo(payload);
    }

    /**
     * 查询用户历史质押奖励（delegatorRewards）。
     * <p>
     * POST /info
     * </p>
     *
     * @param address 用户地址（42 位十六进制格式）
     * @return JSON 响应数组，每个元素包含：
     * <ul>
     * <li>time - 时间戳（int）</li>
     * <li>source - 奖励来源（string）</li>
     * <li>totalAmount - 总奖励数量（float string）</li>
     * </ul>
     */
    public JsonNode userStakingRewards(String address) {
        Map<String, Object> payload = Map.of(
                "type", "delegatorRewards",
                "user", address
        );
        return postInfo(payload);
    }

    /**
     * 查询委托历史记录（delegatorHistory）。
     * <p>
     * POST /info
     * </p>
     *
     * @param user 用户地址（42 位十六进制格式）
     * @return JSON 响应，包含委托和取消委托事件的详细历史记录，包括时间戳、交易哈希及详细 delta 信息
     */
    public JsonNode delegatorHistory(String user) {
        Map<String, Object> payload = Map.of(
                "type", "delegatorHistory",
                "user", user
        );
        return postInfo(payload);
    }
}
