package io.github.hyperliquid.sdk.model.userabstraction;

/**
 * User abstraction mode values for {@link io.github.hyperliquid.sdk.apis.Exchange#userSetAbstraction(String, UserAbstractionMode)}
 * and related API payloads. Each constant maps to the string sent in the {@code abstraction} field.
 */
public enum UserAbstractionMode {


    /** Abstraction disabled. */
    DISABLED("disabled"),
    /** Unified account abstraction. */
    UNIFIED_ACCOUNT("unifiedAccount"),
    /** Portfolio margin abstraction. */
    PORTFOLIO_MARGIN("portfolioMargin");

    private final String value;

    UserAbstractionMode(String value) {
        this.value = value;
    }

    /**
     * @return Wire value used in signed actions and info responses (e.g. {@code "unifiedAccount"})
     */
    public String getValue() {
        return value;
    }
}
