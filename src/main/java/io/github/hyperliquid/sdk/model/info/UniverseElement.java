package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 资产元素（名称、精度、杠杆与保证金表绑定） */
public class UniverseElement {
    
    /** 数量精度（小数位） */
    @JsonProperty("szDecimals")
    private Integer szDecimals;

    /** 资产名称（如 "BTC"） */
    @JsonProperty("name")
    private String name;

    /** 最大杠杆倍数 */
    @JsonProperty("maxLeverage")
    private Integer maxLeverage;

    /** 绑定的保证金表 ID */
    @JsonProperty("marginTableId")
    private Integer marginTableId;

    /** 无参构造函数 */
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