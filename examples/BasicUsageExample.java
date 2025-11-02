import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.info;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基础使用示例：演示如何创建 Info 客户端并进行核心接口调用。
 *
 * <p>本示例展示：</p>
 * - 初始化 Info 客户端（跳过 WebSocket，便于示例运行）
 * - 调用 allMids（类型化 Map 返回）并打印若干条中间价
 * - 调用 meta（返回 JSON）并打印 universe 项的大小
 * - 使用 dex 可选参数调用 allMids（默认空字符串代表第一个 perp dex）
 *
 * 预期输出：
 * - 打印 allMids 返回的币种数量与前若干条样例，如 BTC、ETH 的中间价
 * - 打印 meta.universe 的元素个数（若返回中包含该字段）
 */
public class BasicUsageExample {
    /**
     * 程序入口：执行基础查询示例。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // 1) 创建 Info 客户端（skipWs=true：示例中不使用 WebSocket）
        String baseUrl = "https://api.hyperliquid.xyz";
        Info info = new Info(baseUrl, 10, true);

        // 2) 查询所有中间价（类型化返回）
        Map<String, String> mids = info.allMidsTyped();
        System.out.println("[allMids] 币种数量: " + mids.size());

        // 打印前若干条中间价
        int printed = 0;
        for (Map.Entry<String, String> e : mids.entrySet()) {
            System.out.println("  " + e.getKey() + " -> " + e.getValue());
            printed++;
            if (printed >= 5) break; // 仅示例打印前 5 条
        }

        // 3) 查询 perp 元数据（meta）并检查 universe 大小
        JsonNode meta = info.meta();
        int universeSize = meta.has("universe") && meta.get("universe").isArray() ? meta.get("universe").size() : -1;
        System.out.println("[meta] universe 大小: " + universeSize);

        // 4) 使用 dex 可选参数进行 allMids 查询（空字符串表示第一个 perp dex；对齐官方约定）
        Map<String, String> midsDexDefault = info.allMidsTyped("");
        System.out.println("[allMids with dex=" + "\"\"" + "] 返回币种数量: " + midsDexDefault.size());

        // 5) 演示在 Map 中取值：例如获取 BTC 的中间价（若存在）
        String btcMid = mids.getOrDefault("BTC", "N/A");
        System.out.println("示例：BTC 中间价 = " + btcMid);

        // 附：将结果复制到自定义 Map（演示类型转换）
        Map<String, String> midsCopy = new LinkedHashMap<>(mids);
        System.out.println("[拷贝] midsCopy 大小: " + midsCopy.size());
    }
}

/*
运行与预期结果说明：

1) 构建与测试（确保依赖已下载并代码可编译）：
   mvn -q -DskipTests=false clean test

2) 拷贝依赖（便于用 javac/java 直接运行示例）：
   mvn -q dependency:copy-dependencies

3) 编译示例（Windows 环境；分号作为类路径分隔符）：
   javac -cp target/classes;target/dependency/* examples\\BasicUsageExample.java

4) 运行示例：
   java -cp .;target/classes;target/dependency/* examples.BasicUsageExample

预期输出（示意）：
   [allMids] 币种数量: 100+
     BTC -> 29792.0
     ETH -> 3500.0
     ...（仅示例，具体数值以实时行情为准）
   [meta] universe 大小: 100+
   [allMids with dex=""] 返回币种数量: 100+
   示例：BTC 中间价 = 29792.0
   [拷贝] midsCopy 大小: 100+
*/

