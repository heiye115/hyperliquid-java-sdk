package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Market metadata (perpetual) */
public class Meta {

    /** List of supported trading assets */
    @JsonProperty("universe")
    private List<Universe> universe;

    /** Integer ID of collateral token */
    @JsonProperty("collateralToken")
    private Integer collateralToken;

    /** Margin table collection (raw server structure) */
    @JsonProperty("marginTables")
    private List<List<Object>> marginTables;

    // Getter and Setter methods
    public List<Universe> getUniverse() {
        return universe;
    }

    public void setUniverse(List<Universe> universe) {
        this.universe = universe;
    }

    public Integer getCollateralToken() {
        return collateralToken;
    }

    public void setCollateralToken(Integer collateralToken) {
        this.collateralToken = collateralToken;
    }

    public List<List<Object>> getMarginTables() {
        return marginTables;
    }

    public void setMarginTables(List<List<Object>> marginTables) {
        this.marginTables = marginTables;
    }

    public static class Universe {
        /** Quantity precision (decimal places) */
        @JsonProperty("szDecimals")
        private Integer szDecimals;

        /** Asset name (e.g., "BTC") */
        @JsonProperty("name")
        private String name;

        /** Maximum leverage for this asset */
        @JsonProperty("maxLeverage")
        private Integer maxLeverage;

        /** Corresponding margin table ID */
        @JsonProperty("marginTableId")
        private Integer marginTableId;

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
}