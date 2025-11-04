package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户成交实体封装。
 *
 * <p>说明：官方返回字段在不同接口与版本可能存在差异，同时为 SDK 使用者保留完整信息。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fill {

    /**
     * 成交时间戳（毫秒）
     */
    private Long time;
    /**
     * 币种 ID（perp/spot，若服务端返回为整数 ID）
     */
    private Integer coin;
    /**
     * 币种名称（若服务端返回为字符串名称，如 "AVAX"）
     */
    private String coinName;
    /**
     * 买入为 true，卖出为 false
     */
    private Boolean isBuy;
    /**
     * 成交数量
     */
    private Double size;
    /**
     * 成交价格
     */
    private Double price;

    /**
     * 额外字段容器（用于兼容不同返回结构）
     */
    private Map<String, Object> extensions = new HashMap<>();

    public Fill() {
    }

    /**
     * Builder 构建器，便于友好创建。
     */
    public static class Builder {
        private final Fill f = new Fill();

        public Builder time(Long t) {
            f.time = t;
            return this;
        }

        public Builder coin(Integer c) {
            f.coin = c;
            return this;
        }

        public Builder isBuy(Boolean b) {
            f.isBuy = b;
            return this;
        }

        public Builder size(Double s) {
            f.size = s;
            return this;
        }

        public Builder price(Double p) {
            f.price = p;
            return this;
        }

        public Builder putExtra(String k, Object v) {
            f.extensions.put(k, v);
            return this;
        }

        public Fill build() {
            return f;
        }
    }

    @JsonAnySetter
    public void put(String key, Object value) {
        extensions.put(key, value);
    }

    /**
     * 兼容性 coin 映射：同时支持整数 ID 与字符串名称。
     *
     * @param value 服务端返回的 coin 字段值（可能为数字或字符串）
     */
    @JsonSetter("coin")
    public void setCoinFlexible(Object value) {
        if (value == null) {
            this.coin = null;
            this.coinName = null;
            return;
        }
        if (value instanceof Number) {
            this.coin = ((Number) value).intValue();
            this.coinName = null;
        } else {
            // 兼容字符串与其他类型
            this.coin = null;
            this.coinName = String.valueOf(value);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return extensions;
    }

    // Getter / Setter（含方法注释）

    /**
     * 获取时间戳（毫秒）
     */
    public Long getTime() {
        return time;
    }

    /**
     * 设置时间戳（毫秒）
     */
    public void setTime(Long time) {
        this.time = time;
    }

    /**
     * 获取币种 ID（若返回为整数）
     */
    public Integer getCoin() {
        return coin;
    }

    /**
     * 设置币种 ID（仅当需要强制写入整数时使用）
     */
    public void setCoin(Integer coin) {
        this.coin = coin;
    }

    /**
     * 获取币种名称（若返回为字符串时）
     */
    public String getCoinName() {
        return coinName;
    }

    /**
     * 设置币种名称（仅当需要强制写入字符串时使用）
     */
    public void setCoinName(String coinName) {
        this.coinName = coinName;
    }

    /**
     * 是否买入
     */
    public Boolean getIsBuy() {
        return isBuy;
    }

    /**
     * 设置是否买入
     */
    public void setIsBuy(Boolean buy) {
        isBuy = buy;
    }

    /**
     * 获取成交数量
     */
    public Double getSize() {
        return size;
    }

    /**
     * 设置成交数量
     */
    public void setSize(Double size) {
        this.size = size;
    }

    /**
     * 获取成交价格
     */
    public Double getPrice() {
        return price;
    }

    /**
     * 设置成交价格
     */
    public void setPrice(Double price) {
        this.price = price;
    }

    /**
     * 获取额外字段映射
     */
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /**
     * 设置额外字段映射
     */
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
}
