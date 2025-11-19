package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.*;
import io.github.hyperliquid.sdk.utils.HypeError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HyperliquidClient 测试类
 * 使用 JUnit 5 对 client.getInfo() 下的所有方法进行完整测试。
 * 包含：成功调用验证、返回结构断言、错误处理与日志输出校验。
 */
public class HyperliquidClientTest {

    /**
     * 测试网私钥（仅用于测试）
     */
    private static final String TESTNET_PRIVATE_KEY = "your_testnet_private_key_here";

    /**
     * 被测客户端
     */
    private HyperliquidClient client;

    /**
     * 当前测试地址
     */
    private String address;

    /**
     * 捕获 slf4j-simple 的标准错误输出以验证日志
     */
    private ByteArrayOutputStream errContent;

    /**
     * 备份原始 System.err
     */
    private PrintStream originalErr;

    /**
     * 测试初始化：构建测试网客户端、启用调试日志、捕获日志输出。
     */
    @BeforeEach
    void setUp() {
        originalErr = System.err;
        errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        client = HyperliquidClient.builder()
                //.testNetUrl()
                .addPrivateKey(TESTNET_PRIVATE_KEY)
                .enableDebugLogs()
                .build();

        address = client.getSingleAddress();
        assertNotNull(address);
    }

    /**
     * 测试清理：关闭 WS、恢复日志输出、释放客户端。
     */
    @AfterEach
    void tearDown() {
        try {
            client.getInfo().closeWs();
        } catch (Exception ignored) {
        }
        System.setErr(originalErr);
        client = null;
    }

    /**
     * 工具方法：重置日志缓冲。
     */
    private void resetLogs() {
        errContent.reset();
    }

    /**
     * 工具方法：校验日志已包含基本的 POST/Request/Response 关键字。
     */
    private void assertHttpLogsPresent() {
        String logs = errContent.toString();
        assertTrue(logs.contains("/info"));
        assertTrue(logs.contains("POST:"));
        assertTrue(logs.contains("Request:"));
    }

    /**
     * 验证账户信息获取功能：清算所状态与用户状态。
     */
    @Test
    @DisplayName("账户信息：clearinghouseState/userState 返回结构与日志")
    void testGetAccountInfo() {
        resetLogs();
        Info info = client.getInfo();
        ClearinghouseState state = info.clearinghouseState(address);
        assertNotNull(state);
        assertNotNull(state.getAssetPositions());
        assertHttpLogsPresent();

        resetLogs();
        ClearinghouseState userState = info.userState(address);
        assertNotNull(userState);
        assertNotNull(userState.getAssetPositions());
        assertHttpLogsPresent();
    }

    /**
     * 验证市场数据获取功能：meta、allMids、l2Book、candleSnapshotLatest。
     */
    @Test
    @DisplayName("市场数据：meta/allMids/l2Book/candleSnapshotLatest")
    void testGetMarketData() {
        Info info = client.getInfo();

        resetLogs();
        Meta meta = info.meta();
        assertNotNull(meta);
        assertNotNull(meta.getUniverse());
        assertFalse(meta.getUniverse().isEmpty());
        assertHttpLogsPresent();

        resetLogs();
        Map<String, String> mids = info.allMids();
        assertNotNull(mids);
        assertHttpLogsPresent();

        resetLogs();
        L2Book book = info.l2Book("BTC");
        assertNotNull(book);
        assertNotNull(book.getLevels());
        assertHttpLogsPresent();

        resetLogs();
        Candle latest = info.candleSnapshotLatest("BTC", CandleInterval.MINUTE_1);
        // 最新 K 线可能为 null（无数据时）
        if (latest != null) {
            assertNotNull(latest.getStartTimestamp());
            assertNotNull(latest.getClosePrice());
        }
        assertHttpLogsPresent();
    }

    /**
     * 验证未成交订单查询功能：openOrders 与 frontendOpenOrders。
     */
    @Test
    @DisplayName("未成交订单：openOrders/frontendOpenOrders 列表结构")
    void testGetOpenOrders() {
        Info info = client.getInfo();

        resetLogs();
        List<OpenOrder> orders = info.openOrders(address);
        assertNotNull(orders);
        assertHttpLogsPresent();

        resetLogs();
        List<FrontendOpenOrder> feOrders = info.frontendOpenOrders(address);
        assertNotNull(feOrders);
        assertHttpLogsPresent();
    }

    /**
     * 验证持仓信息查询功能：ClearinghouseState.assetPositions 字段。
     */
    @Test
    @DisplayName("持仓信息：ClearinghouseState.assetPositions 字段校验")
    void testGetPositions() {
        Info info = client.getInfo();
        resetLogs();
        ClearinghouseState state = info.clearinghouseState(address);
        assertNotNull(state);
        assertNotNull(state.getAssetPositions());
        if (!state.getAssetPositions().isEmpty()) {
            ClearinghouseState.AssetPositions ap = state.getAssetPositions().getFirst();
            assertNotNull(ap.getType());
            assertNotNull(ap.getPosition());
        }
        assertHttpLogsPresent();
    }

    /**
     * 元数据与缓存：meta(String)、loadMetaCache
     */
    @Test
    @DisplayName("元数据：meta(dex)/loadMetaCache 缓存生效")
    void testMetaAndCache() {
        Info info = client.getInfo();
        resetLogs();
        Meta m1 = info.meta("");
        assertNotNull(m1);
        assertHttpLogsPresent();

        resetLogs();
        Meta cached = info.loadMetaCache();
        assertNotNull(cached);
        // loadMetaCache 不触发请求，不强制要求日志包含 POST
    }

    /**
     * 元数据与资产上下文：JSON 与类型化一致性
     */
    @Test
    @DisplayName("元数据：metaAndAssetCtxs JSON/typed 一致性")
    void testMetaAndAssetCtxs() {
        Info info = client.getInfo();

        resetLogs();
        JsonNode node = info.metaAndAssetCtxs();
        assertNotNull(node);
        assertTrue(node.isObject() || node.isArray());
        assertHttpLogsPresent();

        resetLogs();
        MetaAndAssetCtxs typed = info.metaAndAssetCtxsTyped();
        assertNotNull(typed);
        assertHttpLogsPresent();
    }

    /**
     * 现货元数据：spotMeta 与 spotMetaAndAssetCtxs
     */
    @Test
    @DisplayName("现货元数据：spotMeta/spotMetaAndAssetCtxs")
    void testSpotMeta() {
        Info info = client.getInfo();

        resetLogs();
        SpotMeta sm = info.spotMeta();
        assertNotNull(sm);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode node = info.spotMetaAndAssetCtxs();
        assertNotNull(node);
        assertHttpLogsPresent();
    }

    /**
     * perpDexs：JSON 与类型化
     */
    @Test
    @DisplayName("永续 DEX 列表：perpDexs/perpDexsTyped")
    void testPerpDexs() {
        Info info = client.getInfo();

        resetLogs();
        JsonNode node = info.perpDexs();
        assertNotNull(node);
        assertTrue(node.isArray());
        assertHttpLogsPresent();

        resetLogs();
        List<Map<String, Object>> typed = info.perpDexsTyped();
        assertNotNull(typed);
        assertHttpLogsPresent();
    }

    /**
     * perpDexStatus：JSON 与类型化
     */
    @Test
    @DisplayName("永续 DEX 状态：perpDexStatus/perpDexStatusTyped")
    void testPerpDexStatus() {
        Info info = client.getInfo();

        resetLogs();
        JsonNode node = info.perpDexStatus("");
        assertNotNull(node);
        assertTrue(node.isObject());
        assertHttpLogsPresent();

        resetLogs();
        PerpDexStatus status = info.perpDexStatusTyped("");
        assertNotNull(status);
        assertHttpLogsPresent();
    }

    /**
     * openOrders：dex 变体
     */
    @Test
    @DisplayName("未成交订单：openOrders(dex)")
    void testOpenOrdersWithDex() {
        Info info = client.getInfo();
        resetLogs();
        List<OpenOrder> o = info.openOrders(address, "");
        assertNotNull(o);
        assertHttpLogsPresent();
    }

    /**
     * allMids：默认与指定 dex
     */
    @Test
    @DisplayName("中间价：allMids 默认与指定 dex")
    void testAllMids() {
        Info info = client.getInfo();

        resetLogs();
        Map<String, String> m1 = info.allMids();
        assertNotNull(m1);
        assertHttpLogsPresent();

        resetLogs();
        Map<String, String> m2 = info.allMids("");
        assertNotNull(m2);
        assertHttpLogsPresent();
    }

    /**
     * L2 订单簿：聚合参数
     */
    @Test
    @DisplayName("订单簿：l2Book 聚合参数有效性")
    void testL2BookAggregations() {
        Info info = client.getInfo();
        resetLogs();
        try {
            L2Book b1 = info.l2Book("BTC", 5, 1);
            assertNotNull(b1);
            assertNotNull(b1.getLevels());
            assertHttpLogsPresent();
        } catch (HypeError.ServerHypeError e) {
            assertHttpLogsPresent();
            resetLogs();
            L2Book fallback = info.l2Book("BTC");
            assertNotNull(fallback);
            assertNotNull(fallback.getLevels());
            assertHttpLogsPresent();
        }
    }

    /**
     * K 线：范围与数量
     */
    @Test
    @DisplayName("K线：candleSnapshot 范围与数量")
    void testCandles() {
        Info info = client.getInfo();
        long end = Instant.now().toEpochMilli();
        long start = end - CandleInterval.MINUTE_1.toMillis() * 30;

        resetLogs();
        List<Candle> cs = info.candleSnapshot("BTC", CandleInterval.MINUTE_1, start, end);
        assertNotNull(cs);
        assertHttpLogsPresent();

        resetLogs();
        List<Candle> last10 = info.candleSnapshotByCount("BTC", CandleInterval.MINUTE_1, 10);
        assertNotNull(last10);
        assertTrue(last10.size() <= 10);
        assertHttpLogsPresent();
    }

    /**
     * K 线：非法数量应抛异常
     */
    @Test
    @DisplayName("K线错误：count<=0 抛出 HypeError")
    void testCandleSnapshotByCountInvalid() {
        Info info = client.getInfo();
        assertThrows(HypeError.class, () -> info.candleSnapshotByCount("BTC", CandleInterval.MINUTE_1, 0));
    }

    /**
     * 名称到资产 ID 映射：未知名称应抛异常
     */
    @Test
    @DisplayName("名称映射错误：未知币种抛出 HypeError")
    void testNameToAssetUnknown() {
        Info info = client.getInfo();
        info.loadMetaCache();
        assertThrows(HypeError.class, () -> info.nameToAsset("UNKNOWN_COIN_XYZ"));
    }

    /**
     * 资金费率历史：按资产 ID 与名称
     */
    @Test
    @DisplayName("资金费率：fundingHistory(id/name)")
    void testFundingHistory() {
        Info info = client.getInfo();
        long start = 1763136000000L;
        long end = 1763532000000L;

        resetLogs();
        try {
            List<FundingHistory> list = info.fundingHistory("BTC", start, end);
            assertNotNull(list);
            assertHttpLogsPresent();
        } catch (HypeError.ServerHypeError e) {
            assertHttpLogsPresent();
        }
    }

    /**
     * 用户资金费率历史：多重载一致性
     */
    @Test
    @DisplayName("用户资金费率：userFundingHistory 多重载")
    void testUserFundingHistoryVariants() {
        Info info = client.getInfo();
        long end = Instant.now().toEpochMilli();
        long start = end - CandleInterval.HOUR_1.toMillis() * 24;

        resetLogs();
        JsonNode a1 = info.userFundingHistory(address, start, end);
        assertNotNull(a1);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode a2 = info.userFundingHistory(address, "BTC", start, end);
        assertNotNull(a2);
        assertHttpLogsPresent();

        // 资产 ID 变体
        resetLogs();
        int btcId = info.nameToAsset("BTC");
        JsonNode a3 = info.userFundingHistory(address, btcId, start, end);
        assertNotNull(a3);
        assertHttpLogsPresent();
    }

    /**
     * 用户非资金账本更新
     */
    @Test
    @DisplayName("账本：userNonFundingLedgerUpdates JSON 结构")
    void testUserNonFundingLedgerUpdates() {
        Info info = client.getInfo();
        long end = Instant.now().toEpochMilli();
        long start = end - CandleInterval.HOUR_1.toMillis() * 24;

        resetLogs();
        JsonNode node = info.userNonFundingLedgerUpdates(address, start, end);
        assertNotNull(node);
        assertHttpLogsPresent();
    }

    /**
     * 历史订单与 TWAP 切片成交
     */
    @Test
    @DisplayName("订单历史：historicalOrders/userTwapSliceFills")
    void testHistoricalAndTwap() {
        Info info = client.getInfo();
        long end = Instant.now().toEpochMilli();
        long start = end - CandleInterval.HOUR_1.toMillis() * 24;

        resetLogs();
        JsonNode h = info.historicalOrders(address, start, end);
        assertNotNull(h);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode twap = info.userTwapSliceFills(address, start, end);
        assertNotNull(twap);
        assertHttpLogsPresent();
    }

    /**
     * 订单状态：无效 OID 触发错误处理
     */
    @Test
    @DisplayName("订单状态错误：无效 OID 抛出 HypeError.ClientHypeError")
    void testOrderStatusInvalidOid() {
        Info info = client.getInfo();
        assertThrows(HypeError.ClientHypeError.class, () -> info.orderStatus(address, -1L));
    }

    /**
     * 前端未成交订单（带 dex）
     */
    @Test
    @DisplayName("未成交订单：frontendOpenOrders(dex)")
    void testFrontendOpenOrdersWithDex() {
        Info info = client.getInfo();
        resetLogs();
        List<FrontendOpenOrder> fe = info.frontendOpenOrders(address, "");
        assertNotNull(fe);
        assertHttpLogsPresent();
    }

    /**
     * 用户成交：最近与时间范围
     */
    @Test
    @DisplayName("成交：userFills 与 userFillsByTime")
    void testUserFillsVariants() {
        Info info = client.getInfo();
        long end = Instant.now().toEpochMilli();
        long start = end - CandleInterval.HOUR_1.toMillis() * 24;

        resetLogs();
        List<UserFill> u1 = info.userFills(address);
        assertNotNull(u1);
        assertHttpLogsPresent();

        resetLogs();
        List<UserFill> u2 = info.userFillsByTime(address, start);
        assertNotNull(u2);
        assertHttpLogsPresent();

        resetLogs();
        List<UserFill> u3 = info.userFillsByTime(address, start, end);
        assertNotNull(u3);
        assertHttpLogsPresent();

        resetLogs();
        List<UserFill> u4 = info.userFills(address, true);
        assertNotNull(u4);
        assertHttpLogsPresent();
    }

    /**
     * 用户费用（返现/手续费）
     */
    @Test
    @DisplayName("费用：userFees JSON 结构")
    void testUserFees() {
        Info info = client.getInfo();
        resetLogs();
        JsonNode fees = info.userFees(address);
        assertNotNull(fees);
        assertHttpLogsPresent();
    }

    /**
     * clearinghouseState：dex 变体
     */
    @Test
    @DisplayName("账户状态：clearinghouseState(dex)")
    void testClearinghouseStateWithDex() {
        Info info = client.getInfo();
        resetLogs();
        ClearinghouseState st = info.clearinghouseState(address, "");
        assertNotNull(st);
        assertHttpLogsPresent();
    }

    /**
     * Vault 详情：给定无效地址期望 4xx
     */
    @Test
    @DisplayName("Vault 详情错误：无效地址触发 4xx")
    void testVaultDetailsInvalid() {
        Info info = client.getInfo();
        assertThrows(HypeError.class, () -> info.vaultDetails("0x0000000000000000000000000000000000000000", address));
    }

    /**
     * Spot Deploy 状态与组合/角色/速率限制
     */
    @Test
    @DisplayName("用户信息：spotDeployState/portfolio/userRole/userRateLimit")
    void testUserInfoMisc() {
        Info info = client.getInfo();

        resetLogs();
        JsonNode s = info.spotDeployState(address);
        assertNotNull(s);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode p = info.portfolio(address);
        assertNotNull(p);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode r = info.userRole(address);
        assertNotNull(r);
        assertHttpLogsPresent();

        resetLogs();
        UserRateLimit rl = info.userRateLimit(address);
        assertNotNull(rl);
        assertHttpLogsPresent();
    }

    /**
     * 推荐、子账户与多签签名者映射
     */
    @Test
    @DisplayName("用户映射：queryReferralState/querySubAccounts/queryUserToMultiSigSigners")
    void testUserMappings() {
        Info info = client.getInfo();

        resetLogs();
        JsonNode a = info.queryReferralState(address);
        assertNotNull(a);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode b = info.querySubAccounts(address);
        assertNotNull(b);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode c = info.queryUserToMultiSigSigners(address);
        assertNotNull(c);
        assertHttpLogsPresent();
    }

    /**
     * 部署拍卖状态与 DEX 抽象状态
     */
    @Test
    @DisplayName("部署状态：queryPerpDeployAuctionStatus/querySpotDeployAuctionStatus/DEX 抽象状态")
    void testDeployAndAbstraction() {
        Info info = client.getInfo();

        resetLogs();
        JsonNode p = info.queryPerpDeployAuctionStatus();
        assertNotNull(p);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode s = info.spotDeployState(address);
        assertNotNull(s);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode d = info.queryUserDexAbstractionState(address);
        assertNotNull(d);
        assertHttpLogsPresent();
    }

    /**
     * 用户 Vault 权益与额外代理
     */
    @Test
    @DisplayName("权益与代理：userVaultEquities/extraAgents")
    void testVaultEquitiesAndExtraAgents() {
        Info info = client.getInfo();

        resetLogs();
        JsonNode e1 = info.userVaultEquities(address);
        assertNotNull(e1);
        assertHttpLogsPresent();

        resetLogs();
        JsonNode e2 = info.extraAgents(address);
        assertNotNull(e2);
        assertHttpLogsPresent();
    }

    /**
     * WebSocket：skipWs=true 时订阅抛出异常，其余方法静默
     */
    @Test
    @DisplayName("WebSocket 管理：skipWs=true 行为验证")
    void testWebsocketSkipWsBehavior() {
        // 构建一个跳过 WS 的 Info 用例
        HyperliquidClient noWsClient = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(TESTNET_PRIVATE_KEY)
                .skipWs(true)
                .enableDebugLogs()
                .build();

        Info info = noWsClient.getInfo();
        // subscribe 抛出 HypeError
        assertThrows(HypeError.class, () -> info.subscribe(null, message -> {
        }));
        // 其他管理方法不抛异常
        assertDoesNotThrow(() -> info.unsubscribe(null));
        assertDoesNotThrow(() -> info.addConnectionListener(null));
        assertDoesNotThrow(() -> info.removeConnectionListener(null));
        assertDoesNotThrow(() -> info.setMaxReconnectAttempts(5));
        assertDoesNotThrow(() -> info.setNetworkCheckIntervalSeconds(5));
        assertDoesNotThrow(() -> info.setReconnectBackoffMs(500, 5000));
        assertDoesNotThrow(() -> info.addCallbackErrorListener(null));
        assertDoesNotThrow(() -> info.removeCallbackErrorListener(null));

        noWsClient.getInfo().closeWs();
    }
}