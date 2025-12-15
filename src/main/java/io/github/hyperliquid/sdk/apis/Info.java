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
 * Info client for the Hyperliquid SDK, providing access to market data, order
 * books, user status, and other queries.
 * <p>
 * This class offers comprehensive functionality for retrieving data from the
 * Hyperliquid exchange, including:
 * </p>
 * <ul>
 * <li>Market data (allMids, meta, spotMeta)</li>
 * <li>Order book snapshots (L2 order book)</li>
 * <li>Candlestick data with various intervals</li>
 * <li>User-specific information (open orders, fills, account state)</li>
 * <li>Staking and delegation details</li>
 * <li>WebSocket subscription management</li>
 * </ul>
 */
public class Info {

    /**
     * Flag to indicate whether WebSocket connection should be skipped.
     * When set to true, WebSocket-related functionalities will be disabled.
     */
    private final boolean skipWs;

    /**
     * WebSocket manager for handling real-time data subscriptions.
     * This is null if skipWs is true.
     */
    private WebsocketManager wsManager;

    /**
     * HTTP client used for making REST API requests.
     */
    private final HypeHttpClient hypeHttpClient;

    /**
     * Meta cache (supports multiple DEX)
     * Key format: "meta:default" or "meta:dexName"
     */
    private final Cache<String, Meta> metaCache;

    /**
     * SpotMeta cache for storing spot market metadata.
     */
    private final Cache<String, SpotMeta> spotMetaCache;

    /**
     * AllMids cache for storing all market IDs.
     **/
    private final Cache<String, Map<String, String>> allMidsCache;

    /**
     * Coin name to asset ID mapping cache
     * Key: Coin name (uppercase), Value: Asset ID
     */
    private final Map<String, Integer> coinToAssetCache = new ConcurrentHashMap<>();

    /**
     * Asset ID to quantity precision mapping cache
     * Key: Asset ID, Value: szDecimals
     */
    private final Map<Integer, Integer> assetToSzDecimalsCache = new ConcurrentHashMap<>();

    /**
     * Construct InfoClient client (using default cache configuration).
     *
     * @param baseUrl        API root URL
     * @param hypeHttpClient HTTP client instance
     * @param skipWs         Whether to skip creating WebSocket connection (for
     *                       testing)
     */
    public Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs) {
        this(baseUrl, hypeHttpClient, skipWs, CacheConfig.defaultConfig());
    }

    /**
     * Construct InfoClient client (support custom cache configuration).
     *
     * @param baseUrl        API root URL
     * @param hypeHttpClient HTTP client instance
     * @param skipWs         Whether to skip creating WebSocket connection (for
     *                       testing)
     * @param cacheConfig    Cache configuration
     */
    public Info(String baseUrl, HypeHttpClient hypeHttpClient, boolean skipWs, CacheConfig cacheConfig) {
        this.hypeHttpClient = hypeHttpClient;
        this.skipWs = skipWs;
        if (!skipWs) {
            this.wsManager = new WebsocketManager(baseUrl);
        }
        // Initialize caches according to the configuration
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

        this.allMidsCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).build();
    }

    /**
     * Map coin name to asset ID (based on meta.universe).
     * <p>
     * Optimization: Query memory mapping cache first, load from meta cache and
     * build mapping if not hit.
     * </p>
     *
     * @param coinName Coin name (case insensitive)
     * @return Asset ID (starting from 0)
     * @throws HypeError Thrown when name cannot be mapped
     */
    public Integer nameToAsset(String coinName) {
        String normalizedName = coinName.trim().toUpperCase();

        // Prefer querying from the mapping cache first
        Integer assetId = coinToAssetCache.get(normalizedName);
        if (assetId != null) {
            return assetId;
        }

        // Cache miss; load from meta and build mapping
        Meta meta = loadMetaCache();
        buildCoinMappingCache(meta);

        // Query again
        assetId = coinToAssetCache.get(normalizedName);
        if (assetId == null) {
            throw new HypeError("Unknown currency name:" + normalizedName);
        }
        return assetId;
    }

    /**
     * Internal wrapper for sending /info requests.
     * <p>
     * This method is used internally by other methods in this class to send
     * requests
     * to the /info endpoint of the Hyperliquid API.
     * </p>
     *
     * @param payload Request body object (Map or POJO)
     * @return JSON response from the API
     */
    public JsonNode postInfo(Object payload) {
        return hypeHttpClient.post("/info", payload);
    }

    /**
     * Query all mid prices (allMids), typed return, can specify perp dex name.
     *
     * @param dex Perp dex name (can be empty or null)
     * @return Coin to mid price mapping
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
     * Query all mid prices (allMids).
     *
     * @return Coin to mid price mapping
     */
    public Map<String, String> allMids() {
        return allMids(null);
    }

    public Map<String, String> getCachedAllMids(String dex) {
        String cacheKey = buildAllMidsCacheKey(dex);
        return allMidsCache.get(cacheKey, key -> allMids(dex));
    }

    public Map<String, String> getCachedAllMids() {
        return getCachedAllMids(null);
    }

    /**
     * Query perp metadata (meta).
     *
     * @return Typed metadata object Meta
     */
    public Meta meta() {
        return meta(null);
    }

    /**
     * Get/refresh locally cached meta (default dex).
     *
     * @return Cached Meta
     */
    public Meta loadMetaCache() {
        return loadMetaCache(null);
    }

    /**
     * Get/refresh locally cached meta (support specifying dex).
     * <p>
     * This method retrieves metadata from the cache, loading it from the API if not
     * present.
     * It supports multiple DEX caches using keys in the format "meta:default" or
     * "meta:dexName".
     * After loading the meta data, it automatically builds the coin mapping cache.
     * </p>
     *
     * @param dex Perp dex name (null or empty string means default dex)
     * @return Cached Meta
     */
    public Meta loadMetaCache(String dex) {
        String cacheKey = buildMetaCacheKey(dex);
        return metaCache.get(cacheKey, key -> {
            Meta meta = meta(dex);
            // Automatically build coin mapping cache after loading meta
            buildCoinMappingCache(meta);
            return meta;
        });
    }

    /**
     * Manually refresh meta cache (force reload).
     *
     * @param dex Perp dex name (null or empty string means default dex)
     * @return Latest Meta
     */
    public Meta refreshMetaCache(String dex) {
        String cacheKey = buildMetaCacheKey(dex);
        metaCache.invalidate(cacheKey);
        // Clear coin mapping cache to force rebuild
        coinToAssetCache.clear();
        assetToSzDecimalsCache.clear();
        return loadMetaCache(dex);
    }

    /**
     * Manually refresh meta cache (default dex).
     *
     * @return Latest Meta
     */
    public Meta refreshMetaCache() {
        return refreshMetaCache(null);
    }

    /**
     * Clear all meta caches.
     */
    public void clearMetaCache() {
        metaCache.invalidateAll();
        coinToAssetCache.clear();
        assetToSzDecimalsCache.clear();
    }

    /**
     * Get meta cache statistics.
     *
     * @return Cache statistics object
     */
    public CacheStats getMetaCacheStats() {
        return metaCache.stats();
    }

    /**
     * Build meta cache key based on DEX name.
     * <p>
     * This helper method generates cache keys for meta data storage.
     * </p>
     *
     * @param dex Perp DEX name (can be null or empty for default DEX)
     * @return Cache key string ("meta:default" for default DEX, "meta:{dex}" for
     * named DEX)
     */
    private String buildMetaCacheKey(String dex) {
        return (dex == null || dex.isEmpty()) ? "meta:default" : "meta:" + dex;
    }

    private String buildAllMidsCacheKey(String dex) {
        return (dex == null || dex.isEmpty()) ? "allMids:default" : "allMids:" + dex;
    }

    /**
     * Build coin mapping cache from Meta (internal method).
     * <p>
     * This method constructs two mapping tables to optimize data retrieval:
     * </p>
     * <ul>
     * <li>coinToAssetCache: Maps coin names (uppercase) to their corresponding
     * asset IDs</li>
     * <li>assetToSzDecimalsCache: Maps asset IDs to their quantity precision
     * (szDecimals)</li>
     * </ul>
     * <p>
     * The mappings are built by iterating through the universe list in the Meta
     * object.
     * For each universe element with a valid name, it creates entries in both
     * caches.
     * </p>
     *
     * @param meta Meta object containing universe data
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
     * Get universe element from meta by coin name.
     * <p>
     * Optimization: Query asset ID from mapping cache first, then get corresponding
     * Universe element.
     * </p>
     *
     * @param coinName Coin name
     * @return Corresponding Universe element
     * @throws HypeError Thrown when name does not exist
     */
    public Meta.Universe getMetaUniverse(String coinName) {
        // Get asset ID via nameToAsset (automatically uses cache)
        Integer assetId = nameToAsset(coinName);
        List<Meta.Universe> universe = loadMetaCache().getUniverse();
        if (assetId >= 0 && assetId < universe.size()) {
            return universe.get(assetId);
        }
        throw new HypeError("Unknown currency name:" + coinName);
    }

    /**
     * Quickly get szDecimals (quantity precision) by coin name.
     * <p>
     * Optimization: Query from assetToSzDecimalsCache cache first to avoid getting
     * complete Universe object every time.
     * This method is mainly used for order formatting scenarios
     * (formatOrderSize/formatOrderPrice).
     * </p>
     *
     * @param coinName Coin name
     * @return szDecimals quantity precision
     * @throws HypeError Thrown when name does not exist or precision is not defined
     *                   for the coin
     */
    public Integer getSzDecimals(String coinName) {
        // Get asset ID via nameToAsset (automatically uses cache)
        Integer assetId = nameToAsset(coinName);
        // Prefer querying from the precision cache first
        Integer szDecimals = assetToSzDecimalsCache.get(assetId);
        if (szDecimals != null) {
            return szDecimals;
        }
        // Cache miss; load from meta
        Meta.Universe universe = getMetaUniverse(coinName);
        szDecimals = universe.getSzDecimals();

        if (szDecimals == null) {
            throw new HypeError("szDecimals not defined for coin: " + coinName);
        }
        // Update cache
        assetToSzDecimalsCache.put(assetId, szDecimals);
        return szDecimals;
    }

    /**
     * Query perp metadata (can specify dex).
     *
     * @param dex Perp dex name (can be empty)
     * @return Typed metadata object Meta
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
     * Get perpetual asset related information (including pricing, current funding,
     * open contracts, etc.)
     */
    public JsonNode metaAndAssetCtxs() {
        Map<String, Object> payload = Map.of("type", "metaAndAssetCtxs");
        return postInfo(payload);
    }

    /**
     * Get perp metadata and asset context (typed return).
     *
     * @return Typed model MetaAndAssetCtxs
     */
    public MetaAndAssetCtxs metaAndAssetCtxsTyped() {
        JsonNode node = metaAndAssetCtxs();
        return JSONUtil.convertValue(node, MetaAndAssetCtxs.class);
    }

    /**
     * Query spot metadata (spotMeta).
     *
     * @return Typed model SpotMeta
     */
    public SpotMeta spotMeta() {
        Map<String, Object> payload = Map.of("type", "spotMeta");
        return JSONUtil.convertValue(postInfo(payload), SpotMeta.class);
    }

    /**
     * Get/refresh locally cached spotMeta.
     *
     * @return Cached SpotMeta
     */
    public SpotMeta loadSpotMetaCache() {
        return spotMetaCache.get("spotMeta", key -> spotMeta());
    }

    /**
     * Manually refresh spotMeta cache (force reload).
     *
     * @return Latest SpotMeta
     */
    public SpotMeta refreshSpotMetaCache() {
        spotMetaCache.invalidate("spotMeta");
        return loadSpotMetaCache();
    }

    /**
     * Clear all spotMeta caches.
     */
    public void clearSpotMetaCache() {
        spotMetaCache.invalidateAll();
    }

    /**
     * Get spotMeta cache statistics.
     *
     * @return Cache statistics object
     */
    public CacheStats getSpotMetaCacheStats() {
        return spotMetaCache.stats();
    }

    /**
     * Warm up cache (call at application startup to preload commonly used data).
     * <p>
     * Preload:
     * 1. Default dex meta
     * 2. spotMeta
     * 3. Coin mapping table
     * </p>
     */
    public void warmUpCache() {
        loadMetaCache();
        // Preload default meta
        loadSpotMetaCache();
        // Preload spotMeta
    }

    /**
     * Warm up cache (support specifying dex list).
     *
     * @param dexList List of dex names to preload (null or empty list means only
     *                load default dex)
     */
    public void warmUpCache(List<String> dexList) {
        if (dexList == null || dexList.isEmpty()) {
            warmUpCache();
            return;
        }

        // Preload meta for specified dex
        for (String dex : dexList) {
            loadMetaCache(dex);
        }

        // Preload spotMeta
        loadSpotMetaCache();
    }

    /**
     * Query spot metadata and asset context (spotMetaAndAssetCtxs).
     *
     * @return JSON response
     */
    public JsonNode spotMetaAndAssetCtxs() {
        Map<String, Object> payload = Map.of("type", "spotMetaAndAssetCtxs");
        return postInfo(payload);
    }

    /**
     * L2 order book snapshot.
     * Optional aggregation parameters for controlling significant digits and
     * mantissa (mantissa can only be set to 1/2/5 when nSigFigs is 5).
     *
     * @param coin     Coin name
     * @param nSigFigs Aggregate to specified significant digits (optional: 2, 3, 4,
     *                 5 or null)
     * @param mantissa Mantissa aggregation (only allowed when nSigFigs=5, values
     *                 1/2/5)
     * @return Typed model L2Book
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
     * L2 order book snapshot (default full precision).
     *
     * @param coin Coin name
     * @return Typed model L2Book
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
     * Candlestick snapshot (typed return, supports passing coin name).
     *
     * <p>
     * In some environments, the server may require the coin field in the request
     * body to be a string (e.g., "BTC" or "@107").
     * To improve compatibility, this overload method is provided to make requests
     * directly using coin names.
     * </p>
     *
     * @param coin      Coin name (e.g., "BTC", or internal identifier like "@107")
     * @param interval  Interval string (e.g., "1m", "15m", "1h", "1d", etc.)
     * @param startTime Start milliseconds
     * @param endTime   End milliseconds
     * @return Candle list
     * @throws HypeError Thrown when parameters are invalid
     */
    public List<Candle> candleSnapshot(String coin, CandleInterval interval, Long startTime, Long endTime) {
        // Parameter validation
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
     * Get the most recent completed candlestick.
     * <p>
     * Query the time range of the last 2 periods to ensure at least the previous
     * completed candlestick is obtained.
     * If the current candlestick is not yet completed, return the previous one; if
     * the current candlestick is completed, return the current one.
     * </p>
     *
     * @param coin     Coin name
     * @param interval Interval enum
     * @return The most recent completed candlestick; returns null if no data
     */
    public Candle candleSnapshotLatest(String coin, CandleInterval interval) {
        long endTime = System.currentTimeMillis();
        // Query the last two periods to ensure a completed candlestick is obtained
        long startTime = endTime - (interval.toMillis() * 2);
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);
        return !candles.isEmpty() ? candles.getLast() : null;
    }

    /**
     * Get recent candlestick list by quantity.
     * <p>
     * Additional buffer time (count + 2 periods) is added during query to ensure
     * sufficient data is obtained before truncation.
     * Note: Hyperliquid API only provides the latest 5000 candlesticks.
     * </p>
     *
     * @param coin     Coin name
     * @param interval Interval enum
     * @param count    Required quantity (>0, recommended ≤5000)
     * @return Recent candlestick list (in ascending time order, last one is the
     * newest)
     * @throws HypeError Thrown when count is less than or equal to 0, or greater
     *                   than 5000
     */
    public List<Candle> candleSnapshotByCount(String coin, CandleInterval interval, int count) {
        if (count <= 0) {
            throw new HypeError("count must be greater than 0");
        }
        if (count > 5000) {
            throw new HypeError("count cannot exceed 5000 (API limit)");
        }

        long endTime = System.currentTimeMillis();
        // Add a buffer of 2 periods to ensure data integrity
        long startTime = endTime - (interval.toMillis() * (count + 2));
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);

        // If the returned data exceeds the requested quantity, keep the last 'count'
        // entries
        if (candles.size() > count) {
            return candles.subList(candles.size() - count, candles.size());
        }
        return candles;
    }

    /**
     * Get candlestick data for the last N days.
     * <p>
     * Calculate the time range based on the specified period and number of days,
     * and query all candlesticks within that time period.
     * For example: Querying 1-hour candlesticks for the last 7 days will return
     * approximately 168 candlesticks.
     * </p>
     *
     * @param coin     Coin name
     * @param interval Interval enum
     * @param days     Number of days (>0, recommended ≤30)
     * @return Candlestick list (in ascending time order)
     * @throws HypeError Thrown when days <= 0
     */
    public List<Candle> candleSnapshotByDays(String coin, CandleInterval interval, int days) {
        if (days <= 0) {
            throw new HypeError("days must be greater than 0");
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24 * 60 * 60 * 1000L);
        // days in milliseconds
        return candleSnapshot(coin, interval, startTime, endTime);
    }

    /**
     * Get all candlestick data for a specified date (UTC timezone).
     * <p>
     * Query all candlesticks between the specified date 00:00:00 and 23:59:59.
     * </p>
     * <p>
     * NOTE: Time is based on UTC timezone. If you need to use a different timezone,
     * you should convert the year/month/day parameters accordingly before calling
     * this method.
     * </p>
     *
     * @param coin     Coin name
     * @param interval Interval enum
     * @param year     Year (e.g., 2024)
     * @param month    Month (1-12)
     * @param day      Day (1-31)
     * @return Candlestick list (in ascending time order)
     * @throws HypeError Thrown when date parameters are invalid
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

        // Construct start and end times in the UTC timezone
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
     * Get the current candlestick being generated (incomplete candlestick).
     * <p>
     * Query candlestick data for the current period, which may not yet be completed
     * (still being updated in real-time).
     * For example: If it's a 1-hour candlestick and the current time is 14:35, it
     * returns the 14:00-15:00 candlestick that is currently being generated.
     * </p>
     *
     * @param coin     Coin name
     * @param interval Interval enum
     * @return The current candlestick being generated; returns null if no data
     */
    public Candle candleSnapshotCurrent(String coin, CandleInterval interval) {
        long endTime = System.currentTimeMillis();
        // Query the current and previous periods to ensure data availability
        long startTime = endTime - (interval.toMillis() * 2);
        List<Candle> candles = candleSnapshot(coin, interval, startTime, endTime);

        // Return the last candlestick (currently being generated)
        return !candles.isEmpty() ? candles.getLast() : null;
    }

    /**
     * Query user's unfilled orders (default perp dex).
     *
     * @param address User address
     * @return Unfilled order list
     */
    public List<OpenOrder> openOrders(String address) {
        return openOrders(address, null);
    }

    /**
     * Query user's unfilled orders (can specify perp dex).
     *
     * @param address User address
     * @param dex     Perp dex name (can be empty)
     * @return Unfilled order list
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
     * Query user fills (by time range).
     * Returns up to 2000 entries; only the latest 10000 entries are available.
     *
     * @param address         User address
     * @param startTime       Start milliseconds
     * @param endTime         End milliseconds (optional)
     * @param aggregateByTime Whether to aggregate by time (optional)
     * @return Fill list
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
     * Query user fees (rebates/commissions).
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode userFees(String address) {
        Map<String, Object> payload = Map.of("type", "userFees", "user", address);
        return postInfo(payload);
    }

    /**
     * Query funding rate history (by coin name).
     *
     * @param coin    Coin name (e.g., "BTC")
     * @param startMs Start milliseconds
     * @param endMs   End milliseconds
     * @return List<FundingHistory> response
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
     * Query user funding rate history (by asset ID).
     *
     * @param address User address
     * @param coin    Asset ID
     * @param startMs Start milliseconds
     * @param endMs   End milliseconds
     * @return JSON response
     */
    public JsonNode userFundingHistory(String address, int coin, long startMs, long endMs) {
        return this.userFundingHistory(address, this.coinIdToInfoCoinString(coin), startMs, endMs);
    }

    /**
     * Convert asset ID to /info API coin field format (e.g., "@107").
     * <p>
     * This helper method converts an internal asset ID to the string format
     * required by the /info API endpoints.
     * </p>
     *
     * @param coinId Asset ID
     * @return String in the format "@<id>"
     * @throws HypeError Thrown when ID is invalid (negative or out of range)
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
     * Query user funding rate history (by coin name).
     *
     * @param address User address
     * @param coin    Coin name or internal identifier (e.g., "BTC" or "@107")
     * @param startMs Start milliseconds
     * @param endMs   End milliseconds
     * @return JSON response
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
     * User non-funding ledger updates (excluding funding).
     *
     * @param address User address
     * @param startMs Start milliseconds
     * @param endMs   End milliseconds
     * @return JSON response
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
     * Historical order query.
     *
     * @param address User address
     * @param startMs Start milliseconds
     * @param endMs   End milliseconds
     * @return JSON response
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
     * User TWAP slice fill query.
     *
     * @param address User address
     * @param startMs Start milliseconds
     * @param endMs   End milliseconds
     * @return JSON response
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
     * Frontend additional information unfilled orders (frontendOpenOrders).
     *
     * @param address User address
     * @param dex     Perp dex name (can be empty)
     * @return Frontend unfilled order list
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
     * Frontend additional information unfilled orders (default perp dex).
     *
     * @param address User address
     * @return Frontend unfilled order list
     */
    public List<FrontendOpenOrder> frontendOpenOrders(String address) {
        return frontendOpenOrders(address, null);
    }

    /**
     * User recent fills (up to 2000 entries).
     *
     * @param address         User address
     * @param aggregateByTime Whether to aggregate by time (optional)
     * @return Fill list
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
     * Query all perpetual dexs (perpDexs).
     *
     * @return JSON array
     */
    public JsonNode perpDexs() {
        Map<String, Object> payload = Map.of("type", "perpDexs");
        return postInfo(payload);
    }

    /**
     * Query all perpetual dexs (typed return).
     * Elements may be null or objects, use Map to receive to adapt to field
     * changes.
     *
     * @return Perp dex list (elements are Map or null)
     */
    public List<Map<String, Object>> perpDexsTyped() {
        JsonNode node = perpDexs();
        return JSONUtil.convertValue(node,
                TypeFactory.defaultInstance().constructCollectionType(List.class,
                        TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)));
    }

    /**
     * Perpetual clearinghouse state (user account summary).
     *
     * @param address User address
     * @param dex     Optional perp dex name
     * @return Typed model ClearinghouseState
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
     * Perpetual clearinghouse state (default perp dex).
     *
     * @param address User address
     * @return Typed model ClearinghouseState
     */
    public ClearinghouseState clearinghouseState(String address) {
        return clearinghouseState(address, null);
    }

    /**
     * User state (same as clearinghouseState).
     *
     * @param address User address
     * @return Typed model ClearinghouseState
     */
    public ClearinghouseState userState(String address) {
        return clearinghouseState(address, null);
    }

    /**
     * Get user's token balances (spot clearinghouse state).
     *
     * @param address User address
     * @return Typed model SpotClearinghouseState
     */
    public SpotClearinghouseState spotClearinghouseState(String address) {
        Map<String, Object> payload = Map.of("type", "spotClearinghouseState", "user", address);
        JsonNode node = postInfo(payload);
        return JSONUtil.convertValue(node, SpotClearinghouseState.class);
    }

    /**
     * Query Vault details.
     *
     * @param vaultAddress Vault address
     * @param user         User address (optional)
     * @return JSON response
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
     * Spot Deploy Auction status.
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode spotDeployState(String address) {
        Map<String, Object> payload = Map.of("type", "spotDeployState", "user", address);
        return postInfo(payload);
    }

    /**
     * User portfolio.
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode portfolio(String address) {
        Map<String, Object> payload = Map.of("type", "portfolio", "user", address);
        return postInfo(payload);
    }

    /**
     * User position fee rate and level (userRole).
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode userRole(String address) {
        Map<String, Object> payload = Map.of("type", "userRole", "user", address);
        return postInfo(payload);
    }

    /**
     * User rate limit (userRateLimit).
     *
     * @param address User address
     * @return Typed model UserRateLimit
     */
    public UserRateLimit userRateLimit(String address) {
        Map<String, Object> payload = Map.of("type", "userRateLimit", "user", address);
        return JSONUtil.convertValue(postInfo(payload), UserRateLimit.class);
    }

    /**
     * Order status query (by OID).
     *
     * @param address User address
     * @param oid     Order OID
     * @return Typed model OrderStatus
     */
    public OrderStatus orderStatus(String address, Long oid) {
        Map<String, Object> payload = Map.of("type", "orderStatus", "user", address, "oid", oid);
        return JSONUtil.convertValue(postInfo(payload), OrderStatus.class);
    }

    /**
     * Order status query (by Cloid, aligned with Python query_order_by_cloid).
     *
     * @param address User address
     * @param cloid   Client order ID
     * @return Typed model OrderStatus
     */
    public OrderStatus orderStatusByCloid(String address, Cloid cloid) {
        if (cloid == null) {
            throw new HypeError("cloid cannot be null");
        }
        Map<String, Object> payload = Map.of("type", "orderStatus", "user", address, "oid", cloid.getRaw());
        return JSONUtil.convertValue(postInfo(payload), OrderStatus.class);
    }

    /**
     * Query referrer status (queryReferralState).
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode queryReferralState(String address) {
        Map<String, Object> payload = Map.of("type", "referral", "user", address);
        return postInfo(payload);
    }

    /**
     * Query sub-account list.
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode querySubAccounts(String address) {
        Map<String, Object> payload = Map.of("type", "subAccounts", "user", address);
        return postInfo(payload);
    }

    /**
     * Query user to multi-signature signer mapping.
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode queryUserToMultiSigSigners(String address) {
        Map<String, Object> payload = Map.of("type", "userToMultiSigSigners", "user", address);
        return postInfo(payload);
    }

    /**
     * Perpetual deploy auction status.
     *
     * @return JSON response
     */
    public JsonNode queryPerpDeployAuctionStatus() {
        Map<String, Object> payload = Map.of("type", "perpDeployAuctionStatus");
        return postInfo(payload);
    }

    /**
     * Spot deploy auction status.
     *
     * @return JSON response
     */
    public JsonNode querySpotDeployAuctionStatus() {
        // This interface corresponds to spotDeployState(user) in the Python SDK; the
        // Java SDK already provides it
        // spotDeployState(address)
        // Keep this method to avoid breaking existing calls, but the server does not
        // support spotDeploy queries without a user; return an empty object to avoid
        // 4xx
        try {
            return JSONUtil.readTree("{}");
        } catch (Exception e) {
            throw new HypeError("Failed to parse empty JSON", e);
        }
    }

    /**
     * Get Perp market status (perpDexStatus).
     *
     * @param dex Perp dex name; empty string represents the first perp dex
     * @return JSON object
     */
    public JsonNode perpDexStatus(String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "perpDexStatus");
        payload.put("dex", dex == null ? "" : dex);
        return postInfo(payload);
    }

    /**
     * Get Perp market status (typed return).
     *
     * @param dex Perp dex name; empty string represents the first perp dex
     * @return Typed model PerpDexStatus
     */
    public PerpDexStatus perpDexStatusTyped(String dex) {
        JsonNode node = perpDexStatus(dex);
        return JSONUtil.convertValue(node, PerpDexStatus.class);
    }

    /**
     * Query user DEX abstraction state.
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode queryUserDexAbstractionState(String address) {
        Map<String, Object> payload = Map.of("type", "userDexAbstraction", "user", address);
        return postInfo(payload);
    }

    /**
     * User vault equities.
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode userVaultEquities(String address) {
        Map<String, Object> payload = Map.of("type", "userVaultEquities", "user", address);
        return postInfo(payload);
    }

    /**
     * User's extra agents.
     *
     * @param address User address
     * @return JSON response
     */
    public JsonNode extraAgents(String address) {
        Map<String, Object> payload = Map.of("type", "extraAgents", "user", address);
        return postInfo(payload);
    }

    /**
     * Subscribe to WebSocket (type-safe version, using Subscription entity class).
     * <p>
     * Recommended to use this method, provides compile-time type checking and
     * better code readability.
     * </p>
     * <p>
     * NOTE: This method will throw an exception if WebSocket functionality is
     * disabled
     * via the skipWs flag during initialization.
     * </p>
     *
     * @param subscription Subscription object (Subscription entity class)
     * @param callback     Message callback
     * @throws HypeError Thrown when WebSocket functionality is disabled
     *                   (skipWs=true)
     */
    public void subscribe(Subscription subscription, WebsocketManager.MessageCallback callback) {
        if (skipWs)
            throw new HypeError("WebSocket disabled by skipWs");
        wsManager.subscribe(subscription, callback);
    }

    /**
     * Subscribe to WebSocket (compatible version, using JsonNode).
     * <p>
     * For better type safety, it is recommended to use the
     * {@link #subscribe(Subscription, WebsocketManager.MessageCallback)} method.
     * </p>
     *
     * @param subscription Subscription object
     * @param callback     Message callback
     */
    public void subscribe(JsonNode subscription, WebsocketManager.MessageCallback callback) {
        if (skipWs)
            throw new HypeError("WebSocket disabled by skipWs");
        wsManager.subscribe(subscription, callback);
    }

    /**
     * Get WebSocket subscriptions.
     * <p>
     * NOTE: This method will throw an exception if WebSocket functionality is
     * disabled
     * via the skipWs flag during initialization.
     * </p>
     *
     * @throws HypeError Thrown when WebSocket functionality is disabled
     *                   (skipWs=true)
     */
    public Map<String, List<WebsocketManager.ActiveSubscription>> getSubscriptions() {
        if (skipWs)
            throw new HypeError("WebSocket disabled by skipWs");
        return wsManager.getSubscriptions();
    }

    /**
     * Unsubscribe (type-safe version, using Subscription entity class).
     *
     * @param subscription Subscription object (Subscription entity class)
     */
    public void unsubscribe(Subscription subscription) {
        if (skipWs)
            return;
        wsManager.unsubscribe(subscription);
    }

    /**
     * Unsubscribe (compatible version, using JsonNode).
     *
     * @param subscription Subscription object
     */
    public void unsubscribe(JsonNode subscription) {
        if (skipWs)
            return;
        wsManager.unsubscribe(subscription);
    }

    /**
     * Close WebSocket connection.
     */
    public void closeWs() {
        if (wsManager != null)
            wsManager.stop();
    }

    /**
     * Add connection status listener (connect/disconnect/reconnect/network status
     * changes).
     *
     * @param listener Listener implementation
     */
    public void addConnectionListener(WebsocketManager.ConnectionListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.addConnectionListener(listener);
    }

    /**
     * Remove connection status listener.
     *
     * @param listener Listener implementation
     */
    public void removeConnectionListener(WebsocketManager.ConnectionListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.removeConnectionListener(listener);
    }

    /**
     * Set network monitoring check interval (seconds).
     *
     * @param seconds Interval seconds (default 5, recommended 3~10)
     */
    public void setNetworkCheckIntervalSeconds(int seconds) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setNetworkCheckIntervalSeconds(seconds);
    }

    /**
     * Set reconnection exponential backoff parameters.
     *
     * @param initialMs Initial reconnection delay milliseconds (recommended
     *                  500~2000)
     * @param maxMs     Maximum reconnection delay milliseconds (recommended not to
     *                  exceed 5000~30000)
     */
    public void setReconnectBackoffMs(long initialMs, long maxMs) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setReconnectBackoffMs(initialMs, maxMs);
    }

    /**
     * Set custom network probe URL.
     * <p>
     * By default, the WebSocket manager uses the API baseUrl for network
     * availability probing.
     * In some enterprise environments or special network configurations, a
     * dedicated probe address may be required.
     * </p>
     *
     * @param url Custom network probe URL (e.g., "https://www.google.com")
     */
    public void setNetworkProbeUrl(String url) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setNetworkProbeUrl(url);
    }

    /**
     * Enable or disable network probing functionality.
     * <p>
     * Network probing is used to periodically check network availability when
     * WebSocket is disconnected, and automatically triggers reconnection when the
     * network is restored.
     * In some scenarios (such as always-available intranet environments), probing
     * can be disabled to reduce unnecessary HTTP requests.
     * </p>
     *
     * @param disabled true=disable network probing, false=enable network probing
     *                 (default enabled)
     */
    public void setNetworkProbeDisabled(boolean disabled) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.setNetworkProbeDisabled(disabled);
    }

    /**
     * Add callback exception listener.
     *
     * @param listener Listener implementation
     */
    public void addCallbackErrorListener(WebsocketManager.CallbackErrorListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.addCallbackErrorListener(listener);
    }

    /**
     * Remove callback exception listener.
     *
     * @param listener Listener implementation
     */
    public void removeCallbackErrorListener(WebsocketManager.CallbackErrorListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.removeCallbackErrorListener(listener);
    }

    /**
     * Get WebSocket manager instance.
     *
     * @return WebSocket manager instance
     */
    public WebsocketManager getWsManager() {
        return wsManager;
    }

    /**
     * Query user staking summary (delegatorSummary).
     * <p>
     * POST /info
     * </p>
     *
     * @param address User address (42-character hexadecimal format)
     * @return JSON response containing:
     * <ul>
     * <li>delegated - Delegated amount (float string)</li>
     * <li>undelegated - Undelegated amount (float string)</li>
     * <li>totalPendingWithdrawal - Total pending withdrawal amount (float
     * string)</li>
     * <li>nPendingWithdrawals - Number of pending withdrawals (int)</li>
     * </ul>
     */
    public JsonNode userStakingSummary(String address) {
        Map<String, Object> payload = Map.of(
                "type", "delegatorSummary",
                "user", address);
        return postInfo(payload);
    }

    /**
     * Query user staking delegation details (delegations).
     * <p>
     * POST /info
     * </p>
     *
     * @param address User address (42-character hexadecimal format)
     * @return JSON response array, each element contains:
     * <ul>
     * <li>validator - Validator address (string)</li>
     * <li>amount - Delegated amount (float string)</li>
     * <li>lockedUntilTimestamp - Locked until timestamp (int)</li>
     * </ul>
     */
    public JsonNode userStakingDelegations(String address) {
        Map<String, Object> payload = Map.of(
                "type", "delegations",
                "user", address);
        return postInfo(payload);
    }

    /**
     * Query user historical staking rewards (delegatorRewards).
     * <p>
     * POST /info
     * </p>
     *
     * @param address User address (42-character hexadecimal format)
     * @return JSON response array, each element contains:
     * <ul>
     * <li>time - Timestamp (int)</li>
     * <li>source - Reward source (string)</li>
     * <li>totalAmount - Total reward amount (float string)</li>
     * </ul>
     * @throws HypeError Thrown when the address is not a valid 42-character
     *                   hexadecimal format
     */
    public JsonNode userStakingRewards(String address) {
        Map<String, Object> payload = Map.of(
                "type", "delegatorRewards",
                "user", address);
        return postInfo(payload);
    }

    /**
     * Query delegation history (delegatorHistory).
     * <p>
     * POST /info
     * </p>
     *
     * @param user User address (42-character hexadecimal format)
     * @return JSON response containing detailed history of delegation and
     * undelegation events, including timestamps, transaction hashes, and
     * detailed delta information
     */
    public JsonNode delegatorHistory(String user) {
        Map<String, Object> payload = Map.of(
                "type", "delegatorHistory",
                "user", user);
        return postInfo(payload);
    }
}
