package io.github.hyperliquid.sdk.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hyperliquid.sdk.model.info.Candle;
import io.github.hyperliquid.sdk.utils.Error;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Candle 解析工具类。
 *
 * 提供 JsonNode 到 Candle 列表/最新一条的解析封装，并在出现异常数据时给出清晰错误信息。
 */
public final class CandleParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CandleParser() {}

    /**
     * 将返回的 JsonNode 解析为 Candle 列表。
     *
     * @param node 服务端返回的 JSON 节点（应为数组）
     * @return Candle 列表（跳过 null 元素与非对象元素）
     * @throws Error 当返回不是数组或元素解析失败时抛出自定义错误
     */
    public static List<Candle> parseList(JsonNode node) throws Error {
        if (node == null || !node.isArray()) {
            throw new Error("Candle 列表解析失败：返回不是数组或为空");
        }
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode e = node.get(i);
            if (e == null || e.isNull()) {
                continue; // 跳过 null 元素
            }
            if (!e.isObject()) {
                // 为健壮性：跳过非对象元素
                continue;
            }
            try {
                Candle c = MAPPER.treeToValue(e, Candle.class);
                result.add(c);
            } catch (Exception ex) {
                throw new Error("Candle 反序列化失败（索引=" + i + "): " + ex.getMessage());
            }
        }
        return result;
    }

    /**
     * 解析最新一条 Candle（列表的最后一个元素）。
     *
     * @param node 服务端返回的 JSON 节点（应为数组）
     * @return 最新一条 Candle；若为空则返回 Optional.empty()
     * @throws Error 当返回不是数组或元素解析失败时抛出自定义错误
     */
    public static Optional<Candle> parseLatest(JsonNode node) throws Error {
        List<Candle> list = parseList(node);
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(list.size() - 1));
    }
}

