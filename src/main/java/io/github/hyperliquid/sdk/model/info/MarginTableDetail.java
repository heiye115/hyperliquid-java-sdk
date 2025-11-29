package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 保证金表详情（描述与各保证金层级） */
public class MarginTableDetail {
    
    /** 描述信息 */
    @JsonProperty("description")
    private String description;

    /** 保证金层级列表 */
    @JsonProperty("marginTiers")
    private List<MarginTier> marginTiers;

    /** 无参构造函数 */
    public MarginTableDetail() {
    }

    // Getter and Setter methods
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<MarginTier> getMarginTiers() {
        return marginTiers;
    }

    public void setMarginTiers(List<MarginTier> marginTiers) {
        this.marginTiers = marginTiers;
    }
}