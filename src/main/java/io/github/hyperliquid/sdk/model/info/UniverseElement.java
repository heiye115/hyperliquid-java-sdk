package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Asset element (name, precision, leverage and margin table binding) */
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

    /** No-argument constructor */
    public UniverseElement() {
    }

    // Getter and Setter methods
    public Integer getSzDecimals() {
        return szDecimals;
    }

    public void setSzDecimals(Integer szDecimals) {
        this.szDecimals = szDecimals;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxLeverage() {
        return maxLeverage;
    }

    public void setMaxLeverage(Integer maxLeverage) {
        this.maxLeverage = maxLeverage;
    }

    public Integer getMarginTableId() {
        return marginTableId;
    }

    public void setMarginTableId(Integer marginTableId) {
        this.marginTableId = marginTableId;
    }
}