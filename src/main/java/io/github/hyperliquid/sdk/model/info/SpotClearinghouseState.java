package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 现货清算所状态封装。
 *
 * <p>结构同样复杂多变，采用扩展容器策略保留完整信息。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotClearinghouseState {
    /** 额外字段容器 */
    private Map<String, Object> extensions = new HashMap<>();

    public SpotClearinghouseState() {}

    /** Builder 构建器 */
    public static class Builder {
        private final SpotClearinghouseState s = new SpotClearinghouseState();
        public Builder putExtra(String k, Object v) { s.extensions.put(k, v); return this; }
        public SpotClearinghouseState build() { return s; }
    }

    @JsonAnySetter
    public void put(String key, Object value) { extensions.put(key, value); }

    @JsonAnyGetter
    public Map<String, Object> any() { return extensions; }

    /** 获取扩展映射 */
    public Map<String, Object> getExtensions() { return extensions; }
    /** 设置扩展映射 */
    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }
}
