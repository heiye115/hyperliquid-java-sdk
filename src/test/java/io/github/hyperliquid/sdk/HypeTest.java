package io.github.hyperliquid;

import io.github.hyperliquid.sdk.client.HypeHttpClient;
import io.github.hyperliquid.sdk.client.InfoClient;
import io.github.hyperliquid.sdk.model.info.Candle;
import io.github.hyperliquid.sdk.model.info.CandleInterval;
import io.github.hyperliquid.sdk.model.info.Meta;
import io.github.hyperliquid.sdk.model.info.SpotMeta;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试类用于验证nameToCoin和coinToAsset映射功能
 * 基于Python SDK中的name_to_coin和coin_to_asset方法实现
 * 使用实际API接口获取数据，移除模拟数据
 */
public class HypeTest {

    // 线程安全的映射存储，模仿Python SDK中的name_to_coin和coin_to_asset
    private static final ConcurrentHashMap<String, String> nameToCoinMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> coinToAssetMap = new ConcurrentHashMap<>();

    // API客户端实例
    private InfoClient infoClient;

    // 基础URL - 使用Hyperliquid主网API
    private static final String BASE_URL = "https://api.hyperliquid.xyz";

    /**
     * 在每个测试之前运行，确保测试环境的清洁和一致性。
     * 这将清除所有映射并使用回退数据重新初始化，以隔离测试。
     */
    @BeforeEach
    public void setUp() {
        // 为每个测试初始化API客户端
        initializeApiClient();
        initializeMappings();
    }

    /**
     * 初始化API客户端
     */
    private void initializeApiClient() {
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();

            HypeHttpClient hypeHttpClient = new HypeHttpClient(BASE_URL, okHttpClient);
            infoClient = new InfoClient(BASE_URL, hypeHttpClient, true); // skipWs=true for testing
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize API client: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化nameToCoin和coinToAsset映射
     * 从实际API获取现货资产和永续合约资产的映射关系
     */
    private void initializeMappings() {
        try {
            System.out.println("Starting API calls for mapping initialization...");

            Meta perpMeta = infoClient.meta();
            SpotMeta spotMeta = infoClient.spotMeta();

            if (perpMeta != null && perpMeta.getUniverse() != null && !perpMeta.getUniverse().isEmpty()) {
                System.out.println("Processing perp universe size: " + perpMeta.getUniverse().size());
                for (int assetId = 0; assetId < perpMeta.getUniverse().size(); assetId++) {
                    Meta.Universe universe = perpMeta.getUniverse().get(assetId);
                    if (universe != null && universe.getName() != null) {
                        String coinName = universe.getName();
                        nameToCoinMap.put(universe.getName() + "-PERP", coinName);
                        nameToCoinMap.put(coinName, coinName);
                        coinToAssetMap.put(coinName, assetId);
                        if (coinName.equals("BTC")) {
                            System.out.println("Mapped BTC perp with assetId: " + assetId);
                        }
                    }
                }
            }

            if (spotMeta != null && spotMeta.getUniverse() != null && !spotMeta.getUniverse().isEmpty()) {
                System.out.println("Processing spot universe size: " + spotMeta.getUniverse().size());
                for (int i = 0; i < spotMeta.getUniverse().size(); i++) {
                    SpotMeta.Universe universe = spotMeta.getUniverse().get(i);
                    if (universe != null && universe.getName() != null) {
                        String coinName = universe.getName();
                        int assetId = 10000 + i;
                        nameToCoinMap.put(universe.getName() + "/USD", coinName);
                        nameToCoinMap.put(coinName, coinName);
                        coinToAssetMap.put(coinName, assetId);
                    }
                }
            }

            if (coinToAssetMap.isEmpty()) {
                System.err.println("Mappings are empty after API processing, falling back.");

            }

        } catch (Exception e) {
            System.err.println("An exception occurred, falling back to fallback mappings.");

        }
    }


    /**
     * 根据名称获取对应的币种
     * 模仿Python SDK中的name_to_coin映射功能
     *
     * @param name 资产名称或交易对名称
     * @return 对应的币种名称，如果不存在返回Optional.empty()
     */
    public static Optional<String> nameToCoin(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nameToCoinMap.get(name));
    }

    /**
     * 根据币种获取对应的资产ID
     * 模仿Python SDK中的coin_to_asset映射功能
     *
     * @param coin 币种名称
     * @return 对应的资产ID，如果不存在返回Optional.empty()
     */
    public static Optional<Integer> coinToAsset(String coin) {
        if (coin == null || coin.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(coinToAssetMap.get(coin));
    }

    /**
     * 获取名称对应的资产ID（组合方法）
     * 先通过nameToCoin获取币种，再通过coinToAsset获取资产ID
     *
     * @param name 资产名称或交易对名称
     * @return 对应的资产ID，如果任何一步失败返回Optional.empty()
     */
    public static Optional<Integer> nameToAsset(String name) {
        return nameToCoin(name).flatMap(HypeTest::coinToAsset);
    }

    // 单元测试部分

    /**
     * 测试nameToCoin方法的功能正确性
     */
    @Test
    public void testNameToCoin() {
        setUp();
        Optional<String> coin = nameToCoin("BTC");
        System.out.println(coin.get());

    }

    /**
     * 测试coinToAsset方法的功能正确性
     */
    @Test
    public void testCoinToAsset() {
        // 测试存在的映射
        assertEquals(Integer.valueOf(10000), coinToAsset("btc").orElse(null));
        assertEquals(Integer.valueOf(10001), coinToAsset("eth").orElse(null));
        assertEquals(Integer.valueOf(10002), coinToAsset("sol").orElse(null));

        // 测试不存在的币种
        assertFalse(coinToAsset("NONEXISTENT").isPresent());

        // 测试空值和空字符串
        assertFalse(coinToAsset(null).isPresent());
        assertFalse(coinToAsset("").isPresent());
        assertFalse(coinToAsset("   ").isPresent());
    }

    /**
     * 测试nameToAsset组合方法的功能正确性
     */
    @Test
    public void testNameToAsset() {
        // 测试完整的映射链
        assertEquals(Integer.valueOf(10000), nameToAsset("BTC/USD").orElse(null));
        assertEquals(Integer.valueOf(10001), nameToAsset("ETH/USD").orElse(null));
        assertEquals(Integer.valueOf(10002), nameToAsset("SOL/USD").orElse(null));

        // 测试不存在的名称
        assertFalse(nameToAsset("NONEXISTENT").isPresent());

        // 测试空值和空字符串
        assertFalse(nameToAsset(null).isPresent());
        assertFalse(nameToAsset("").isPresent());
    }

    /**
     * 测试线程安全性
     * 多个线程同时访问映射不应出现数据竞争
     */
    @Test
    public void testThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    assertTrue(nameToCoin("BTC/USD").isPresent());
                    assertTrue(coinToAsset("btc").isPresent());
                    assertTrue(nameToAsset("BTC/USD").isPresent());
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    /**
     * 提供K线快照测试的参数
     */
    private static Stream<Arguments> candleSnapshotByCountProvider() {
        return Stream.of(
                Arguments.of("BTC", CandleInterval.MINUTE_1, 10),
                Arguments.of("ETH", CandleInterval.HOUR_1, 20),
                Arguments.of("BTC", CandleInterval.DAY_1, 5));
    }

    /**
     * 测试获取K线快照的功能
     */
    @ParameterizedTest
    @MethodSource("candleSnapshotByCountProvider")
    @DisplayName("测试 candleSnapshotByCount 方法获取现货K线数据")
    public void testCandleSnapshotByCount(String coin, CandleInterval interval, int count) {
        // 1. 调用API
        List<Candle> candles = infoClient.candleSnapshotByCount(coin, interval, count);

        // 2. 验证返回数据
        assertNotNull(candles, "K线数据列表不应为null");
        assertEquals(count, candles.size(), "返回的K线数量应与请求的数量一致");

        for (Candle candle : candles) {
            assertNotNull(candle, "列表中的K线对象不应为null");
            assertEquals(coin, candle.getSymbol(), "K线的交易对符号应与请求一致");
            assertEquals(interval.getCode(), candle.getInterval(), "K线的时间间隔应与请求一致");

            // 验证关键字段不为null
            assertNotNull(candle.getStartTimestamp(), "起始时间戳不应为null");
            assertNotNull(candle.getOpenPrice(), "开盘价不应为null");
            assertNotNull(candle.getHighPrice(), "最高价不应为null");
            assertNotNull(candle.getLowPrice(), "最低价不应为null");
            assertNotNull(candle.getClosePrice(), "收盘价不应为null");
            assertNotNull(candle.getVolume(), "交易量不应为null");

            // 验证数据合理性
            assertTrue(candle.getStartTimestamp() > 0, "起始时间戳应为正数");

            BigDecimal open = new BigDecimal(candle.getOpenPrice());
            BigDecimal high = new BigDecimal(candle.getHighPrice());
            BigDecimal low = new BigDecimal(candle.getLowPrice());
            BigDecimal close = new BigDecimal(candle.getClosePrice());
            BigDecimal volume = new BigDecimal(candle.getVolume());

            assertTrue(open.compareTo(BigDecimal.ZERO) >= 0, "开盘价应为非负数");
            assertTrue(high.compareTo(BigDecimal.ZERO) >= 0, "最高价应为非负数");
            assertTrue(low.compareTo(BigDecimal.ZERO) >= 0, "最低价应为非负数");
            assertTrue(close.compareTo(BigDecimal.ZERO) >= 0, "收盘价应为非负数");
            assertTrue(volume.compareTo(BigDecimal.ZERO) >= 0, "交易量应为非负数");

            assertTrue(high.compareTo(low) >= 0, "最高价必须大于或等于最低价");
        }
    }
}
