import io.github.hyperliquid.sdk.info.*;
import io.github.hyperliquid.sdk.model.info.FrontendOpenOrder;
import io.github.hyperliquid.sdk.model.info.OpenOrder;
import io.github.hyperliquid.sdk.model.info.Fill;
import io.github.hyperliquid.sdk.model.info.ClearinghouseState;
import io.github.hyperliquid.sdk.model.info.SpotClearinghouseState;
import io.github.hyperliquid.sdk.model.info.Candle;

import java.util.List;
import java.util.Map;

/**
 * 进阶使用示例：演示在真实环境下调用多个 typed 方法进行综合查询。
 *
 * 说明：
 * - 需要网络访问官方 Info API。
 * - 该示例对 Fill.coin 字段做了兼容输出：若返回为字符串（如 "AVAX"），将输出 coinName；若为整数 ID，则输出 coin。
 * - 请根据需要替换 ownerAddress 为你的地址（示例使用 0x000... 占位，不会产生敏感信息）。
 *
 * 运行方式（Windows PowerShell）：
 * 1) 先复制依赖到 target/dependency
 * mvn -q dependency:copy-dependencies
 * 2) 编译示例（注意 classpath 引号与分号分隔）
 * javac -cp "target/classes;target/dependency/*"
 * examples/AdvancedUsageExample.java
 * 3) 运行示例（确保包含 examples 目录到 classpath）
 * java -cp ".;examples;target/classes;target/dependency/*" AdvancedUsageExample
 *
 * 预期输出：
 * - 打印 open orders 数量（可能为 0），示例将展示首条订单部分字段。
 * - 打印近期用户成交数量与若干成交明细（coinName 或 coin、方向、数量、价格、时间）。
 * - 打印清算所（perp/spot）状态的若干关键字段摘要或大小统计。
 * - 打印 perpDexs 数量统计。
 */
public class AdvancedUsageExample {
    public static void main(String[] args) {
        // 1. 初始化 Info 客户端
        // 注意：这里的 baseUrl 应为 API 根地址，不包含 "/info" 路径，
        // 因为客户端会在内部发送 POST 到 "/info"。
        final String baseUrl = "https://api.hyperliquid.xyz";
        Info info = new Info(baseUrl, 10, true);

        // 使用占位地址；你可以替换为自己的地址以获得真实数据
        final String ownerAddress = "0x0000000000000000000000000000000000000000";

        try {
            // 2. 查询前端未成交订单（typed）
            List<FrontendOpenOrder> openOrders = info.frontendOpenOrdersTyped(ownerAddress);
            System.out.println(
                    "[frontendOpenOrdersTyped] openOrders count: " + (openOrders == null ? 0 : openOrders.size()));
            if (openOrders != null && !openOrders.isEmpty()) {
                FrontendOpenOrder o = openOrders.get(0);
                // 打印关键信息：订单币种、买卖方向、限价与数量
                Integer coinId = o.getOrder() != null ? Integer.valueOf(o.getOrder().coin) : null;
                Boolean isBuy = o.getOrder() != null ? o.getOrder().isBuy : null;
                Double limitPx = null;
                Double size = null;
                if (o.getOrder() != null) {
                    try {
                        if (o.getOrder().limitPx != null) {
                            limitPx = Double.parseDouble(o.getOrder().limitPx);
                        }
                    } catch (Exception ignore) {
                    }
                    try {
                        if (o.getOrder().sz != null) {
                            size = Double.parseDouble(o.getOrder().sz);
                        }
                    } catch (Exception ignore) {
                    }
                }
                System.out.println(
                        "  first order: coinId=" + coinId + ", isBuy=" + isBuy + ", size=" + size + ", limitPx="
                                + limitPx);
            }

            // 2b. 查询基础 openOrders（typed，字段更精简，coin 通常为字符串）
            List<OpenOrder> openOrdersSimple = info.openOrdersTyped(ownerAddress);
            System.out.println(
                    "[openOrdersTyped] openOrders count: " + (openOrdersSimple == null ? 0 : openOrdersSimple.size()));
            if (openOrdersSimple != null && !openOrdersSimple.isEmpty()) {
                OpenOrder o = openOrdersSimple.get(0);
                System.out.println(String.format("  first order(simple): coin=%s, side=%s, sz=%s, limitPx=%s, ts=%d",
                        o.getCoin(), o.getSide(), o.getSz(), o.getLimitPx(), o.getTimestamp()));
            }

            // 3. 查询用户成交（typed）并输出兼容的 coin 字段
            List<Fill> fills = info.userFillsTyped(ownerAddress, false);
            System.out.println("[userFillsTyped] recent fills: " + (fills == null ? 0 : fills.size()));
            if (fills != null) {
                int show = Math.min(5, fills.size());
                for (int i = 0; i < show; i++) {
                    Fill f = fills.get(i);
                    String coinOut = f.getCoinName() != null ? f.getCoinName() : String.valueOf(f.getCoin());
                    System.out.println(String.format("  fill[%d]: coin=%s, isBuy=%s, size=%.6f, price=%.6f, time=%d",
                            i,
                            coinOut,
                            String.valueOf(f.getIsBuy()),
                            f.getSize() == null ? 0.0 : f.getSize(),
                            f.getPrice() == null ? 0.0 : f.getPrice(),
                            f.getTime() == null ? 0L : f.getTime()));
                }
            }

            // 3b. 通过时间窗口查询用户成交（typed）
            long now = System.currentTimeMillis();
            long oneDayAgo = now - 24L * 60L * 60L * 1000L;
            List<Fill> fillsByTime = info.userFillsByTimeTyped(ownerAddress, oneDayAgo, now, false);
            System.out.println("[userFillsByTimeTyped] fills in last 24h: "
                    + (fillsByTime == null ? 0 : fillsByTime.size()));

            // 4. 清算所（perp）状态（typed）
            ClearinghouseState ch = info.clearinghouseStateTyped(ownerAddress, "");
            if (ch != null) {
                // 示例输出：assets/positions 扩展字段统计与已知字段摘要
                Map<String, Object> ext = ch.getExtensions();
                System.out
                        .println("[clearinghouseStateTyped] extensions keys count: " + (ext == null ? 0 : ext.size()));
            } else {
                System.out.println("[clearinghouseStateTyped] result is null");
            }

            // 5. 现货清算所状态（typed）
            SpotClearinghouseState sch = info.spotClearinghouseStateTyped(ownerAddress);
            if (sch != null) {
                Map<String, Object> ext = sch.getExtensions();
                System.out.println(
                        "[spotClearinghouseStateTyped] extensions keys count: " + (ext == null ? 0 : ext.size()));
            } else {
                System.out.println("[spotClearinghouseStateTyped] result is null");
            }

            // 6. perpDexs（typed）数量统计（返回为对象列表，元素可能为 null）
            List<Map<String, Object>> perpDexs = info.perpDexsTyped();
            System.out.println("[perpDexsTyped] count: " + (perpDexs == null ? 0 : perpDexs.size()));

            // 7. K 线快照（类型化）：与文档一致的字符串间隔版本（BTC=0，最近 24 小时 15m）
            long endTimeDoc = System.currentTimeMillis();
            long startTimeDoc = endTimeDoc - 24L * 60L * 60L * 1000L;
            List<Candle> candlesDoc = info.candleSnapshotTyped(0, "15m", startTimeDoc, endTimeDoc);
            System.out.println(
                    "[candleSnapshotTyped] 15m 24h candles count: " + (candlesDoc == null ? 0 : candlesDoc.size()));
            if (candlesDoc != null && !candlesDoc.isEmpty()) {
                Candle last = candlesDoc.get(candlesDoc.size() - 1);
                System.out.println(String.format("  last candle: s=%s, i=%s, o=%s, h=%s, l=%s, c=%s, v=%s, T=%d",
                        last.getS(), last.getI(), last.getO(), last.getH(), last.getL(), last.getC(), last.getV(),
                        last.getT() == null ? 0L : last.getT()));
            }

            // 8. K 线快照（类型化）：兼容现有毫秒间隔版本（BTC=0，最近 24 小时 15m）
            List<Candle> candlesMs = info.candlesSnapshotTyped(0, 15 * 60 * 1000L, startTimeDoc, endTimeDoc);
            System.out.println(
                    "[candlesSnapshotTyped] 15m 24h candles count: " + (candlesMs == null ? 0 : candlesMs.size()));
        } catch (Exception e) {
            System.err.println("AdvancedUsageExample error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
