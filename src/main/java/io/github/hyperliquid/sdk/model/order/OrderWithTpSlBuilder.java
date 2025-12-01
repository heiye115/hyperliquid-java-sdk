package io.github.hyperliquid.sdk.model.order;

import java.util.ArrayList;
import java.util.List;

/**
 * 开仓 + 止盈止损组合订单构建器。
 * <p>
 * 用于构建 normalTpsl 或 positionTpsl 订单组，简化一键设置止盈止损的操作。
 * <p>
 * 使用示例：
 * <pre>
 * // 1. 限价开仓 + 止盈止损（normalTpsl）
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     .buy(0.1)
 *     .entryPrice(3500.0)  // 设置了entryPrice，将使用限价单
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildNormalTpsl();
 *
 * // 提交时自动推断 grouping="normalTpsl"
 * JsonNode result = exchange.bulkOrders(orderGroup);
 *
 * // 2. 市价开仓 + 止盈止损（normalTpsl）
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     .buy(0.1)
 *     // 没有设置entryPrice，将使用市价单
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildNormalTpsl();
 *
 * // 提交时自动推断 grouping="normalTpsl"
 * JsonNode result = exchange.bulkOrders(orderGroup);
 *
 * // 3. 仅为已有仓位添加止盈止损（positionTpsl - 手动指定）
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     .closePosition(0.5, true)  // 平掉 0.5 ETH 多仓
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildPositionTpsl();
 *
 * // 提交时自动推断 grouping="positionTpsl"
 * JsonNode result = exchange.bulkOrders(orderGroup);
 *
 * // 4. 仅为已有仓位添加止盈止损（positionTpsl - 自动推断）
 * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
 *     .perp("ETH")
 *     // 不调用 closePosition()，Exchange 会自动查询账户持仓并推断方向和数量
 *     .takeProfit(3600.0)
 *     .stopLoss(3400.0)
 *     .buildPositionTpsl();
 *
 * // Exchange 会自动调用 API 获取仓位信息并填充方向和数量
 * JsonNode result = exchange.bulkOrders(orderGroup);
 * </pre>
 */
public class OrderWithTpSlBuilder {
    /**
     * 交易品种类型（PERP 或 SPOT）
     */
    private InstrumentType instrumentType;

    /**
     * 币种名称
     */
    private String coin;

    /**
     * 是否买入（true=买/做多，false=卖/做空）
     */
    private Boolean isBuy;

    /**
     * 下单数量
     */
    private Double sz;

    /**
     * 开仓限价（buildAll 模式必填）
     */
    private Double entryPrice;

    /**
     * 止盈价格
     */
    private Double takeProfitPrice;

    /**
     * 止损价格
     */
    private Double stopLossPrice;

    /**
     * 开仓单的 TIF 策略（默认 GTC）
     */
    private Tif entryTif = Tif.GTC;

    /**
     * 客户端订单 ID
     */
    private Cloid cloid;

    /**
     * 订单过期时间（毫秒）
     */
    private Long expiresAfter;

    public OrderWithTpSlBuilder() {
    }

    // ========================================
    // 5. 过期时间
    // ========================================

    /**
     * 设置订单过期时间。
     *
     * @param expiresAfter 过期时间（毫秒）
     * @return this
     */
    public OrderWithTpSlBuilder expiresAfter(Long expiresAfter) {
        this.expiresAfter = expiresAfter;
        return this;
    }

    // ========================================
    // 1. 交易品种
    // ========================================

    /**
     * 设置为永续合约。
     *
     * @param coin 币种名称
     * @return this
     */
    public OrderWithTpSlBuilder perp(String coin) {
        this.instrumentType = InstrumentType.PERP;
        this.coin = coin;
        return this;
    }

    /**
     * 设置为现货。
     *
     * @param coin 币种名称
     * @return this
     */
    public OrderWithTpSlBuilder spot(String coin) {
        this.instrumentType = InstrumentType.SPOT;
        this.coin = coin;
        return this;
    }

    // ========================================
    // 2. 方向与数量
    // ========================================

    /**
     * 买入开仓。
     *
     * @param sz 数量
     * @return this
     */
    public OrderWithTpSlBuilder buy(Double sz) {
        this.isBuy = true;
        this.sz = sz;
        return this;
    }

    /**
     * 卖出开仓。
     *
     * @param sz 数量
     * @return this
     */
    public OrderWithTpSlBuilder sell(Double sz) {
        this.isBuy = false;
        this.sz = sz;
        return this;
    }

    /**
     * 平仓模式（用于 positionTpsl）。
     * <p>
     * 当你已经持有仓位，想为仓位添加止盈止损时使用。
     *
     * @param sz             仓位数量
     * @param isLongPosition 是否多仓（true=多仓，false=空仓）
     * @return this
     */
    public OrderWithTpSlBuilder closePosition(Double sz, boolean isLongPosition) {
        this.isBuy = isLongPosition; // 多仓需要卖出平仓，空仓需要买入平仓
        this.sz = sz;
        this.entryPrice = null; // 不需要开仓价
        return this;
    }

    // ========================================
    // 3. 价格设置
    // ========================================

    /**
     * 设置开仓限价。
     *
     * @param entryPrice 开仓价格
     * @return this
     */
    public OrderWithTpSlBuilder entryPrice(Double entryPrice) {
        this.entryPrice = entryPrice;
        return this;
    }

    /**
     * 设置止盈价格。
     *
     * @param tpPrice 止盈价
     * @return this
     */
    public OrderWithTpSlBuilder takeProfit(Double tpPrice) {
        this.takeProfitPrice = tpPrice;
        return this;
    }

    /**
     * 设置止损价格。
     *
     * @param slPrice 止损价
     * @return this
     */
    public OrderWithTpSlBuilder stopLoss(Double slPrice) {
        this.stopLossPrice = slPrice;
        return this;
    }

    // ========================================
    // 4. 其他选项
    // ========================================

    /**
     * 设置开仓单的 TIF 策略。
     *
     * @param tif TIF 策略
     * @return this
     */
    public OrderWithTpSlBuilder entryTif(Tif tif) {
        this.entryTif = tif;
        return this;
    }

    /**
     * 设置客户端订单 ID。
     *
     * @param cloid Cloid
     * @return this
     */
    public OrderWithTpSlBuilder cloid(Cloid cloid) {
        this.cloid = cloid;
        return this;
    }

    // ========================================
    // 5. 构建
    // ========================================

    /**
     * 构建 normalTpsl 订单组（开仓 + TP + SL）。
     * <p>
     * 返回的 OrderGroup 会自动携带 grouping="normalTpsl" 类型信息。
     * 在调用 exchange.bulkOrders(orderGroup) 时会自动推断使用 normalTpsl 分组。
     *
     * @return OrderGroup 包含订单列表和分组类型
     * @throws IllegalStateException 当必填字段缺失时抛出
     */
    public OrderGroup buildNormalTpsl() {
        return new OrderGroup(buildOrderList(true), GroupingType.NORMAL_TPSL);
    }

    /**
     * 构建 positionTpsl 订单组（仅 TP + SL，不含开仓单）。
     * <p>
     * 返回的 OrderGroup 会自动携带 grouping="positionTpsl" 类型信息。
     * 用于为已有仓位添加或修改止盈止损。
     * <p>
     * <b>自动推断仓位功能：</b>
     * <ul>
     *   <li>如果调用了 closePosition(sz, isLong) - 手动指定仓位方向和数量</li>
     *   <li>如果未调用 closePosition() - Exchange 会自动查询账户持仓并推断方向和数量</li>
     * </ul>
     *
     * @return OrderGroup 包含订单列表和分组类型
     * @throws IllegalStateException 当必填字段缺失时抛出
     */
    public OrderGroup buildPositionTpsl() {
        return new OrderGroup(buildOrderList(false), GroupingType.POSITION_TPSL);
    }

    /**
     * 构建订单列表。
     *
     * @param includeEntry 是否包含开仓单（true=normalTpsl，false=positionTpsl）
     * @return 订单列表
     */
    private List<OrderRequest> buildOrderList(boolean includeEntry) {
        validate(includeEntry);
        List<OrderRequest> orders = new ArrayList<>();
        
        // 1. 开仓单（仅在 normalTpsl 模式下添加）
        if (includeEntry) {
            OrderRequest entry;
            if (entryPrice == null) {
            // 市价单 - 创建一个占位请求，后续在Exchange中会通过marketOpenTransition方法处理
            entry = new OrderRequest();
            entry.setInstrumentType(instrumentType != null ? instrumentType : InstrumentType.PERP);
            entry.setCoin(coin);
            entry.setIsBuy(isBuy);
            entry.setSz(sz);
            entry.setReduceOnly(false);
            entry.setCloid(cloid);
            // 设置为 IOC 市价单，价格将在Exchange.marketOpenTransition中计算
            LimitOrderType limitOrderType = new LimitOrderType(Tif.IOC);
            OrderType orderType = new OrderType(limitOrderType);
            entry.setOrderType(orderType);
        } else {
            // 限价单
            entry = new OrderRequest(
                    instrumentType != null ? instrumentType : InstrumentType.PERP,
                    coin,
                    isBuy,
                    sz,
                    entryPrice,
                    new OrderType(new LimitOrderType(entryTif)),
                    false,
                    cloid
            );
        }
            if (expiresAfter != null) {
                entry.setExpiresAfter(expiresAfter);
            }
            orders.add(entry);
        }
        // 2. 止盈单（平仓）
        if (takeProfitPrice != null) {
            OrderRequest tp = new OrderRequest(
                    instrumentType != null ? instrumentType : InstrumentType.PERP,
                    coin,
                    isBuy != null ? !isBuy : null,  // 反向平仓（如果 isBuy 为 null，则保持 null）
                    sz,
                    takeProfitPrice,
                    new OrderType(new TriggerOrderType(takeProfitPrice, true, TriggerOrderType.TpslType.TP)),
                    true,
                    null
            );

            if (expiresAfter != null) {
                tp.setExpiresAfter(expiresAfter);
            }
            orders.add(tp);
        }

        // 3. 止损单（平仓）
        if (stopLossPrice != null) {
            OrderRequest sl = new OrderRequest(
                    instrumentType != null ? instrumentType : InstrumentType.PERP,
                    coin,
                    isBuy != null ? !isBuy : null,
                    sz,
                    stopLossPrice,
                    new OrderType(new TriggerOrderType(stopLossPrice, true, TriggerOrderType.TpslType.SL)),
                    true,
                    null
            );

            if (expiresAfter != null) {
                sl.setExpiresAfter(expiresAfter);
            }
            orders.add(sl);
        }

        return orders;
    }

    /**
     * 校验必填字段。
     *
     * @param isNormalTpsl 是否为 normalTpsl 模式（true=normalTpsl，false=positionTpsl）
     */
    private void validate(boolean isNormalTpsl) {
        if (coin == null || coin.isEmpty()) {
            throw new IllegalStateException("coin is required");
        }
        
        // normalTpsl 模式：必须指定方向和数量
        if (isNormalTpsl) {
            if (isBuy == null) {
                throw new IllegalStateException("direction is required for normalTpsl (call buy() or sell())");
            }
            if (sz == null || sz <= 0) {
                throw new IllegalStateException("size must be positive");
            }
        }
        // positionTpsl 模式：允许 isBuy 和 sz 为 null（由 Exchange 自动推断）
        // 但如果设置了 sz，则必须 > 0
        else {
            if (sz != null && sz <= 0) {
                throw new IllegalStateException("size must be positive if specified");
            }
        }
        
        if (takeProfitPrice == null && stopLossPrice == null) {
            throw new IllegalStateException("at least one of takeProfit or stopLoss is required");
        }
    }
}
