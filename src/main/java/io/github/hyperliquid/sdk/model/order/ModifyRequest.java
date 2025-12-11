package io.github.hyperliquid.sdk.model.order;

/**
 * Order modification request wrapper.
 * <p>
 * Used for batch order modification, supports locating orders via OID or Cloid.
 * </p>
 */
public class ModifyRequest {

    /**
     * Currency name (e.g., "ETH", "BTC")
     */
    private String coinName;

    /**
     * Order ID (OID)
     */
    private Long oid;

    /**
     * Client order ID (Cloid)
     */
    private Cloid cloid;

    /**
     * New order content
     */
    private OrderRequest newOrder;

    /**
     * Constructor
     *
     * @param coinName currency name
     * @param oid      order OID
     * @param cloid    client order ID
     * @param newOrder new order request
     */
    public ModifyRequest(String coinName, Long oid, Cloid cloid, OrderRequest newOrder) {
        this.coinName = coinName;
        this.oid = oid;
        this.cloid = cloid;
        this.newOrder = newOrder;
    }

    /**
     * Create modification request via OID
     *
     * @param coinName currency name
     * @param oid      order OID
     * @param newOrder new order request
     * @return ModifyRequest instance
     */
    public static ModifyRequest byOid(String coinName, Long oid, OrderRequest newOrder) {
        return new ModifyRequest(coinName, oid, null, newOrder);
    }

    /**
     * Create modification request via Cloid
     *
     * @param coinName currency name
     * @param cloid    client order ID
     * @param newOrder new order request
     * @return ModifyRequest instance
     */
    public static ModifyRequest byCloid(String coinName, Cloid cloid, OrderRequest newOrder) {
        return new ModifyRequest(coinName, null, cloid, newOrder);
    }

    public String getCoinName() {
        return coinName;
    }

    public void setCoinName(String coinName) {
        this.coinName = coinName;
    }

    public Long getOid() {
        return oid;
    }

    public void setOid(Long oid) {
        this.oid = oid;
    }

    public Cloid getCloid() {
        return cloid;
    }

    public void setCloid(Cloid cloid) {
        this.cloid = cloid;
    }

    public OrderRequest getNewOrder() {
        return newOrder;
    }

    public void setNewOrder(OrderRequest newOrder) {
        this.newOrder = newOrder;
    }
}
