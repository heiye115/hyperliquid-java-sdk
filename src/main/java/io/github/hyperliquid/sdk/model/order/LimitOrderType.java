package io.github.hyperliquid.sdk.model.order;

/**
 * 限价单类型，支持 TIF 策略（Gtc/Alo/Ioc）。
 */
public class LimitOrderType {

    private final Tif tif;

    /**
     * 构造限价单类型。
     */
    public LimitOrderType(Tif tif) {
        if (tif == null) {
            throw new IllegalArgumentException("tif cannot be null");
        }
        this.tif = tif;
    }

    public Tif getTif() {
        return tif;
    }
}