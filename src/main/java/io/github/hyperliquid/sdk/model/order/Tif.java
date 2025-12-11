package io.github.hyperliquid.sdk.model.order;

/**
 * TIF type enum
 */
public enum Tif {
    /**
     * GTC (Good Till Cancel): order has no special behavior, will remain in order book until executed or canceled.
     **/
    GTC("Gtc"),

    /**
     * ALO (Add Liquidity Only): if order would execute immediately, it will be canceled instead of matching.
     **/
    ALO("Alo"),

    /**
     * IOC (Immediate Or Cancel): unexecuted portion will be canceled instead of remaining in order book.
     **/
    IOC("Ioc");

    private final String value;

    Tif(String value) {
        this.value = value;
    }

    /**
     * Get TIF value.
     *
     * @return TIF value string
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}