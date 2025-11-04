package io.github.hyperliquid.sdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 客户端订单 ID（Cloid）表示，支持字符串形式。
 */
public class Cloid {
    
    private final String raw;

    /**
     * 创建 Cloid。
     *
     * @param raw 原始字符串（允许是纯数字字符串）
     */
    @JsonCreator
    public Cloid(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("Cloid cannot be null or empty");
        }
        this.raw = raw;
    }

    /**
     * 获取原始字符串表示。
     *
     * @return 原始字符串
     */
    @JsonValue
    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return raw;
    }
}
