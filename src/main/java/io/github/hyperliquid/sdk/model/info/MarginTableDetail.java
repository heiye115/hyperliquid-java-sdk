package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/** Margin table details (description and margin tiers) */
@Data
@ToString
public class MarginTableDetail {
    
    /** Description information */
    @JsonProperty("description")
    private String description;

    /** Margin tier list */
    @JsonProperty("marginTiers")
    private List<MarginTier> marginTiers;
}