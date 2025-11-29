package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PerpDexStatus 类型化模型。
 *
 * <p>
 * 文档示例：{"totalNetDeposit": "4103492112.4478230476"}
 * 该模型保留扩展字段以兼容未来返回结构。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerpDexStatus {

    /**
     * 总净充值/提现（字符串）
     */
    private String totalNetDeposit;

    /**
     * 其他未知/扩展字段
     */
    private Map<String, Object> extensions = new LinkedHashMap<>();

    // Getter and Setter methods
    public String getTotalNetDeposit() {
        return totalNetDeposit;
    }

    public void setTotalNetDeposit(String totalNetDeposit) {
        this.totalNetDeposit = totalNetDeposit;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    /**
     * 获取扩展字段
     */
    @JsonAnyGetter
    public Map<String, Object> any() {
        return extensions;
    }

    /**
     * 设置扩展字段
     */
    @JsonAnySetter
    public void setExtensions(String key, Object value) {
        if (this.extensions == null) {
            this.extensions = new LinkedHashMap<>();
        }
        this.extensions.put(key, value);
    }
}