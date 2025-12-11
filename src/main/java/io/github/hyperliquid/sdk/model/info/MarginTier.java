package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Margin tier (position lower bound and maximum leverage) */
public class MarginTier {
    
    /** Position size lower bound (string) */
    @JsonProperty("lowerBound")
    private String lowerBound;

    /** Corresponding maximum leverage multiple */
    @JsonProperty("maxLeverage")
    private Integer maxLeverage;

    /** No-argument constructor */
    public MarginTier() {
    }

    // Getter and Setter methods
    public String getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(String lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Integer getMaxLeverage() {
        return maxLeverage;
    }

    public void setMaxLeverage(Integer maxLeverage) {
        this.maxLeverage = maxLeverage;
    }
}