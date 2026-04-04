package io.github.hyperliquid.sdk.model.order;

/**
 * Identifies an open order to cancel by coin and client order id ({@link Cloid}).
 * <p>
 * Used with {@link io.github.hyperliquid.sdk.apis.Exchange#cancelByCloids(java.util.List)}.
 * </p>
 */
public class CancelByCloidRequest {

    /**
     * Asset symbol or numeric asset id string as accepted by the exchange.
     */
    private String coin;

    /**
     * Client-generated order id ({@link Cloid}) of the order to cancel.
     */
    private Cloid cloid;

    /** Default constructor for deserialization. */
    public CancelByCloidRequest() {

    }

    /**
     * @param coin  Symbol or asset id string
     * @param cloid Client order id
     */
    public CancelByCloidRequest(String coin, Cloid cloid) {
        this.coin = coin;
        this.cloid = cloid;
    }

    /**
     * @param coin  Symbol or asset id string
     * @param cloid Client order id
     * @return New cancel-by-cloid request
     */
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
