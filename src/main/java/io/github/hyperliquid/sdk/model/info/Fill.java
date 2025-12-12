package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashMap;
import java.util.Map;

/**
 * User trade entity wrapper.
 *
 * <p>Note: Official return fields may vary across different interfaces and versions, while preserving complete information for SDK users.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fill {

    /**
     * Execution timestamp (milliseconds)
     */
    private Long time;
    /**
     * Currency ID (perp/spot, if server returns as integer ID)
     */
    private Integer coin;
    /**
     * Currency name (if server returns as string name, e.g., "AVAX")
     */
    private String coinName;
    /**
     * true for buy, false for sell
     */
    private Boolean isBuy;
    /**
     * Execution quantity
     */
    private Double size;
    /**
     * Execution price
     */
    private Double price;

    /**
     * Extra fields container (for compatibility with different return structures)
     */
    private Map<String, Object> extensions = new HashMap<>();

    public Fill() {
    }

    // Getter and Setter methods
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Integer getCoin() {
        return coin;
    }

    public void setCoin(Integer coin) {
        this.coin = coin;
    }

    public String getCoinName() {
        return coinName;
    }

    public void setCoinName(String coinName) {
        this.coinName = coinName;
    }

    public Boolean getIsBuy() {
        return isBuy;
    }

    public void setIsBuy(Boolean isBuy) {
        this.isBuy = isBuy;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    /**
     * Builder builder, for friendly creation.
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
     * Compatibility coin mapping: supports both integer ID and string name.
     *
     * @param value server returned coin field value (may be number or string)
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
            // Compatible with string and other types
            this.coin = null;
            this.coinName = String.valueOf(value);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return extensions;
    }
}