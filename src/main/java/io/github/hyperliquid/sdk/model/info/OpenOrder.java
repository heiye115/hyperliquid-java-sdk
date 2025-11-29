package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * openOrders 返回的未成交订单实体。
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

    // Getter and Setter methods
    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public String getLimitPx() {
        return limitPx;
    }

    public void setLimitPx(String limitPx) {
        this.limitPx = limitPx;
    }

    public Long getOid() {
        return oid;
    }

    public void setOid(Long oid) {
        this.oid = oid;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getSz() {
        return sz;
    }

    public void setSz(String sz) {
        this.sz = sz;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}