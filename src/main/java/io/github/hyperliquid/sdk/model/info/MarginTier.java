package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 保证金层级（仓位下限与最大杠杆） */
public class MarginTier {
    
    /** 仓位规模下限（字符串） */
    @JsonProperty("lowerBound")
    private String lowerBound;

    /** 对应最大杠杆倍数 */
    @JsonProperty("maxLeverage")
    private Integer maxLeverage;

    /** 无参构造函数 */
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