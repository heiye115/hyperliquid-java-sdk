package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 市场元数据（类型化） */
public class MetaInfo {
    /** 支持交易的资产集合 */
    @JsonProperty("universe")
    private List<UniverseElement> universe;

    /** 保证金表集合（类型化） */
    @JsonProperty("marginTables")
    private List<MarginTableEntry> marginTables;

    /** 抵押品 Token 的整数 ID */
    @JsonProperty("collateralToken")
    private Integer collateralToken;

    /** 无参构造函数 */
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