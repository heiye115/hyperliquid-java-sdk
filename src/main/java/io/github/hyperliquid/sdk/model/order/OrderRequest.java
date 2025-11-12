package io.github.hyperliquid.sdk.model.order;

import lombok.Data;

/**
 * 下单请求结构。
 */
@Data
public class OrderRequest {

    /**
     * 交易品种类型
     **/
    private final InstrumentType instrumentType;
    private final String coin;
    private final Boolean isBuy;
    private final Double sz;
    private Double limitPx; // 可为 null（市价）或与触发单组合
    private final OrderType orderType; // 可为 null（普通限价/市价）
    private final Boolean reduceOnly;
    private final Cloid cloid; // 可为 null

    /**
     * 市价下单 滑点比例，例如 0.05 代表 5%
     **/
    private Double slippage = 0.05;

    /**
     * 构造下单请求。
     *
     * @param coin       币种名称（如 "ETH"）
     * @param isBuy      是否买入
     * @param sz         数量
     * @param limitPx    限价价格（可为 null）
     * @param orderType  订单类型（可为 null）
     * @param reduceOnly 是否只减仓
     * @param cloid      客户端订单 ID（可为 null）
     */
    public OrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx,
                        OrderType orderType, Boolean reduceOnly, Cloid cloid) {
        this.instrumentType = instrumentType;
        this.coin = coin;
        this.isBuy = isBuy;
        this.sz = sz;
        this.limitPx = limitPx;
        this.orderType = orderType;
        this.reduceOnly = reduceOnly;
        this.cloid = cloid;
    }

    /**
     * 创建限价单请求。
     *
     * @param instrumentType 交易品种类型
     * @param coin           币种名称（如 "ETH"）
     * @param isBuy          是否买入
     * @param sz             数量
     * @param limitPx        限价价格（可为 null）
     * @param tif            TIF 策略（Gtc/Alo/Ioc）
     * @param reduceOnly     是否只减仓
     * @param cloid          客户端订单 ID
     * @return 限价单请求对象
     */
    public static OrderRequest createLimitOrder(InstrumentType instrumentType, Tif tif, String coin, Boolean isBuy,
                                                Double sz, Double limitPx, Boolean reduceOnly, Cloid cloid) {
        return new OrderRequest(instrumentType, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)),
                reduceOnly, cloid);
    }

    /**
     * 创建默认永续合约限价单请求（非仅减仓，无客户端订单 ID）。
     *
     * @param coin    币种名称（如 "ETH"）
     * @param isBuy   是否买入
     * @param sz      数量
     * @param limitPx 限价价格（可为 null）
     * @param tif     TIF 策略（Gtc/Alo/Ioc）
     * @return 永续合约限价单请求对象
     */
    public static OrderRequest createDefaultPerpLimitOrder(Tif tif, String coin, Boolean isBuy, Double sz,
                                                           Double limitPx) {
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)),
                false, null);
    }

    /**
     * 创建永续合约限价单请求。
     *
     * @param coin       币种名称（如 "ETH"）
     * @param isBuy      是否买入
     * @param sz         数量
     * @param limitPx    限价价格（可为 null）
     * @param tif        TIF 策略（Gtc/Alo/Ioc）
     * @param reduceOnly 是否只减仓
     * @param cloid      客户端订单 ID（可为 null）
     * @return 永续合约限价单请求对象
     */
    public static OrderRequest createPerpLimitOrder(Tif tif, String coin, Boolean isBuy, Double sz, Double limitPx,
                                                    Boolean reduceOnly, Cloid cloid) {
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)),
                reduceOnly, cloid);
    }

    /**
     * 创建永续合约限价单请求（非仅减仓）。
     *
     * @param coin    币种名称（如 "ETH"）
     * @param isBuy   是否买入
     * @param sz      数量
     * @param limitPx 限价价格（可为 null）
     * @param tif     TIF 策略（Gtc/Alo/Ioc）
     * @param cloid   客户端订单 ID
     * @return 永续合约限价单请求对象
     */
    public static OrderRequest createPerpLimitOrder(Tif tif, String coin, Boolean isBuy, Double sz, Double limitPx,
                                                    Long cloid) {
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)),
                false, Cloid.fromLong(cloid));
    }

    /**
     * 创建默认永续合约市价单请求（IOC，非仅减仓，无客户端订单 ID）。
     *
     * @param coin  币种名称（如 "ETH"）
     * @param isBuy 是否买入
     * @param sz    数量
     * @return 永续合约市价单请求对象
     */
    public static OrderRequest createDefaultPerpMarketOrder(String coin, Boolean isBuy, Double sz) {
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, null, new OrderType(new LimitOrderType(Tif.IOC)),
                false, null);
    }

    /**
     * 创建永续合约市价单请求。
     *
     * @param coin       币种名称（如 "ETH"）
     * @param isBuy      是否买入
     * @param sz         数量
     * @param reduceOnly 是否只减仓
     * @param cloid      客户端订单 ID（可为 null）
     * @return 永续合约市价单请求对象
     */
    public static OrderRequest createPerpMarketOrder(String coin, Boolean isBuy, Double sz, Boolean reduceOnly, Cloid cloid) {
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, null, new OrderType(new LimitOrderType(Tif.IOC)), reduceOnly, cloid);
    }

    /**
     * 创建永续合约市价单请求（非仅减仓）。
     *
     * @param coin     币种名称（如 "ETH"）
     * @param isBuy    是否买入
     * @param sz       数量
     * @param slippage 滑点比例
     * @param cloid    客户端订单 ID
     * @return 永续合约市价单请求对象
     */
    public static OrderRequest createPerpMarketOrder(String coin, Boolean isBuy, Double sz, Double slippage, Long cloid) {
        OrderRequest orderRequest = new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, null, new OrderType(new LimitOrderType(Tif.IOC)),
                false, Cloid.fromLong(cloid));
        orderRequest.setSlippage(slippage);
        return orderRequest;
    }

    /**
     * 市价平仓（IOC + reduceOnly=true）。
     * 说明：此方法的第二参数表示“要平掉的数量”（正数，非签名大小）。
     * 方向将由 Exchange.order 在提交前根据用户当前仓位自动推断，
     * 并严格按该数量进行减仓，不会自动改为“全部平仓”。
     *
     * @param coin 币种名称（如 "ETH"）
     * @param sz   要平掉的数量（正数）
     * @return 市价平仓的下单请求
     */
    public static OrderRequest closePositionAtMarket(String coin, Double sz) {
        if (sz == null || sz <= 0.0) {
            throw new IllegalArgumentException("closePositionAtMarket sz must be > 0, coin=" + coin);
        }
        return new OrderRequest(InstrumentType.PERP, coin, null, sz, null, new OrderType(new LimitOrderType(Tif.IOC)),
                true, null);
    }

    /**
     * 市价平仓（IOC + reduceOnly=true），携带客户端订单 ID。
     *
     * @param coin  币种名称（如 "ETH"）
     * @param szi   当前仓位的签名数量（正数表示多仓，负数表示空仓）
     * @param cloid 客户端订单 ID
     * @return 市价平仓的下单请求
     */
    public static OrderRequest closePositionAtMarket(String coin, Double szi, Cloid cloid) {
        if (szi == null || szi == 0.0) {
            throw new IllegalArgumentException("No position to close for coin=" + coin);
        }
        boolean isBuy = szi < 0.0;
        double sz = Math.abs(szi);
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, null, new OrderType(new LimitOrderType(Tif.IOC)),
                true, cloid);
    }

    /**
     * 市价平仓（IOC + reduceOnly=true），携带 long 类型客户端订单 ID。
     *
     * @param coin  币种名称（如 "ETH"）
     * @param szi   当前仓位的签名数量（正数表示多仓，负数表示空仓）
     * @param cloid 客户端订单 ID（long）
     * @return 市价平仓的下单请求
     */
    public static OrderRequest closePositionAtMarket(String coin, Double szi, Long cloid) {
        if (szi == null || szi == 0.0) {
            throw new IllegalArgumentException("No position to close for coin=" + coin);
        }
        boolean isBuy = szi < 0.0;
        double sz = Math.abs(szi);
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, null, new OrderType(new LimitOrderType(Tif.IOC)),
                true, Cloid.fromLong(cloid));
    }

    /**
     * 市价平仓（IOC + reduceOnly=true）。
     * <p>
     * 说明：不提供方向与数量，提交前由 {@code Exchange.prepareRequest} 根据当前仓位推断并补全。
     *
     * @param coin 币种名称（如 "ETH"）
     * @return 市价平仓的下单请求
     */
    public static OrderRequest closePositionAtMarket(String coin) {
        return new OrderRequest(InstrumentType.PERP, coin, null, null, null, new OrderType(new LimitOrderType(Tif.IOC)),
                true, null);
    }

    /**
     * 市价平仓（IOC + reduceOnly=true），携带 long 类型客户端订单 ID。
     * <p>
     * 说明：不提供方向与数量，提交前由 {@code Exchange.prepareRequest} 根据当前仓位推断并补全。
     *
     * @param coin  币种名称（如 "ETH"）
     * @param cloid 客户端订单 ID（long）
     * @return 市价平仓的下单请求
     */
    public static OrderRequest closePositionAtMarket(String coin, Long cloid) {
        return new OrderRequest(InstrumentType.PERP, coin, null, null, null, new OrderType(new LimitOrderType(Tif.IOC)),
                true, Cloid.fromLong(cloid));
    }

    /**
     * 市价平仓（IOC + reduceOnly=true），携带 {@link Cloid} 客户端订单 ID。
     * <p>
     * 说明：不提供方向与数量，提交前由 {@code Exchange.prepareRequest} 根据当前仓位推断并补全。
     *
     * @param coin  币种名称（如 "ETH"）
     * @param cloid 客户端订单 ID
     * @return 市价平仓的下单请求
     */
    public static OrderRequest closePositionAtMarket(String coin, Cloid cloid) {
        return new OrderRequest(InstrumentType.PERP, coin, null, null, null, new OrderType(new LimitOrderType(Tif.IOC)),
                true, cloid);
    }

    /**
     * 创建永续合约触发单请求。
     *
     * @param coin       币种名称（如 "ETH"）
     * @param isBuy      是否买入
     * @param sz         数量
     * @param limitPx    限价价格（可为 null）
     * @param triggerPx  触发价格
     * @param isMarket   是否市价单
     * @param tpsl       止盈止损类型
     * @param reduceOnly 是否只减仓
     * @param cloid      客户端订单 ID（可为 null）
     * @return 永续合约触发单请求对象
     */
    public static OrderRequest createPerpTriggerOrder(String coin, Boolean isBuy, Double sz, Double limitPx,
                                                      Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Boolean reduceOnly, Cloid cloid) {
        return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, limitPx,
                new OrderType(new TriggerOrderType(triggerPx, isMarket, tpsl)), reduceOnly, cloid);
    }

}
