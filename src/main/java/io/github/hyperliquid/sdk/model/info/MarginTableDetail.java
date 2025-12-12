package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Margin table details (description and margin tiers) */
public class MarginTableDetail {
    
    /** Description information */
    @JsonProperty("description")
    private String description;

    /** Margin tier list */
    @JsonProperty("marginTiers")
    private List<MarginTier> marginTiers;

    /** No-argument constructor */
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