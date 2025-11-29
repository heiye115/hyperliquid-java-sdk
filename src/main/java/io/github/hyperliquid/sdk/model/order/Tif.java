package io.github.hyperliquid.sdk.model.order;

/**
 * TIF 类型枚举
 */
public enum Tif {
    /**
     * GTC（直到取消前有效）：订单没有特殊行为，会一直留在订单簿中直到成交或被取消。
     **/
    GTC("Gtc"),

    /**
     * ALO（仅添加流动性，即"仅做市"）：如果订单会立即成交，则将被取消，而不是立即匹配。
     **/
    ALO("Alo"),

    /**
     * IOC（立即成交或取消）：未成交部分将被取消，而不是留在订单簿中等待。
     **/
    IOC("Ioc");

    private final String value;

    Tif(String value) {
        this.value = value;
    }

    /**
     * 获取TIF值。
     *
     * @return TIF值字符串
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}