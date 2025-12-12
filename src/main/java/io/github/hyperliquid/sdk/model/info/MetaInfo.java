package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Market metadata (typed) */
public class MetaInfo {
    /** List of supported trading assets */
    @JsonProperty("universe")
    private List<UniverseElement> universe;

    /** Margin table collection (typed) */
    @JsonProperty("marginTables")
    private List<MarginTableEntry> marginTables;

    /** Integer ID of collateral token */
    @JsonProperty("collateralToken")
    private Integer collateralToken;

    /** No-argument constructor */
    public MetaInfo() {
    }

    // Getter and Setter methods
    public List<UniverseElement> getUniverse() {
        return universe;
    }

    public void setUniverse(List<UniverseElement> universe) {
        this.universe = universe;
    }

    public List<MarginTableEntry> getMarginTables() {
        return marginTables;
    }

    public void setMarginTables(List<MarginTableEntry> marginTables) {
        this.marginTables = marginTables;
    }

    public Integer getCollateralToken() {
        return collateralToken;
    }

    public void setCollateralToken(Integer collateralToken) {
        this.collateralToken = collateralToken;
    }
}