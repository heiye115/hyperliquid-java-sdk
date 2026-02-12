package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/** Market metadata (perpetual) */
@Data
@ToString
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

    @Data
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
    }
}