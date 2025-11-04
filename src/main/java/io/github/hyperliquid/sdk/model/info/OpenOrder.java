package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * openOrders 返回的未成交订单实体。
 * 说明：
 * - 与 frontendOpenOrders 不同，openOrders 返回的字段更精简，coin 为字符串（如 "BTC" 或 "@107"）。
 * - 为保持兼容性，除已知字段外的其它返回内容会保存在 {@link #extensions} 中。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenOrder {

    /**
     * 币种名称或 Spot 索引（如 "BTC"、"@107"）
     */
    private String coin;
    /**
     * 限价，字符串形式，例如 "29792.0"
     */
    private String limitPx;
    /**
     * 订单 ID
     */
    private Long oid;
    /**
     * 方向字符串（例如 "A"/"B"、或 "Buy"/"Sell" 等，多端可能不同），保持原样
     */
    private String side;
    /**
     * 订单数量，字符串形式
     */
    private String sz;
    /**
     * 创建时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 额外字段容器（用于兼容不同返回结构）
     */
    private Map<String, Object> extensions = new HashMap<>();

    public OpenOrder() {
    }

    /**
     * Builder 便于友好构建
     */
    public static class Builder {
        private final OpenOrder o = new OpenOrder();

        public Builder coin(String v) {
            o.coin = v;
            return this;
        }

        public Builder limitPx(String v) {
            o.limitPx = v;
            return this;
        }

        public Builder oid(Long v) {
            o.oid = v;
            return this;
        }

        public Builder side(String v) {
            o.side = v;
            return this;
        }

        public Builder sz(String v) {
            o.sz = v;
            return this;
        }

        public Builder timestamp(Long v) {
            o.timestamp = v;
            return this;
        }

        public Builder putExtra(String k, Object v) {
            o.extensions.put(k, v);
            return this;
        }

        public OpenOrder build() {
            return o;
        }
    }

    @JsonAnySetter
    public void put(String key, Object value) {
        extensions.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return extensions;
    }

    // Getter / Setter

    /**
     * 获取币种（如 "BTC" 或 "@107"）
     */
    public String getCoin() {
        return coin;
    }

    /**
     * 设置币种（如 "BTC" 或 "@107"）
     */
    public void setCoin(String coin) {
        this.coin = coin;
    }

    /**
     * 获取限价（字符串）
     */
    public String getLimitPx() {
        return limitPx;
    }

    /**
     * 设置限价（字符串）
     */
    public void setLimitPx(String limitPx) {
        this.limitPx = limitPx;
    }

    /**
     * 获取订单 ID
     */
    public Long getOid() {
        return oid;
    }

    /**
     * 设置订单 ID
     */
    public void setOid(Long oid) {
        this.oid = oid;
    }

    /**
     * 获取方向字符串
     */
    public String getSide() {
        return side;
    }

    /**
     * 设置方向字符串
     */
    public void setSide(String side) {
        this.side = side;
    }

    /**
     * 获取订单数量（字符串）
     */
    public String getSz() {
        return sz;
    }

    /**
     * 设置订单数量（字符串）
     */
    public void setSz(String sz) {
        this.sz = sz;
    }

    /**
     * 获取时间戳（毫秒）
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * 设置时间戳（毫秒）
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 获取扩展字段映射
     */
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /**
     * 设置扩展字段映射
     */
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
}

