package io.github.hyperliquid.sdk.model.order;

/**
 * 订单修改请求封装。
 * <p>
 * 用于批量修改订单，支持通过 OID 或 Cloid 定位订单。
 * </p>
 */
public class ModifyRequest {

    /**
     * 币种名称（例如 "ETH"、"BTC"）
     */
    private String coinName;

    /**
     * 订单 ID（OID）
     */
    private Long oid;

    /**
     * 客户端订单 ID（Cloid）
     */
    private Cloid cloid;

    /**
     * 新的订单内容
     */
    private OrderRequest newOrder;

    /**
     * 构造函数
     *
     * @param coinName 币种名称
     * @param oid      订单 OID
     * @param cloid    客户端订单 ID
     * @param newOrder 新订单请求
     */
    public ModifyRequest(String coinName, Long oid, Cloid cloid, OrderRequest newOrder) {
        this.coinName = coinName;
        this.oid = oid;
        this.cloid = cloid;
        this.newOrder = newOrder;
    }

    /**
     * 通过 OID 创建修改请求
     *
     * @param coinName 币种名称
     * @param oid      订单 OID
     * @param newOrder 新订单请求
     * @return ModifyRequest 实例
     */
    public static ModifyRequest byOid(String coinName, Long oid, OrderRequest newOrder) {
        return new ModifyRequest(coinName, oid, null, newOrder);
    }

    /**
     * 通过 Cloid 创建修改请求
     *
     * @param coinName 币种名称
     * @param cloid    客户端订单 ID
     * @param newOrder 新订单请求
     * @return ModifyRequest 实例
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
