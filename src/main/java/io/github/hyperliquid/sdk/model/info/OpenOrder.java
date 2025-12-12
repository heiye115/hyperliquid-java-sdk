package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * openOrders returned unexecuted order entity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenOrder {

    /**
     * Currency name or Spot index (e.g., "BTC", "@107")
     */
    private String coin;
    /**
     * Limit price, string format, e.g., "29792.0"
     */
    private String limitPx;
    /**
     * Order ID
     */
    private Long oid;
    /**
     * Direction string (e.g., "A"/"B", or "Buy"/"Sell", etc., may vary across platforms), keep as is
     */
    private String side;
    /**
     * Order quantity, string format
     */
    private String sz;
    /**
     * Creation timestamp (milliseconds)
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