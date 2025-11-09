package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.model.info.Candle;
import io.github.hyperliquid.sdk.model.info.CandleInterval;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 示例测试：使用 Info.candleSnapshot 接口获取 BTC 1 小时 K 线。
 * <p>
 * 说明：
 * - 为了直接得到类型安全的返回，本示例使用 SDK 提供的 candleSnapshotTyped 方法；
 * - 示例会自动解析 "BTC" 的整数 coinId，并在最近 24 小时窗口内拉取 1h K 线；
 * - 断言包括：返回列表非空、间隔标识为 "1h"、时间戳周期约 1 小时、币种名称为 "BTC"。
 */
public class CandleSnapshotBtc1hTest {

    // 初始化 ExchangeManager，用于与 HyperLiquid 交易所交互。
    ExchangeManager manager = ExchangeManager.builder().build();

    /**
     * 测试：获取 BTC 最近 24 根 1 小时 K 线数据。
     */
    @Test
    public void testCandleSnapshotBTC1H() {
        List<Candle> candles = manager.getInfoClient().candleSnapshotByCount("BTC", CandleInterval.HOUR_1, 24);
        candles.forEach(System.out::println);
    }

}
