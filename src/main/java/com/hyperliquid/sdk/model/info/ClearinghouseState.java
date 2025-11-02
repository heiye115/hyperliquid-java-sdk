package com.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 永续清算所状态封装。
 *
 * <p>由于清算所返回结构复杂且可能迭代，此类仅定义通用扩展容器，
 * 以最小约束方式提供类型安全与兼容性。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClearinghouseState {
    /** 额外字段容器（完整返回结构保留于此） */
    private Map<String, Object> extensions = new HashMap<>();

    public ClearinghouseState() {}

    /** Builder 构建器 */
    public static class Builder {
        private final ClearinghouseState s = new ClearinghouseState();
        public Builder putExtra(String k, Object v) { s.extensions.put(k, v); return this; }
        public ClearinghouseState build() { return s; }
    }

    @JsonAnySetter
    public void put(String key, Object value) { extensions.put(key, value); }

    @JsonAnyGetter
    public Map<String, Object> any() { return extensions; }

    /** 获取完整扩展映射 */
    public Map<String, Object> getExtensions() { return extensions; }
    /** 设置完整扩展映射 */
    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }
}
