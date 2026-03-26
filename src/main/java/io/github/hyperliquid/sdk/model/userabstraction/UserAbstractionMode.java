package io.github.hyperliquid.sdk.model.userabstraction;

public enum UserAbstractionMode {


    DISABLED("disabled"),
    UNIFIED_ACCOUNT("unifiedAccount"),
    PORTFOLIO_MARGIN("portfolioMargin");

    private final String value;

    UserAbstractionMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
