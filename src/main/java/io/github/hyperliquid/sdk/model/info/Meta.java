package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 市场元数据（永续） */
public class Meta {

    /** 支持交易的资产集合 */
    @JsonProperty("universe")
    private List<Universe> universe;

    /** 抵押品 Token 的整数 ID */
    @JsonProperty("collateralToken")
    private Integer collateralToken;

    /** 保证金表集合（服务端原始结构） */
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
        /** 数量精度（小数位） */
        @JsonProperty("szDecimals")
        private Integer szDecimals;

        /** 资产名称（如 "BTC"） */
        @JsonProperty("name")
        private String name;

        /** 该资产的最大杠杆 */
        @JsonProperty("maxLeverage")
        private Integer maxLeverage;

        /** 对应的保证金表 ID */
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