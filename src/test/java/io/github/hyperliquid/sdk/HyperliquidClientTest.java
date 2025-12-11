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
 * HyperliquidClient test class
 * Uses JUnit 5 to comprehensively test all methods under client.getInfo().
 * Includes: successful call validation, return structure assertions, error handling, and log output verification.
 */
public class HyperliquidClientTest {

    /**
     * Testnet private key (for testing only)
     */
    private static final String TESTNET_PRIVATE_KEY = "your_testnet_private_key_here";

    /**
     * Client under test
     */
    private HyperliquidClient client;

    /**
     * Current test address
     */
    private String address;

    /**
     * Capture slf4j-simple's standard error output for log verification
     */
    private ByteArrayOutputStream errContent;

    /**
     * Backup original System.err
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
     * Verify account information retrieval functionality: clearinghouse state and user state.
     */
    @Test
    @DisplayName("Account info: clearinghouseState/userState return structure and logs")
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
     * Verify market data retrieval functionality: meta, allMids, l2Book, candleSnapshotLatest.
     */
    @Test
    @DisplayName("Market data: meta/allMids/l2Book/candleSnapshotLatest")
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
     * Verify open orders query functionality: openOrders and frontendOpenOrders.
     */
    @Test
    @DisplayName("Open orders: openOrders/frontendOpenOrders list structure")
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
     * Verify position information query functionality: ClearinghouseState.assetPositions field.
     */
    @Test
    @DisplayName("Position info: ClearinghouseState.assetPositions field validation")
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
     * Metadata and cache: meta(String), loadMetaCache
     */
    @Test
    @DisplayName("Metadata: meta(dex)/loadMetaCache cache effectiveness")
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
     * Metadata and asset contexts: JSON and typed consistency
     */
    @Test
    @DisplayName("Metadata: metaAndAssetCtxs JSON/typed consistency")
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
     * Spot metadata: spotMeta and spotMetaAndAssetCtxs
     */
    @Test
    @DisplayName("Spot metadata: spotMeta/spotMetaAndAssetCtxs")
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
     * perpDexs: JSON and typed
     */
    @Test
    @DisplayName("Perpetual DEX list: perpDexs/perpDexsTyped")
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
     * perpDexStatus: JSON and typed
     */
    @Test
    @DisplayName("Perpetual DEX status: perpDexStatus/perpDexStatusTyped")
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
     * openOrders: dex variant
     */
    @Test
    @DisplayName("Open orders: openOrders(dex)")
    void testOpenOrdersWithDex() {
        Info info = client.getInfo();
        resetLogs();
        List<OpenOrder> o = info.openOrders(address, "");
        assertNotNull(o);
        assertHttpLogsPresent();
    }

    /**
     * allMids: default and specified dex
     */
    @Test
    @DisplayName("Mid prices: allMids default and specified dex")
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
     * L2 order book: aggregation parameters
     */
    @Test
    @DisplayName("Order book: l2Book aggregation parameters validity")
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
     * Candles: range and count
     */
    @Test
    @DisplayName("Candles: candleSnapshot range and count")
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
     * Candles: invalid count should throw exception
     */
    @Test
    @DisplayName("Candle error: count<=0 throws HypeError")
    void testCandleSnapshotByCountInvalid() {
        Info info = client.getInfo();
        assertThrows(HypeError.class, () -> info.candleSnapshotByCount("BTC", CandleInterval.MINUTE_1, 0));
    }

    /**
     * Name to asset ID mapping: unknown name should throw exception
     */
    @Test
    @DisplayName("Name mapping error: unknown coin throws HypeError")
    void testNameToAssetUnknown() {
        Info info = client.getInfo();
        info.loadMetaCache();
        assertThrows(HypeError.class, () -> info.nameToAsset("UNKNOWN_COIN_XYZ"));
    }

    /**
     * Funding rate history: by asset ID and name
     */
    @Test
    @DisplayName("Funding rates: fundingHistory(id/name)")
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
     * User funding rate history: multiple overload consistency
     */
    @Test
    @DisplayName("User funding rates: userFundingHistory multiple overloads")
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
     * User non-funding ledger updates
     */
    @Test
    @DisplayName("Ledger: userNonFundingLedgerUpdates JSON structure")
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
     * Historical orders and TWAP slice fills
     */
    @Test
    @DisplayName("Order history: historicalOrders/userTwapSliceFills")
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
     * Order status: invalid OID triggers error handling
     */
    @Test
    @DisplayName("Order status error: invalid OID throws HypeError.ClientHypeError")
    void testOrderStatusInvalidOid() {
        Info info = client.getInfo();
        assertThrows(HypeError.ClientHypeError.class, () -> info.orderStatus(address, -1L));
    }

    /**
     * Frontend open orders (with dex)
     */
    @Test
    @DisplayName("Open orders: frontendOpenOrders(dex)")
    void testFrontendOpenOrdersWithDex() {
        Info info = client.getInfo();
        resetLogs();
        List<FrontendOpenOrder> fe = info.frontendOpenOrders(address, "");
        assertNotNull(fe);
        assertHttpLogsPresent();
    }

    /**
     * User fills: recent and time range
     */
    @Test
    @DisplayName("Fills: userFills and userFillsByTime")
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
     * User fees (rebates/commissions)
     */
    @Test
    @DisplayName("Fees: userFees JSON structure")
    void testUserFees() {
        Info info = client.getInfo();
        resetLogs();
        JsonNode fees = info.userFees(address);
        assertNotNull(fees);
        assertHttpLogsPresent();
    }

    /**
     * clearinghouseState: dex variant
     */
    @Test
    @DisplayName("Account state: clearinghouseState(dex)")
    void testClearinghouseStateWithDex() {
        Info info = client.getInfo();
        resetLogs();
        ClearinghouseState st = info.clearinghouseState(address, "");
        assertNotNull(st);
        assertHttpLogsPresent();
    }

    /**
     * Vault details: invalid address expected to trigger 4xx
     */
    @Test
    @DisplayName("Vault details error: invalid address triggers 4xx")
    void testVaultDetailsInvalid() {
        Info info = client.getInfo();
        assertThrows(HypeError.class, () -> info.vaultDetails("0x0000000000000000000000000000000000000000", address));
    }

    /**
     * Spot Deploy status and portfolio/role/rate limit
     */
    @Test
    @DisplayName("User info: spotDeployState/portfolio/userRole/userRateLimit")
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
     * Referral, sub-accounts and multi-sig signer mappings
     */
    @Test
    @DisplayName("User mappings: queryReferralState/querySubAccounts/queryUserToMultiSigSigners")
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
     * Deployment auction status and DEX abstraction state
     */
    @Test
    @DisplayName("Deployment status: queryPerpDeployAuctionStatus/querySpotDeployAuctionStatus/DEX abstraction state")
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
     * User Vault equities and extra agents
     */
    @Test
    @DisplayName("Equities and agents: userVaultEquities/extraAgents")
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
 
}