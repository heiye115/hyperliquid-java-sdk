package io.github.hyperliquid.sdk.model.order;

/**
 * 触发单类型（止盈/止损/触发价与是否市价）。
 */
public class TriggerOrderType {
    private final double triggerPx;
    private final boolean isMarket;
    private final String tpsl; // "tp" | "sl"

    /**
     * 构造触发单类型。
     *
     * @param triggerPx 触发价格
     * @param isMarket  是否市价
     * @param tpsl      止盈/止损类型（tp/sl）
     */
    public TriggerOrderType(double triggerPx, boolean isMarket, String tpsl) {
        if (!"tp".equals(tpsl) && !"sl".equals(tpsl)) {
            throw new IllegalArgumentException("tpsl must be 'tp' or 'sl'");
        }
        this.triggerPx = triggerPx;
        this.isMarket = isMarket;
        this.tpsl = tpsl;
    }

    public double getTriggerPx() {
        return triggerPx;
    }

    public boolean isMarket() {
        return isMarket;
    }

    public String getTpsl() {
        return tpsl;
    }
}

