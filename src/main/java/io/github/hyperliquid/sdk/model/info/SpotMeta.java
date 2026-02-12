package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** Spot market metadata (asset and token information) */
@Data
public class SpotMeta {
    /** Spot assets (aggregated tokens) collection */
    private List<Universe> universe;
    /** Spot tokens list */
    private List<Token> tokens;

    @Data
    public static class Universe {
        /** List of token IDs contained in this spot asset */
        private List<Integer> tokens;
        /** Asset abbreviation (e.g., "BTC") */
        private String name;
        /** Asset index (integer) */
        private int index;
        /** Whether it is a canonical main asset */
        @JsonProperty("isCanonical")
        private boolean isCanonical;

        @JsonProperty("isCanonical")
        public boolean isCanonical() {
            return isCanonical;
        }

        @JsonProperty("isCanonical")
        public void setCanonical(boolean canonical) {
            isCanonical = canonical;
        }
    }

    @Data
    public static class Token {
        /** Token name (e.g., "WETH") */
        private String name;
        /** Trading quantity precision */
        private Integer szDecimals;
        /** Wei precision (EVM token smallest unit precision) */
        private Integer weiDecimals;
        /** Token index (integer) */
        private Integer index;
        /** Token unique ID (string) */
        private String tokenId;
        /** Whether it is a canonical main token */
        private Boolean isCanonical;
        /** EVM contract information (may be null) */
        private EvmContract evmContract;
        /** Token full name (may be null) */
        private String fullName;
        /** Deployer trading fee share ratio (string, may be null) */
        private String deployerTradingFeeShare;

        @JsonProperty("isCanonical")
        public Boolean getCanonical() {
            return isCanonical;
        }

        @JsonProperty("isCanonical")
        public void setCanonical(Boolean canonical) {
            isCanonical = canonical;
        }

        @Data
        public static class EvmContract {
            /** Contract address */
            private String address;
            /** Additional Wei precision (contract feature) */
            @JsonProperty("evm_extra_wei_decimals")
            private int evmExtraWeiDecimals;
        }
    }
}
