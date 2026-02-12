package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/** Market metadata (typed) */
@Data
@ToString
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
}