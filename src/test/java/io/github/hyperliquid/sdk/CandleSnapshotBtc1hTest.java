package io.github.hyperliquid.sdk;


import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.model.info.Candle;
import io.github.hyperliquid.sdk.utils.Error;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 示例测试：使用 Info.candleSnapshot 接口获取 BTC 1 小时 K 线。
 *
 * 说明：
 * - 为了直接得到类型安全的返回，本示例使用 SDK 提供的 candleSnapshotTyped 方法；
 * - 示例会自动解析 "BTC" 的整数 coinId，并在最近 24 小时窗口内拉取 1h K 线；
 * - 断言包括：返回列表非空、间隔标识为 "1h"、时间戳周期约 1 小时、币种名称为 "BTC"。
 */
public class CandleSnapshotBtc1hTest {

    /**
     * 将资产 ID 转换为请求所需的 coinId。
     *
     * 约定：
     * - perp（合约）资产的 assetId 与 coinId 相同（为 meta.universe 的枚举下标）；
     * - spot（现货）资产的 assetId 为 index + 10000，因此 coinId = assetId - 10000。
     *
     * @param info Info 客户端
     * @param name 币种名称（如 "BTC"）
     * @return 对应的整数 coinId
     */
    private int resolveCoinId(Info info, String name) {
        // 首选：使用 meta.assets 的 coin 字段（若可用），否则回退到 universe 枚举下标约定
        com.fasterxml.jackson.databind.JsonNode meta = info.meta();
        if (meta != null && meta.has("assets") && meta.get("assets").isArray()) {
            com.fasterxml.jackson.databind.JsonNode assets = meta.get("assets");
            for (int i = 0; i < assets.size(); i++) {
                com.fasterxml.jackson.databind.JsonNode a = assets.get(i);
                String nm = a.has("name") ? a.get("name").asText() : ("ASSET_" + i);
                if (name.equalsIgnoreCase(nm) && a.has("coin")) {
                    return a.get("coin").asInt();
                }
            }
        }
        // 回退方案：通过 SDK 的约定映射计算 coinId
        int assetId = info.nameToAsset(name);
        return assetId >= 10000 ? (assetId - 10000) : assetId;
    }

    /**
     * 测试：获取 BTC 最近 24 小时的 1h K 线，并校验基本字段。
     */
    //@Disabled("集成示例：依赖外部 API，默认跳过。删除 @Disabled 后再运行。")
    @Test
    public void testCandleSnapshotBTC1H() {
        // 1) 构造 Info 客户端（跳过 WS，便于在 CI 或示例中运行）
        Info info = new Info("https://api.hyperliquid.xyz", 10, true);

        // 2) 解析 BTC 的 coinId（适配 perp/spot 两类映射）
        int coinId = resolveCoinId(info, "BTC");

        // 3) 计算时间窗口：最近 24 小时
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24L * 3_600_000L;

        // 4) 拉取 1h K 线（类型化返回）。若服务端不接受整数 coinId，则回退为字符串形式（如 "BTC"）。
        List<Candle> candles;
        try {
            candles = info.candleSnapshotTyped(coinId, "1h", startTime, endTime);
        } catch ( Error.ClientError e) {
            e.printStackTrace();
            // 兼容性回退：部分环境要求 coin 为字符串（如 "BTC" 或 "@107"）
            System.out.println("candleSnapshotTyped(coinId) 失败，尝试使用 coinName：" + e.getMessage());
            try {
                candles = info.candleSnapshotTyped("BTC", "1h", startTime, endTime);
            } catch ( Error.ClientError e2) {
                e2.printStackTrace();
                System.out.println(
                        "candleSnapshotTyped(coinName) 仍失败，尝试使用 candlesSnapshot(intervalMs)：" + e2.getMessage());
                candles = info.candlesSnapshotTyped(coinId, 3_600_000L, startTime, endTime);
            }
        }

        // 5) 断言与示例输出
        Assertions.assertNotNull(candles, "返回结果不应为 null");
        Assertions.assertFalse(candles.isEmpty(), "K 线列表不应为空");

        Candle last = candles.get(candles.size() - 1);
        Assertions.assertEquals("1h", last.getI(), "间隔字符串应为 1h");
        Assertions.assertNotNull(last.getTStart(), "起始时间戳不应为 null");
        Assertions.assertNotNull(last.getT(), "结束时间戳不应为 null");
        Assertions.assertEquals("BTC", last.getS(), "币种名称应为 BTC");
        //打印所有Candle last
        System.out.println(last.getT()+"," +
                last.getTStart()+","+
                last.getC()+","+
                last.getH()+","+
                last.getL()+","+
                last.getO()+","+
                last.getV()
        );



        long duration = last.getT() - last.getTStart();
        // 服务器端返回通常为 [start, end] 区间，end 可能为该周期结束时间或结束时间-1 毫秒，故使用近似判断
        Assertions.assertTrue(duration >= 3_599_000L && duration <= 3_600_000L,
                "K 线周期应约为 1 小时（允许 1 秒以内的边界差异）");

        // 如需观察示例输出，可取消下行注释：
        // System.out.printf("BTC 1h 最新K线: tStart=%d, tEnd=%d, o=%s, h=%s, l=%s, c=%s,
        // v=%s\n",
        // last.getTStart(), last.getT(), last.getO(), last.getH(), last.getL(),
        // last.getC(), last.getV());
    }
}
