package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/** Asset element (name, precision, leverage and margin table binding) */
@Data
@ToString
public class UniverseElement {
    
    /** Quantity precision (decimal places) */
    @JsonProperty("szDecimals")
    private Integer szDecimals;

    /** Asset name (e.g., "BTC") */
    @JsonProperty("name")
    private String name;

    /** Maximum leverage multiple */
    @JsonProperty("maxLeverage")
    private Integer maxLeverage;

    /** Bound margin table ID */
    @JsonProperty("marginTableId")
    private Integer marginTableId;
}