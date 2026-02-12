package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/** Margin tier (position lower bound and maximum leverage) */
@Data
@ToString
public class MarginTier {
    
    /** Position size lower bound (string) */
    @JsonProperty("lowerBound")
    private String lowerBound;

    /** Corresponding maximum leverage multiple */
    @JsonProperty("maxLeverage")
    private Integer maxLeverage;
}