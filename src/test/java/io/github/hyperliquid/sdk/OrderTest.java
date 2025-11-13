package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.utils.Constants;
import org.junit.jupiter.api.Assertions;

import java.util.Map;

/**
 * 订单下单与平仓逻辑综合测试。
 */
public class OrderTest {

    /**
     * 私钥（测试网）
     */
    String privateKey = "your_testnet_private_key_here";

    /**
     * 统一客户端（测试网 + 启用调试日志）
     */
    HyperliquidClient client = HyperliquidClient.builder()
            .baseUrl(Constants.TESTNET_API_URL)
            .addPrivateKey(privateKey)
            .enableDebugLogs()
            .build();

    /**
     * 获取币种当前中间价。
     * <p>
     * 设计考虑：
     * - 限价/触发价选择需要参考当前行情中位数，避免写死；
     * - 返回 double 以便后续进行百分比偏移。
     */
    private double mid(String symbol) {
        Map<String, String> mids = client.getInfo().allMids();
        String px = mids.get(symbol);
        Assertions.assertNotNull(px, "mid price not found for " + symbol);
        return Double.parseDouble(px);
    }


}
