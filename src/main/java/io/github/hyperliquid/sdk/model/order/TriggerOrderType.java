package io.github.hyperliquid.sdk.model.order;

/**
 * 触发单类型：承载触发价、是否按市价执行、触发方向（tp/sl）。
 * <p>
 * 与 Python `TriggerOrderType` 对齐（triggerPx/isMarket/tpsl）。
 * <p>
 * <b>重要说明：</b>
 * <ul>
 * <li>`tpsl` 字段决定的是<b>触发方向</b>，而非是否平仓！</li>
 * <li>`tpsl="tp"`：价格<b>向上突破</b> triggerPx 时触发（适合止盈或做多突破）</li>
 * <li>`tpsl="sl"`：价格<b>向下跌破</b> triggerPx 时触发（适合止损或做空突破）</li>
 * <li>`reduceOnly` 字段才决定是开仓还是平仓：</li>
 *   <ul>
 *   <li>`reduceOnly=false` + `tpsl="tp"` = 价格突破时开仓（做多）</li>
 *   <li>`reduceOnly=false` + `tpsl="sl"` = 价格跌破时开仓（做空）</li>
 *   <li>`reduceOnly=true` + `tpsl="tp"` = 价格突破时平仓（止盈）</li>
 *   <li>`reduceOnly=true` + `tpsl="sl"` = 价格跌破时平仓（止损）</li>
 *   </ul>
 * </ul>
 */
public class TriggerOrderType {
    /**
     * 触发价格（浮点，最终会规范化为字符串）
     */
    private final Double triggerPx;
    /**
     * 触发后是否以市价执行（true=市价触发；false=限价触发）
     */
    private final Boolean isMarket;
    /**
     * 触发方向类型（必填，决定向上突破还是向下跌破时触发）
     */
    private final TpslType tpsl;

    /**
     * 触发方向类型枚举
     */
    public enum TpslType {
        TP("tp"), // 向上突破触发（Take Profit / 止盈 / 做多突破）
        SL("sl"); // 向下跌破触发（Stop Loss / 止损 / 做空突破）

        private final String value;

        TpslType(String value) {
            this.value = value;
        }

        /**
         * 获取TPSL值。
         *
         * @return TPSL值字符串
         */
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * 构造触发单类型。
     * <p>
     * <b>示例1：无仓位时做多突破（价格向上突破 2950 时买入）</b>
     * <pre>
     * TriggerOrderType trigger = new TriggerOrderType(2950.0, false, TpslType.TP);
     * OrderRequest req = OrderRequest.builder()
     *     .coin("ETH")
     *     .isBuy(true)
     *     .sz(0.1)
     *     .limitPx(3000.0)
     *     .orderType(OrderType.trigger(trigger))
     *     .reduceOnly(false)  // 开仓单
     *     .build();
     * </pre>
     *
     * <b>示例2：有多仓时止盈（价格向上突破 3600 时平仓）</b>
     * <pre>
     * TriggerOrderType tpTrigger = new TriggerOrderType(3600.0, true, TpslType.TP);
     * OrderRequest tpReq = OrderRequest.builder()
     *     .coin("ETH")
     *     .isBuy(false)
     *     .sz(0.5)
     *     .limitPx(3600.0)
     *     .orderType(OrderType.trigger(tpTrigger))
     *     .reduceOnly(true)  // 平仓单
     *     .build();
     * </pre>
     *
     * @param triggerPx 触发价格
     * @param isMarket  是否市价执行（true=触发后市价成交；false=触发后按limitPx挂限价单）
     * @param tpsl      触发方向类型（必填）
     *                  <ul>
     *                  <li>TP：价格向上突破 triggerPx 时触发</li>
     *                  <li>SL：价格向下跌破 triggerPx 时触发</li>
     *                  </ul>
     */
    public TriggerOrderType(double triggerPx, boolean isMarket, TpslType tpsl) {
        if (tpsl == null) {
            throw new IllegalArgumentException("tpsl cannot be null（必须指定触发方向：TP=向上突破，SL=向下跌破）");
        }
        this.triggerPx = triggerPx;
        this.isMarket = isMarket;
        this.tpsl = tpsl;
    }


    /**
     * 获取触发价格
     */
    public double getTriggerPx() {
        return triggerPx;
    }

    /**
     * 是否以市价触发执行
     */
    public boolean isMarket() {
        return isMarket;
    }

    /**
     * 获取触发方向类型字符串值。
     *
     * @return "tp"（向上突破） 或 "sl"（向下跌破）
     */
    public String getTpsl() {
        return tpsl.getValue();
    }

    /**
     * 获取触发方向枚举类型。
     *
     * @return TpslType 枚举
     */
    public TpslType getTpslEnum() {
        return tpsl;
    }
}