package io.github.hyperliquid.sdk.model.order;

public class CancelByCloidRequest {

    /**
     * The coin to cancel the order for.
     */
    private String coin;

    /**
     * The client order ID (CLOID) of the order to cancel.
     */
    private Cloid cloid;

    public CancelByCloidRequest() {

    }

    public CancelByCloidRequest(String coin, Cloid cloid) {
        this.coin = coin;
        this.cloid = cloid;
    }

    public static CancelByCloidRequest of(String coin, Cloid cloid) {
        return new CancelByCloidRequest(coin, cloid);
    }

    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public Cloid getCloid() {
        return cloid;
    }

    public void setCloid(Cloid cloid) {
        this.cloid = cloid;
    }
}
