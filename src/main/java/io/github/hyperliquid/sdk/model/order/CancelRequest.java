package io.github.hyperliquid.sdk.model.order;

public class CancelRequest {

    /**
     * The coin to cancel the order for.
     */
    private String coin;

    /**
     * The order id to cancel.
     */
    private Long oid;

    public CancelRequest() {
    }

    public CancelRequest(String coin, Long oid) {
        this.coin = coin;
        this.oid = oid;
    }

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
