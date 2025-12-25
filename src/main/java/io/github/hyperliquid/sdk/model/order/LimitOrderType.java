package io.github.hyperliquid.sdk.model.order;

/**
 * Limit order type (TIF strategy):
 * - Gtc: valid until canceled;
 * - Alo: add liquidity only (Post Only);
 * - Ioc: immediate or cancel.
 */
public class LimitOrderType {

    private final Tif tif;

    /**
     * Construct limit order type.
     */
    public LimitOrderType(Tif tif) {
        if (tif == null) {
            throw new IllegalArgumentException("tif cannot be null");
        }
        this.tif = tif;
    }

    /**
     * Get TIF strategy.
     *
     * @return TIF strategy
     */
    public Tif getTif() {
        return tif;
    }

    public static LimitOrderType gtc() {
        return new LimitOrderType(Tif.GTC);
    }

    public static LimitOrderType alo() {
        return new LimitOrderType(Tif.ALO);
    }

    public static LimitOrderType ioc() {
        return new LimitOrderType(Tif.IOC);
    }
}