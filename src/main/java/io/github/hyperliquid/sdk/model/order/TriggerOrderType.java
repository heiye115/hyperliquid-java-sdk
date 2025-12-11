package io.github.hyperliquid.sdk.model.order;

/**
 * Trigger order type: carries trigger price, whether to execute at market price, and trigger direction (tp/sl).
 * <p>
 * Aligned with Python `TriggerOrderType` (triggerPx/isMarket/tpsl).
 * <p>
 * <b>Important note:</b>
 * <ul>
 * <li>The `tpsl` field determines the <b>trigger direction</b>, not whether to close position!</li>
 * <li>`tpsl="tp"`: triggers when price <b>breaks above</b> triggerPx (suitable for take-profit or long breakout)</li>
 * <li>`tpsl="sl"`: triggers when price <b>breaks below</b> triggerPx (suitable for stop-loss or short breakout)</li>
 * <li>The `reduceOnly` field determines whether to open or close position:</li>
 *   <ul>
 *   <li>`reduceOnly=false` + `tpsl="tp"` = open position when price breaks out (go long)</li>
 *   <li>`reduceOnly=false` + `tpsl="sl"` = open position when price breaks down (go short)</li>
 *   <li>`reduceOnly=true` + `tpsl="tp"` = close position when price breaks out (take-profit)</li>
 *   <li>`reduceOnly=true` + `tpsl="sl"` = close position when price breaks down (stop-loss)</li>
 *   </ul>
 * </ul>
 */
public class TriggerOrderType {
    /**
     * Trigger price (string)
     */
    private final String triggerPx;
    /**
     * Whether to execute at market price after trigger (true=market trigger; false=limit trigger)
     */
    private final Boolean isMarket;
    /**
     * Trigger direction type (required, determines whether to trigger on upward breakout or downward breakdown)
     */
    private final TpslType tpsl;

    /**
     * Trigger direction type enum
     */
    public enum TpslType {
        TP("tp"), // Trigger on upward breakout (Take Profit / take-profit / long breakout)
        SL("sl"); // Trigger on downward breakdown (Stop Loss / stop-loss / short breakout)

        private final String value;

        TpslType(String value) {
            this.value = value;
        }

        /**
         * Get TPSL value.
         *
         * @return TPSL value string
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
     * Construct trigger order type.
     * <p>
     * <b>Example 1: Long breakout when no position (buy when price breaks above 2950)</b>
     * <pre>
     * TriggerOrderType trigger = new TriggerOrderType("2950.0", false, TpslType.TP);
     * OrderRequest req = OrderRequest.builder()
     *     .coin("ETH")
     *     .isBuy(true)
     *     .sz("0.1")
     *     .limitPx("3000.0")
     *     .orderType(OrderType.trigger(trigger))
     *     .reduceOnly(false)  // Open position order
     *     .build();
     * </pre>
     *
     * <b>Example 2: Take-profit when having long position (close position when price breaks above 3600)</b>
     * <pre>
     * TriggerOrderType tpTrigger = new TriggerOrderType("3600.0", true, TpslType.TP);
     * OrderRequest tpReq = OrderRequest.builder()
     *     .coin("ETH")
     *     .isBuy(false)
     *     .sz("0.5")
     *     .limitPx("3600.0")
     *     .orderType(OrderType.trigger(tpTrigger))
     *     .reduceOnly(true)  // Close position order
     *     .build();
     * </pre>
     *
     * @param triggerPx trigger price (string)
     * @param isMarket  whether to execute at market price (true=execute at market price after trigger; false=place limit order at limitPx after trigger)
     * @param tpsl      trigger direction type (required)
     *                  <ul>
     *                  <li>TP: triggers when price breaks above triggerPx</li>
     *                  <li>SL: triggers when price breaks below triggerPx</li>
     *                  </ul>
     */
    public TriggerOrderType(String triggerPx, boolean isMarket, TpslType tpsl) {
        if (tpsl == null) {
            throw new IllegalArgumentException("tpsl cannot be null (must specify trigger direction: TP=break above, SL=break below)");
        }
        this.triggerPx = triggerPx;
        this.isMarket = isMarket;
        this.tpsl = tpsl;
    }


    /**
     * Get trigger price
     */
    public String getTriggerPx() {
        return triggerPx;
    }

    /**
     * Whether to trigger execution at market price
     */
    public boolean isMarket() {
        return isMarket;
    }

    /**
     * Get trigger direction type string value.
     *
     * @return "tp" (break above) or "sl" (break below)
     */
    public String getTpsl() {
        return tpsl.getValue();
    }

    /**
     * Get trigger direction enum type.
     *
     * @return TpslType enum
     */
    public TpslType getTpslEnum() {
        return tpsl;
    }
}