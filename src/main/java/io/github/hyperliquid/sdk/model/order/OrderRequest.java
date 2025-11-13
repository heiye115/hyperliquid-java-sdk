package io.github.hyperliquid.sdk.model.order;

import lombok.Data;

/**
 * 下单请求结构（Java 侧语义化表示）。
 * <p>
 * 说明：
 * - 与 Python SDK 的 `OrderRequest` 对齐：coin/is_buy/sz/limit_px/order_type/reduce_only/cloid；
 * - 市价单在协议层以“限价 + IOC”表达，`limitPx` 可为空，价格由业务层根据中间价及滑点计算；
 * - 触发单通过 `orderType.trigger` 承载触发参数；
 * - 最终会被转换为线缆结构并发送（见 `utils.Signing.orderRequestToOrderWire`）。
 */
@Data
public class OrderRequest {

    /**
     * 交易品种类型（PERP 永续 / SPOT 现货）
     **/
    private InstrumentType instrumentType;
    /**
     * 币种名称（例如 "ETH"、"BTC"）
     **/
    private String coin;
    /**
     * 是否买入（true=买/做多，false=卖/做空）；市价平仓场景可为空，交由业务层自动推断
     **/
    private Boolean isBuy;
    /**
     * 下单数量（浮点）；最终会规范化为字符串（8 位小数内）
     **/
    private Double sz;
    /**
     * 限价价格；
     * - 可为空（市价单或触发单的市价执行）；
     * - PERP 价格会在 Exchange 层按“5 位有效数字 + (6 - szDecimals) 小数位”规范化。
     **/
    private Double limitPx;
    /**
     * 订单类型：限价（TIF）或触发（triggerPx/isMarket/tpsl）；可为空表示普通限价/市价默认行为
     **/
    private OrderType orderType;
    /**
     * 仅减仓标记（true 表示不会增加仓位）；用于平仓或触发减仓
     **/
    private Boolean reduceOnly;
    /**
     * 客户端订单 ID（Cloid），可为空
     **/
    private Cloid cloid;

    /**
     * 市价下单滑点比例（例如 0.05 表示 5%）；
     * 仅用于业务层模拟“市价=带滑点的 IOC 限价”时计算占位价格
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

    public OrderRequest(InstrumentType instrumentType, String coin, Boolean isBuy, Double sz, Double limitPx, OrderType orderType, Boolean reduceOnly, Cloid cloid, Double slippage) {
        this.instrumentType = instrumentType;
        this.coin = coin;
        this.isBuy = isBuy;
        this.sz = sz;
        this.limitPx = limitPx;
        this.orderType = orderType;
        this.reduceOnly = reduceOnly;
        this.cloid = cloid;
        this.slippage = slippage;
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

    /**
     * 永续开仓
     **/
    public static class Open {

        public static OrderRequest limit(Tif tif, String coin, Boolean isBuy, Double sz, Double limitPx, Cloid cloid) {
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, limitPx, new OrderType(new LimitOrderType(tif)), false, cloid);
        }

        public static OrderRequest limit(Tif tif, String coin, Boolean isBuy, java.math.BigDecimal sz, java.math.BigDecimal limitPx, Cloid cloid) {
            Double s = sz == null ? null : sz.doubleValue();
            Double p = limitPx == null ? null : limitPx.doubleValue();
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, s, p, new OrderType(new LimitOrderType(tif)), false, cloid);
        }

        public static OrderRequest limit(Tif tif, String coin, Boolean isBuy, Double sz, Double limitPx) {
            return limit(tif, coin, isBuy, sz, limitPx, null);
        }

        public static OrderRequest market(String coin, Boolean isBuy, Double sz, Cloid cloid, Double slippage) {
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, null, new OrderType(new LimitOrderType(Tif.IOC)), false, cloid, slippage);
        }

        public static OrderRequest market(String coin, Boolean isBuy, Double sz) {
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, null, new OrderType(new LimitOrderType(Tif.IOC)), false, null);
        }

        public static OrderRequest trigger(String coin, Boolean isBuy, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Cloid cloid) {
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, sz, limitPx, new OrderType(new TriggerOrderType(triggerPx, isMarket, tpsl)), false, cloid);
        }

        public static OrderRequest trigger(String coin, Boolean isBuy, java.math.BigDecimal sz, java.math.BigDecimal limitPx, java.math.BigDecimal triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Cloid cloid) {
            Double s = sz == null ? null : sz.doubleValue();
            Double p = limitPx == null ? null : limitPx.doubleValue();
            Double t = triggerPx == null ? null : triggerPx.doubleValue();
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, s, p, new OrderType(new TriggerOrderType(t, isMarket, tpsl)), false, cloid);
        }

        public static OrderRequest trigger(String coin, Boolean isBuy, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl) {
            return trigger(coin, isBuy, sz, limitPx, triggerPx, isMarket, tpsl, null);
        }

        public static OrderRequest tp(String coin, Boolean isBuy, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, Cloid cloid) {
            return trigger(coin, isBuy, sz, limitPx, triggerPx, isMarket, TriggerOrderType.TpslType.TP, cloid);
        }

        public static OrderRequest sl(String coin, Boolean isBuy, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, Cloid cloid) {
            return trigger(coin, isBuy, sz, limitPx, triggerPx, isMarket, TriggerOrderType.TpslType.SL, cloid);
        }

        public static java.util.List<OrderRequest> tpslBracket(String coin, Boolean isBuy, Double sz, Double tpPx, Double slPx, Cloid cloid) {
            OrderRequest tp = trigger(coin, isBuy, sz, null, tpPx, Boolean.TRUE, TriggerOrderType.TpslType.TP, cloid);
            OrderRequest sl = trigger(coin, isBuy, sz, null, slPx, Boolean.TRUE, TriggerOrderType.TpslType.SL, cloid);
            return java.util.List.of(tp, sl);
        }

    }

    /**
     * 永续平仓
     **/
    public static class Close {

        public static OrderRequest limit(Tif tif, String coin, Double sz, Double limitPx, Cloid cloid) {
            if (sz == null || sz == 0.0) {
                throw new IllegalArgumentException("No position to close for coin=" + coin);
            }
            boolean isBuy = sz < 0.0;
            double absSz = Math.abs(sz);
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, absSz, limitPx, new OrderType(new LimitOrderType(tif)), true, cloid);
        }

        public static OrderRequest limit(Tif tif, String coin, Double sz, Double limitPx) {
            return limit(tif, coin, sz, limitPx, null);
        }

        public static OrderRequest limit(Tif tif, String coin, java.math.BigDecimal signedSz, java.math.BigDecimal limitPx, Cloid cloid) {
            if (signedSz == null || signedSz.doubleValue() == 0.0) {
                throw new IllegalArgumentException("No position to close for coin=" + coin);
            }
            double s = signedSz.doubleValue();
            boolean isBuy = s < 0.0;
            double absSz = Math.abs(s);
            Double p = limitPx == null ? null : limitPx.doubleValue();
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, absSz, p, new OrderType(new LimitOrderType(tif)), true, cloid);
        }

        public static OrderRequest market(String coin, Double sz, Cloid cloid) {
            if (sz == null || sz == 0.0) {
                throw new IllegalArgumentException("No position to close for coin=" + coin);
            }
            boolean isBuy = sz < 0.0;
            double absSz = Math.abs(sz);
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, absSz, null, new OrderType(new LimitOrderType(Tif.IOC)), true, cloid);
        }

        public static OrderRequest market(String coin, Double sz) {
            return market(coin, sz, null);
        }

        public static OrderRequest trigger(String coin, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Cloid cloid) {
            if (sz == null || sz == 0.0) {
                throw new IllegalArgumentException("No position to close for coin=" + coin);
            }
            boolean isBuy = sz < 0.0;
            double absSz = Math.abs(sz);
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, absSz, limitPx, new OrderType(new TriggerOrderType(triggerPx, isMarket, tpsl)), true, cloid);
        }

        public static OrderRequest trigger(String coin, Double sz, Double limitPx, Double triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl) {
            return trigger(coin, sz, limitPx, triggerPx, isMarket, tpsl, null);
        }

        public static OrderRequest trigger(String coin, java.math.BigDecimal signedSz, java.math.BigDecimal limitPx, java.math.BigDecimal triggerPx, Boolean isMarket, TriggerOrderType.TpslType tpsl, Cloid cloid) {
            if (signedSz == null || signedSz.doubleValue() == 0.0) {
                throw new IllegalArgumentException("No position to close for coin=" + coin);
            }
            double s = signedSz.doubleValue();
            boolean isBuy = s < 0.0;
            double absSz = Math.abs(s);
            Double p = limitPx == null ? null : limitPx.doubleValue();
            Double t = triggerPx == null ? null : triggerPx.doubleValue();
            return new OrderRequest(InstrumentType.PERP, coin, isBuy, absSz, p, new OrderType(new TriggerOrderType(t, isMarket, tpsl)), true, cloid);
        }

        public static OrderRequest tp(String coin, Double signedSz, Double limitPx, Double triggerPx, Boolean isMarket, Cloid cloid) {
            return trigger(coin, signedSz, limitPx, triggerPx, isMarket, TriggerOrderType.TpslType.TP, cloid);
        }

        public static OrderRequest sl(String coin, Double signedSz, Double limitPx, Double triggerPx, Boolean isMarket, Cloid cloid) {
            return trigger(coin, signedSz, limitPx, triggerPx, isMarket, TriggerOrderType.TpslType.SL, cloid);
        }
    }

}
