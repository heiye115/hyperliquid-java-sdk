package io.github.hyperliquid.sdk.info;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.hyperliquid.sdk.api.API;
import io.github.hyperliquid.sdk.model.info.*;
import io.github.hyperliquid.sdk.parser.CandleParser;
import io.github.hyperliquid.sdk.utils.Error;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;

import java.util.*;

/**
 * Info 客户端，提供行情、订单簿、用户状态等查询。
 */
public class Info extends API {
    private final boolean skipWs;
    private WebsocketManager wsManager;

    // 元数据缓存与映射
    private final Map<String, Integer> nameToCoin = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Integer> coinToAsset = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Integer> assetToSzDecimals = new java.util.concurrent.ConcurrentHashMap<>();

    // 缓存 TTL 与命中率统计
    private volatile long cacheTtlMs = 10 * 60 * 1000L; // 默认 10 分钟
    private volatile long metaLastRefreshMs = 0L;
    private volatile long spotMetaLastRefreshMs = 0L;
    private volatile long nameToCoinHits = 0L;
    private volatile long nameToCoinMisses = 0L;
    private volatile long coinToAssetHits = 0L;
    private volatile long coinToAssetMisses = 0L;

    /**
     * 构造 Info 客户端。
     *
     * @param baseUrl API 根地址
     * @param timeout 超时（秒）
     * @param skipWs  是否跳过创建 WebSocket 连接（用于测试）
     */
    public Info(String baseUrl, int timeout, boolean skipWs) {
        super(baseUrl, timeout);
        this.skipWs = skipWs;
        if (!skipWs) {
            this.wsManager = new WebsocketManager(baseUrl, mapper);
        }
        // 初始化：尝试刷新 perp 与 spot 元数据，填充映射
        try {
            refreshPerpMeta();
            refreshSpotMeta();
        } catch (Error e) {
            // 初始化失败不影响基本功能，可延迟到调用时再请求
        }
    }

    /** 设置缓存 TTL（毫秒） */
    public void setCacheTtlMs(long ttlMs) {
        this.cacheTtlMs = Math.max(1_000L, ttlMs);
    }

    /** 获取缓存命中率统计与刷新时间信息 */
    public Map<String, Long> getCacheStats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("nameToCoinHits", nameToCoinHits);
        m.put("nameToCoinMisses", nameToCoinMisses);
        m.put("coinToAssetHits", coinToAssetHits);
        m.put("coinToAssetMisses", coinToAssetMisses);
        m.put("metaLastRefreshMs", metaLastRefreshMs);
        m.put("spotMetaLastRefreshMs", spotMetaLastRefreshMs);
        m.put("cacheTtlMs", cacheTtlMs);
        return m;
    }

    /** 显式刷新全部元数据缓存（线程安全） */
    public synchronized void refreshMetadata() throws Error {
        nameToCoin.clear();
        coinToAsset.clear();
        assetToSzDecimals.clear();
        refreshPerpMeta();
        refreshSpotMeta();
    }

    /** 刷新 perp 元数据，并更新映射与刷新时间 */
    private void refreshPerpMeta() throws Error {
        JsonNode meta = meta();
        if (meta != null && meta.has("universe") && meta.get("universe").isArray()) {
            JsonNode universe = meta.get("universe");
            for (int i = 0; i < universe.size(); i++) {
                JsonNode a = universe.get(i);
                String nm = a.has("name") ? a.get("name").asText() : ("ASSET_" + i);
                int assetId = i; // perp 资产使用枚举下标
                nameToCoin.putIfAbsent(nm, i);
                coinToAsset.putIfAbsent(i, assetId);
                int szDec = a.has("szDecimals") ? a.get("szDecimals").asInt() : 2;
                assetToSzDecimals.putIfAbsent(assetId, szDec);
            }
        }
        if (meta != null && meta.has("assets") && meta.get("assets").isArray()) {
            JsonNode assets = meta.get("assets");
            for (int i = 0; i < assets.size(); i++) {
                JsonNode a = assets.get(i);
                int assetId = a.has("id") ? a.get("id").asInt() : i;
                int coinId = a.has("coin") ? a.get("coin").asInt() : i;
                coinToAsset.putIfAbsent(coinId, assetId);
                int szDec = a.has("szDecimals") ? a.get("szDecimals").asInt() : 2;
                assetToSzDecimals.putIfAbsent(assetId, szDec);
            }
        }
        metaLastRefreshMs = System.currentTimeMillis();
    }

    /** 刷新 spot 元数据，并更新映射与刷新时间 */
    private void refreshSpotMeta() throws Error {
        JsonNode spot = spotMeta();
        if (spot != null && spot.has("universe") && spot.get("universe").isArray()) {
            JsonNode universe = spot.get("universe");
            for (int i = 0; i < universe.size(); i++) {
                JsonNode asset = universe.get(i);
                String name = asset.has("name") ? asset.get("name").asText() : ("SPOT_" + i);
                int index = asset.has("index") ? asset.get("index").asInt() : i;
                int assetId = index + 10000; // Spot 资产偏移量

                // name -> coin 的映射：优先使用返回中的 coin 字段，否则使用 index 作为占位
                if (asset.has("coin")) {
                    nameToCoin.putIfAbsent(name, asset.get("coin").asInt());
                } else {
                    nameToCoin.putIfAbsent(name, index);
                }

                // coin -> asset 映射（Spot 使用 index + 10000）
                coinToAsset.putIfAbsent(index, assetId);

                // 计算 szDecimals（取 base token 的 szDecimals）
                if (spot.has("tokens") && asset.has("tokens") && asset.get("tokens").isArray()
                        && asset.get("tokens").size() >= 1) {
                    int baseTokenIndex = asset.get("tokens").get(0).asInt();
                    JsonNode tokens = spot.get("tokens");
                    if (tokens.isArray() && baseTokenIndex >= 0 && baseTokenIndex < tokens.size()) {
                        JsonNode baseInfo = tokens.get(baseTokenIndex);
                        int szDec = baseInfo.has("szDecimals") ? baseInfo.get("szDecimals").asInt() : 2;
                        assetToSzDecimals.putIfAbsent(assetId, szDec);
                    }
                }
            }
        }
        spotMetaLastRefreshMs = System.currentTimeMillis();
    }

    /** TTL 自动刷新检查（必要时调用 refresh） */
    private void ensureFreshCaches() {
        long now = System.currentTimeMillis();
        if (now - metaLastRefreshMs > cacheTtlMs) {
            try {
                refreshPerpMeta();
            } catch (Error ignored) {
            }
        }
        if (now - spotMetaLastRefreshMs > cacheTtlMs) {
            try {
                refreshSpotMeta();
            } catch (Error ignored) {
            }
        }
    }

    /**
     * 查询所有中间价（allMids）。
     *
     * @return JSON 结果
     */
    public JsonNode allMids() {
        Map<String, Object> payload = Map.of("type", "allMids");
        return post("/info", payload);
    }

    /**
     * 查询所有中间价（allMids），类型化返回。
     *
     * <p>
     * 返回 Map 结构，键为币种名称（如 "BTC"、"ETH"），值为字符串形式的中间价。
     * </p>
     *
     * @return 币种到中间价的映射
     */
    public Map<String, String> allMidsTyped() {
        JsonNode node = allMids();
        return mapper.convertValue(node,
                TypeFactory.defaultInstance().constructMapType(Map.class, String.class, String.class));
    }

    /**
     * 查询所有中间价（allMids），可指定 perp dex 名称。
     *
     * <p>
     * 当 dex 为空字符串或未提供时，默认代表第一个 perp dex（与官方文档约定一致）。
     * </p>
     *
     * @param dex perp dex 名称（可为空或空字符串）
     * @return JSON 结果
     */
    public JsonNode allMids(String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "allMids");
        if (dex != null)
            payload.put("dex", dex);
        return post("/info", payload);
    }

    /**
     * 查询所有中间价（allMids），类型化返回，可指定 perp dex 名称。
     *
     * @param dex perp dex 名称（可为空或空字符串）
     * @return 币种到中间价的映射
     */
    public Map<String, String> allMidsTyped(String dex) {
        JsonNode node = allMids(dex);
        return mapper.convertValue(node,
                TypeFactory.defaultInstance().constructMapType(Map.class, String.class, String.class));
    }

    /**
     * 查询 perp 元数据（meta）。
     */
    public JsonNode meta() {
        Map<String, Object> payload = Map.of("type", "meta");
        return post("/info", payload);
    }

    /**
     * 查询 perp 元数据（meta），可指定 perp dex 名称。
     *
     * @param dex perp dex 名称（可为空或空字符串）
     * @return JSON 结果
     */
    public JsonNode meta(String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "meta");
        if (dex != null)
            payload.put("dex", dex);
        return post("/info", payload);
    }

    /**
     * 查询 perp 元数据与资产上下文（metaAndAssetCtxs）。
     */
    public JsonNode metaAndAssetCtxs() {
        Map<String, Object> payload = Map.of("type", "metaAndAssetCtxs");
        return post("/info", payload);
    }

    /**
     * 查询 spot 元数据（spotMeta）。
     */
    public JsonNode spotMeta() {
        Map<String, Object> payload = Map.of("type", "spotMeta");
        return post("/info", payload);
    }

    /**
     * 查询 spot 元数据与资产上下文（spotMetaAndAssetCtxs）。
     */
    public JsonNode spotMetaAndAssetCtxs() {
        Map<String, Object> payload = Map.of("type", "spotMetaAndAssetCtxs");
        return post("/info", payload);
    }

    /**
     * L2 订单簿快照。
     *
     * @param coin 币种整数 ID
     * @return JSON 结果
     */
    public JsonNode l2Snapshot(int coin) {
        Map<String, Object> payload = Map.of("type", "l2Snapshot", "coin", coin);
        return post("/info", payload);
    }

    /**
     * K 线快照。
     *
     * @param coin       币种整数 ID
     * @param intervalMs 间隔毫秒
     * @param startMs    起始毫秒
     * @param endMs      结束毫秒
     * @return JSON 结果
     */
    public JsonNode candlesSnapshot(int coin, long intervalMs, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "candlesSnapshot");
        payload.put("coin", coin);
        payload.put("intervalMs", intervalMs);
        payload.put("startMs", startMs);
        payload.put("endMs", endMs);
        return post("/info", payload);
    }

    /**
     * K 线快照（与官方文档完全一致的接口名称与参数结构）。
     *
     * <p>
     * 官方文档请求体示例：
     * {"type":"candleSnapshot","req": {"coin": <int>, "interval": "15m",
     * "startTime": <ms>, "endTime": <ms>}}
     * </p>
     *
     * @param coin      币种整数 ID
     * @param interval  间隔字符串（如 "1m"、"15m"、"1h"、"1d" 等）
     * @param startTime 起始毫秒
     * @param endTime   结束毫秒
     * @return JSON 结果（数组）
     */
    public JsonNode candleSnapshot(int coin, String interval, long startTime, long endTime) {
        // 为了与官方 /info 文档保持一致，这里的 coin 字段需要是字符串：
        // - perp：使用 meta.universe[coin].name（如 "BTC"、"ETH"）
        // - spot：使用形如 "@index" 的字符串（如 "@107"）
        String coinStr = coinIdToInfoCoinString(coin);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("coin", coinStr);
        req.put("interval", interval);
        req.put("startTime", startTime);
        req.put("endTime", endTime);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "candleSnapshot");
        payload.put("req", req);
        return post("/info", payload);
    }

    /**
     * K 线快照（类型化返回，基于当前 SDK 的毫秒版接口）。
     *
     * @param coin       币种整数 ID
     * @param intervalMs 间隔毫秒
     * @param startMs    起始毫秒
     * @param endMs      结束毫秒
     * @return Candle 列表
     */
    public List<Candle> candlesSnapshotTyped(int coin, long intervalMs,
                                             long startMs, long endMs) {
        JsonNode node = candlesSnapshot(coin, intervalMs, startMs, endMs);
        return CandleParser.parseList(node);
    }

    /**
     * K 线快照（类型化返回，与官方文档一致的接口名称与参数结构）。
     *
     * @param coin      币种整数 ID
     * @param interval  间隔字符串（如 "1m"、"15m"、"1h"、"1d" 等）
     * @param startTime 起始毫秒
     * @param endTime   结束毫秒
     * @return Candle 列表
     */
    public List<Candle> candleSnapshotTyped(int coin, String interval,
                                            long startTime, long endTime) {
        JsonNode node = candleSnapshot(coin, interval, startTime, endTime);
        return CandleParser.parseList(node);
    }

    /**
     * K 线快照（类型化返回，支持以币种名称传参）。
     *
     * <p>
     * 某些环境下服务端可能要求请求体中 coin 字段为字符串（如 "BTC" 或 "@107"）。
     * 为提升兼容性，提供该重载方法，使用币种名称直接发起请求。
     * </p>
     *
     * @param coinName  币种名称（如 "BTC"，或形如 "@107" 的内部标识）
     * @param interval  间隔字符串（如 "1m"、"15m"、"1h"、"1d" 等）
     * @param startTime 起始毫秒
     * @param endTime   结束毫秒
     * @return Candle 列表
     */
    public List<Candle> candleSnapshotTyped(String coinName, String interval,
                                            long startTime, long endTime) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("coin", coinName);
        req.put("interval", interval);
        req.put("startTime", startTime);
        req.put("endTime", endTime);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "candleSnapshot");
        payload.put("req", req);
        JsonNode node = post("/info", payload);
        return CandleParser.parseList(node);
    }

    /**
     * 将整数 coinId 转换为 /info 接口需要的字符串 coin 表达。
     *
     * 规则：
     * - 若能在 meta.universe 中以下标找到名称，则返回该名称（用于 perp，例如 "BTC"、"ETH"）。
     * - 否则回退为 "@" + coinId（用于 spot，例如 "@107"）。
     *
     * @param coinId 整数 coin 标识
     * @return /info 请求所需的字符串 coin
     */
    private String coinIdToInfoCoinString(int coinId) {
        try {
            JsonNode m = meta();
            if (m != null && m.has("universe") && m.get("universe").isArray()) {
                JsonNode uni = m.get("universe");
                if (coinId >= 0 && coinId < uni.size()) {
                    JsonNode a = uni.get(coinId);
                    if (a != null && a.has("name")) {
                        String nm = a.get("name").asText();
                        if (nm != null && !nm.isEmpty()) {
                            return nm;
                        }
                    }
                }
            }
        } catch (Error e) {
            // 忽略初始化失败，走回退逻辑
        }
        // Spot 或未命中时的通用回退
        return "@" + coinId;
    }

    /**
     * 获取最近一个间隔周期的最新 K 线（类型化返回，与官方文档一致），便捷方法。
     *
     * <p>
     * 该方法会以当前时间为 endTime，startTime=endTime-intervalMs，发起一次 candleSnapshot 请求，
     * 并返回列表的最后一条。注意：服务端仅提供最近 5000 根 K 线，若时间跨度过大可能导致窗口裁剪。
     * </p>
     *
     * @param coin     币种整数 ID
     * @param interval 间隔字符串（如 "1m"、"15m"、"1h"、"1d" 等）
     * @return 最新一条 Candle（若不存在则返回 Optional.empty）
     */
    public Optional<Candle> candleSnapshotLatestTyped(int coin,
                                                      String interval) {
        long endTime = System.currentTimeMillis();
        long intervalMs = intervalToMs(interval);
        long startTime = endTime - intervalMs;
        JsonNode node = candleSnapshot(coin, interval, startTime, endTime);
        return CandleParser.parseLatest(node);
    }

    /**
     * 间隔字符串转毫秒工具（支持官方文档列出的间隔）。
     *
     * @param interval 间隔字符串（如 "1m"、"15m"、"1h"、"1d"、"1w"、"1M"）
     * @return 间隔毫秒（"1M" 近似按 30 天计算）
     * @throws Error 若间隔不受支持
     */
    private long intervalToMs(String interval) {
        if (interval == null)
            throw new Error("间隔字符串不能为空");
        switch (interval) {
            case "1m":
                return 60_000L;
            case "3m":
                return 180_000L;
            case "5m":
                return 300_000L;
            case "15m":
                return 900_000L;
            case "30m":
                return 1_800_000L;
            case "1h":
                return 3_600_000L;
            case "2h":
                return 7_200_000L;
            case "4h":
                return 14_400_000L;
            case "8h":
                return 28_800_000L;
            case "12h":
                return 43_200_000L;
            case "1d":
                return 86_400_000L;
            case "3d":
                return 259_200_000L;
            case "1w":
                return 604_800_000L;
            case "1M":
                return 2_592_000_000L; // 30 天近似
            default:
                throw new Error("不支持的间隔字符串：" + interval);
        }
    }

    /**
     * 用户状态查询。
     *
     * @param address 用户地址
     * @return JSON 结果
     */
    public JsonNode userState(String address) {
        Map<String, Object> payload = Map.of("type", "userState", "user", address);
        return post("/info", payload);
    }

    /**
     * 查询用户未成交订单。
     */
    public JsonNode openOrders(String address) {
        Map<String, Object> payload = Map.of("type", "openOrders", "user", address);
        return post("/info", payload);
    }

    /**
     * 查询用户未成交订单（openOrders），可指定 perp dex 名称。
     *
     * @param address 用户地址
     * @param dex     perp dex 名称（可为空或空字符串）
     * @return JSON 数组
     */
    public JsonNode openOrders(String address, String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "openOrders");
        payload.put("user", address);
        if (dex != null)
            payload.put("dex", dex);
        return post("/info", payload);
    }

    /**
     * 查询用户未成交订单（类型化返回）。
     *
     * <p>
     * 与 frontendOpenOrdersTyped 相比，openOrdersTyped 返回字段更精简，coin 为字符串（如 "BTC" 或
     * "@107"）。
     * </p>
     *
     * @param address 用户地址
     * @return 订单列表（类型安全）
     */
    public List<OpenOrder> openOrdersTyped(String address) {
        JsonNode node = openOrders(address);
        return mapper.convertValue(node,
                TypeFactory.defaultInstance().constructCollectionType(List.class,
                        OpenOrder.class));
    }

    /**
     * 查询用户未成交订单（类型化返回），可指定 perp dex 名称。
     *
     * @param address 用户地址
     * @param dex     perp dex 名称（可为空或空字符串）
     * @return 订单列表（类型安全）
     */
    public List<OpenOrder> openOrdersTyped(String address, String dex) {
        JsonNode node = openOrders(address, dex);
        return mapper.convertValue(node,
                TypeFactory.defaultInstance().constructCollectionType(List.class,
                        OpenOrder.class));
    }

    /**
     * 查询用户成交（按时间范围）。
     */
    public JsonNode userFillsByTime(String address, long startTime, long endTime) {
        // 与官方文档对齐字段名：startTime / endTime
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFillsByTime");
        payload.put("user", address);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        return post("/info", payload);
    }

    /**
     * 查询用户成交（按时间范围，带聚合选项）。
     *
     * @param address         用户地址
     * @param startTime       起始毫秒（含）
     * @param endTime         结束毫秒（含；可传当前时间）
     * @param aggregateByTime 是否按时间聚合（参考文档说明）
     * @return JSON 结果
     */
    public JsonNode userFillsByTime(String address, long startTime, long endTime, boolean aggregateByTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFillsByTime");
        payload.put("user", address);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        payload.put("aggregateByTime", aggregateByTime);
        return post("/info", payload);
    }

    /**
     * 用户成交（按时间范围，类型化返回）。
     *
     * @param address   用户地址
     * @param startTime 起始毫秒（含）
     * @param endTime   结束毫秒（含）
     * @return 成交列表（类型安全）
     */
    public List<Fill> userFillsByTimeTyped(String address, long startTime, long endTime) {
        JsonNode node = userFillsByTime(address, startTime, endTime);
        return mapper.convertValue(node, TypeFactory.defaultInstance().constructCollectionType(List.class, Fill.class));
    }

    /**
     * 用户成交（按时间范围，类型化返回，带聚合选项）。
     *
     * @param address         用户地址
     * @param startTime       起始毫秒（含）
     * @param endTime         结束毫秒（含）
     * @param aggregateByTime 是否按时间聚合
     * @return 成交列表（类型安全）
     */
    public List<Fill> userFillsByTimeTyped(String address, long startTime, long endTime, boolean aggregateByTime) {
        JsonNode node = userFillsByTime(address, startTime, endTime, aggregateByTime);
        return mapper.convertValue(node, TypeFactory.defaultInstance().constructCollectionType(List.class, Fill.class));
    }

    /**
     * 查询用户费用（返现/手续费）。
     */
    public JsonNode userFees(String address) {
        Map<String, Object> payload = Map.of("type", "userFees", "user", address);
        return post("/info", payload);
    }

    /**
     * 查询资金费率历史（指定币种与时间范围）。
     */
    public JsonNode fundingHistory(int coin, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "fundingHistory");
        payload.put("coin", coin);
        // 文档此处未明确 startMs/endMs 命名，保留历史命名；如需对齐可改为 startTime/endTime
        payload.put("startMs", startMs);
        payload.put("endMs", endMs);
        return post("/info", payload);
    }

    /**
     * 查询用户资金费率历史（按用户与币种）。
     */
    public JsonNode userFundingHistory(String address, int coin, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFundingHistory");
        payload.put("user", address);
        payload.put("coin", coin);
        // 文档此处未明确 startMs/endMs 命名，保留历史命名；如需对齐可改为 startTime/endTime
        payload.put("startMs", startMs);
        payload.put("endMs", endMs);
        return post("/info", payload);
    }

    /**
     * 用户非资金费率账本更新（不含 funding）。
     */
    public JsonNode userNonFundingLedgerUpdates(String address, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userNonFundingLedgerUpdates");
        payload.put("user", address);
        // 文档未明确字段命名，保留历史命名；如需对齐可改为 startTime/endTime
        payload.put("startMs", startMs);
        payload.put("endMs", endMs);
        return post("/info", payload);
    }

    /**
     * 历史订单查询。
     */
    public JsonNode historicalOrders(String address, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "historicalOrders");
        payload.put("user", address);
        // 文档未明确字段命名，保留历史命名；如需对齐可改为 startTime/endTime
        payload.put("startMs", startMs);
        payload.put("endMs", endMs);
        return post("/info", payload);
    }

    /**
     * 用户 TWAP 切片成交查询。
     */
    public JsonNode userTwapSliceFills(String address, long startMs, long endMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userTwapSliceFills");
        payload.put("user", address);
        // 文档未明确字段命名，保留历史命名；如需对齐可改为 startTime/endTime
        payload.put("startMs", startMs);
        payload.put("endMs", endMs);
        return post("/info", payload);
    }

    // =====================
    // 文档缺失接口补全
    // =====================

    /**
     * 前端附加信息的未成交订单（frontendOpenOrders）。
     *
     * @param address 用户地址
     * @return JSON 数组（包含前端额外字段）
     */
    public JsonNode frontendOpenOrders(String address) {
        Map<String, Object> payload = Map.of("type", "frontendOpenOrders", "user", address);
        return post("/info", payload);
    }

    /**
     * 前端附加信息的未成交订单（frontendOpenOrders），可指定 perp dex 名称。
     *
     * @param address 用户地址
     * @param dex     perp dex 名称（可为空或空字符串）
     * @return JSON 数组（包含前端额外字段）
     */
    public JsonNode frontendOpenOrders(String address, String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "frontendOpenOrders");
        payload.put("user", address);
        if (dex != null)
            payload.put("dex", dex);
        return post("/info", payload);
    }

    /**
     * 前端未成交订单（类型化返回）。
     *
     * @param address 用户地址
     * @return 订单列表（类型安全）
     */
    public List<FrontendOpenOrder> frontendOpenOrdersTyped(String address) {
        JsonNode node = frontendOpenOrders(address);
        return mapper.convertValue(node,
                TypeFactory.defaultInstance().constructCollectionType(List.class, FrontendOpenOrder.class));
    }

    /**
     * 前端未成交订单（类型化返回），可指定 perp dex 名称。
     *
     * @param address 用户地址
     * @param dex     perp dex 名称（可为空或空字符串）
     * @return 订单列表（类型安全）
     */
    public List<FrontendOpenOrder> frontendOpenOrdersTyped(String address, String dex) {
        JsonNode node = frontendOpenOrders(address, dex);
        return mapper.convertValue(node,
                TypeFactory.defaultInstance().constructCollectionType(List.class, FrontendOpenOrder.class));
    }

    /**
     * 用户最近成交（最多 2000 条）。
     *
     * @param address         用户地址
     * @param aggregateByTime 是否按时间聚合（true/false）
     * @return JSON 数组
     */
    public JsonNode userFills(String address, boolean aggregateByTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "userFills");
        payload.put("user", address);
        payload.put("aggregateByTime", aggregateByTime);
        return post("/info", payload);
    }

    /**
     * 用户最近成交（类型化返回）。
     */
    public List<Fill> userFillsTyped(String address, boolean aggregateByTime) {
        JsonNode node = userFills(address, aggregateByTime);
        return mapper.convertValue(node, TypeFactory.defaultInstance().constructCollectionType(List.class, Fill.class));
    }

    /**
     * 查询所有 perpetual dexs（perpDexs）。
     *
     * @return JSON 数组
     */
    public JsonNode perpDexs() {
        Map<String, Object> payload = Map.of("type", "perpDexs");
        return post("/info", payload);
    }

    /**
     * 查询所有 perpetual dexs（类型化返回）。
     *
     * <p>
     * 文档示例显示返回数组的元素可能为 null 或对象，形如：
     * [null, {"name":"test","fullName":"test dex", ...}]
     * 因此此处采用 Map<String, Object> 以保持灵活的字段接收能力。
     * </p>
     */
    public List<Map<String, Object>> perpDexsTyped() {
        JsonNode node = perpDexs();
        return mapper.convertValue(node,
                TypeFactory.defaultInstance().constructCollectionType(List.class,
                        TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)));
    }

    /**
     * 永续清算所状态（用户账户摘要）。
     *
     * @param address 用户地址
     * @param dex     可选 dex 名称（传 null 则不包含该字段）
     * @return JSON 对象
     */
    public JsonNode clearinghouseState(String address, String dex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "clearinghouseState");
        payload.put("user", address);
        if (dex != null && !dex.isEmpty()) {
            payload.put("dex", dex);
        }
        return post("/info", payload);
    }

    /**
     * 永续清算所状态（类型化返回）。
     */
    public ClearinghouseState clearinghouseStateTyped(String address, String dex) {
        JsonNode node = clearinghouseState(address, dex);
        return mapper.convertValue(node, ClearinghouseState.class);
    }

    /**
     * 现货清算所状态（用户 token 余额）。
     *
     * @param address 用户地址
     * @return JSON 对象
     */
    public JsonNode spotClearinghouseState(String address) {
        Map<String, Object> payload = Map.of("type", "spotClearinghouseState", "user", address);
        return post("/info", payload);
    }

    /**
     * 现货清算所状态（类型化返回）。
     */
    public SpotClearinghouseState spotClearinghouseStateTyped(String address) {
        JsonNode node = spotClearinghouseState(address);
        return mapper.convertValue(node, SpotClearinghouseState.class);
    }

    /**
     * Spot Deploy Auction 状态。
     *
     * @param address 用户地址（文档要求）
     * @return JSON 对象
     */
    public JsonNode spotDeployState(String address) {
        Map<String, Object> payload = Map.of("type", "spotDeployState", "user", address);
        return post("/info", payload);
    }

    /**
     * 用户组合（portfolio）。
     */
    public JsonNode portfolio(String address) {
        Map<String, Object> payload = Map.of("type", "portfolio", "user", address);
        return post("/info", payload);
    }

    /**
     * 用户仓位费率与等级（userRole）。
     */
    public JsonNode userRole(String address) {
        Map<String, Object> payload = Map.of("type", "userRole", "user", address);
        return post("/info", payload);
    }

    /**
     * 用户速率限制（userRateLimit）。
     */
    public JsonNode userRateLimit(String address) {
        Map<String, Object> payload = Map.of("type", "userRateLimit", "user", address);
        return post("/info", payload);
    }

    /**
     * 查询推荐人状态。
     */
    public JsonNode queryReferralState(String address) {
        Map<String, Object> payload = Map.of("type", "queryReferralState", "user", address);
        return post("/info", payload);
    }

    /**
     * 查询子账户列表。
     */
    public JsonNode querySubAccounts(String address) {
        Map<String, Object> payload = Map.of("type", "querySubAccounts", "user", address);
        return post("/info", payload);
    }

    /**
     * 查询用户到多签签名者映射。
     */
    public JsonNode queryUserToMultiSigSigners(String address) {
        Map<String, Object> payload = Map.of("type", "queryUserToMultiSigSigners", "user", address);
        return post("/info", payload);
    }

    /**
     * 永续部署拍卖状态。
     */
    public JsonNode queryPerpDeployAuctionStatus() {
        Map<String, Object> payload = Map.of("type", "queryPerpDeployAuctionStatus");
        return post("/info", payload);
    }

    /**
     * 现货部署拍卖状态。
     */
    public JsonNode querySpotDeployAuctionStatus() {
        Map<String, Object> payload = Map.of("type", "querySpotDeployAuctionStatus");
        return post("/info", payload);
    }

    // =====================
    // 新增：Perp 市场状态
    // =====================

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
        return post("/info", payload);
    }

    /**
     * 获取 Perp 市场状态（类型化返回）。
     *
     * @param dex perp dex 名称；空字符串代表第一个 perp dex
     * @return 类型化模型 PerpDexStatus
     */
    public PerpDexStatus perpDexStatusTyped(String dex) {
        JsonNode node = perpDexStatus(dex);
        return mapper.convertValue(node, PerpDexStatus.class);
    }

    /**
     * 查询用户 DEX 抽象状态。
     */
    public JsonNode queryUserDexAbstractionState(String address) {
        Map<String, Object> payload = Map.of("type", "queryUserDexAbstractionState", "user", address);
        return post("/info", payload);
    }

    /**
     * 用户仓库（vault）权益。
     */
    public JsonNode userVaultEquities(String address) {
        Map<String, Object> payload = Map.of("type", "userVaultEquities", "user", address);
        return post("/info", payload);
    }

    /**
     * 用户的额外代理（extraAgents）。
     */
    public JsonNode extraAgents(String address) {
        Map<String, Object> payload = Map.of("type", "extraAgents", "user", address);
        return post("/info", payload);
    }

    /**
     * 名称映射为资产 ID。
     *
     * @param name 币种名（如 "ETH"）
     * @return 资产 ID（可能抛出异常）
     */
    public int nameToAsset(String name) {
        // TTL 检查，必要时刷新缓存
        ensureFreshCaches();
        // 尝试直接从缓存读取
        Integer coin = nameToCoin.get(name);
        if (coin != null) {
            nameToCoinHits++;
        } else {
            nameToCoinMisses++;
        }
        if (coin == null) {
            // 刷新 perp 元数据
            refreshPerpMeta();
            coin = nameToCoin.get(name);
        }

        // 若仍未命中，尝试刷新 spot 元数据（spotMeta.universe）
        if (coin == null) {
            refreshSpotMeta();
            coin = nameToCoin.get(name);
        }

        if (coin == null)
            throw new Error("Unknown coin name: " + name);
        Integer asset = coinToAsset.get(coin);
        if (asset != null) {
            coinToAssetHits++;
        } else {
            coinToAssetMisses++;
        }
        if (asset == null) {
            // 兼容旧结构：尝试从 meta.assets 刷新
            // 使用 perp meta.assets 进行补充刷新
            refreshPerpMeta();
            asset = coinToAsset.get(coin);
        }
        if (asset == null)
            throw new Error("Unknown asset for coin: " + name);
        return asset;
    }

    /**
     * 订阅 WebSocket。
     *
     * @param subscription 订阅对象
     * @param callback     消息回调
     */
    public void subscribe(JsonNode subscription, WebsocketManager.MessageCallback callback) {
        if (skipWs)
            throw new Error("WebSocket disabled by skipWs");
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

    // ============================
    // WebSocket 重连与网络监听配置代理
    // ============================

    /**
     * 添加连接状态监听器（用于接收连接、断开、重连、网络状态变化的通知）。
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

    // ============================
    // WebSocket 回调异常监听配置代理
    // ============================

    /** 添加回调异常监听器 */
    public void addCallbackErrorListener(WebsocketManager.CallbackErrorListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.addCallbackErrorListener(listener);
    }

    /** 移除回调异常监听器 */
    public void removeCallbackErrorListener(WebsocketManager.CallbackErrorListener listener) {
        if (skipWs)
            return;
        if (wsManager != null)
            wsManager.removeCallbackErrorListener(listener);
    }
}
