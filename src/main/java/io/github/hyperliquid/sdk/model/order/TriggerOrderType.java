package io.github.hyperliquid.sdk.model.order;

/**
 * Trigger order type: carries trigger price, whether to execute at market price, and trigger direction (tp/sl).
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
     **/
    public TriggerOrderType(String triggerPx, boolean isMarket, TpslType tpsl) {
        if (tpsl == null) {
            throw new IllegalArgumentException("tpsl cannot be null (must specify trigger direction: TP=break above, SL=break below)");
        }
        this.triggerPx = triggerPx;
        this.isMarket = isMarket;
        this.tpsl = tpsl;
    }

    public static TriggerOrderType tp(String triggerPx, boolean isMarket) {
        return new TriggerOrderType(triggerPx, isMarket, TpslType.TP);
    }

    public static TriggerOrderType sl(String triggerPx, boolean isMarket) {
        return new TriggerOrderType(triggerPx, isMarket, TpslType.SL);
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