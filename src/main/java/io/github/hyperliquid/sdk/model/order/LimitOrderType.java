package io.github.hyperliquid.sdk.model.order;

/**
 * 限价单类型，支持 TIF 策略（Gtc/Alo/Ioc）。
 */
public class LimitOrderType {
    
    private final String tif; // "Gtc" | "Alo" | "Ioc"

    /**
     * 构造限价单类型。
     *
     * @param tif TIF（Good-till-cancel/Alo/Ioc）
     */
    public LimitOrderType(String tif) {
        if (!"Gtc".equals(tif) && !"Alo".equals(tif) && !"Ioc".equals(tif)) {
            throw new IllegalArgumentException("tif must be one of Gtc, Alo, Ioc");
        }
        this.tif = tif;
    }

    /**
     * 获取 TIF 值。
     *
     * @return TIF 字符串
     */
    public String getTif() {
        return tif;
    }
}

