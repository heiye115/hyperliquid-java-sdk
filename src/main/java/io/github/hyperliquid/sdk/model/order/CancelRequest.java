package io.github.hyperliquid.sdk.model.order;

/**
 * Identifies an open order to cancel by coin symbol and exchange order id (OID).
 * <p>
 * Used with {@link io.github.hyperliquid.sdk.apis.Exchange#cancels(java.util.List)}.
 * </p>
 */
public class CancelRequest {

    /**
     * Asset symbol or numeric asset id string as accepted by {@link io.github.hyperliquid.sdk.apis.Info#nameToAsset(String)}.
     */
    private String coin;

    /**
     * Server-assigned order id to cancel.
     */
    private Long oid;

    /** Default constructor for deserialization. */
    public CancelRequest() {
    }

    /**
     * @param coin Symbol or asset id string
     * @param oid  Order id (OID)
     */
    public CancelRequest(String coin, Long oid) {
        this.coin = coin;
        this.oid = oid;
    }

    /**
     * @param coin Symbol or asset id string
     * @param oid  Order id (OID)
     * @return New cancel request
     */
    public static CancelRequest of(String coin, Long oid) {
        return new CancelRequest(coin, oid);
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public Long getOid() {
        return oid;
    }

    public void setOid(Long oid) {
        this.oid = oid;
    }


}
