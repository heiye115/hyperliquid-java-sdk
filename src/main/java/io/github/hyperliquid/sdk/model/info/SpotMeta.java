package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Spot market metadata (asset and token information) */
public class SpotMeta {
    /** Spot assets (aggregated tokens) collection */
    private List<Universe> universe;
    /** Spot tokens list */
    private List<Token> tokens;

    // Getter and Setter methods
    public List<Universe> getUniverse() {
        return universe;
    }

    public void setUniverse(List<Universe> universe) {
        this.universe = universe;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static class Universe {
        /** List of token IDs contained in this spot asset */
        private List<Integer> tokens;
        /** Asset abbreviation (e.g., "BTC") */
        private String name;
        /** Asset index (integer) */
        private int index;
        /** Whether it is a canonical main asset */
        private boolean isCanonical;

        // Getter and Setter methods
        public List<Integer> getTokens() {
            return tokens;
        }

        public void setTokens(List<Integer> tokens) {
            this.tokens = tokens;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public boolean isCanonical() {
            return isCanonical;
        }

        public void setCanonical(boolean canonical) {
            isCanonical = canonical;
        }
    }

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

        // Getter and Setter methods
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSzDecimals() {
            return szDecimals;
        }

        public void setSzDecimals(Integer szDecimals) {
            this.szDecimals = szDecimals;
        }

        public Integer getWeiDecimals() {
            return weiDecimals;
        }

        public void setWeiDecimals(Integer weiDecimals) {
            this.weiDecimals = weiDecimals;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getTokenId() {
            return tokenId;
        }

        public void setTokenId(String tokenId) {
            this.tokenId = tokenId;
        }

        public Boolean getCanonical() {
            return isCanonical;
        }

        public void setCanonical(Boolean canonical) {
            isCanonical = canonical;
        }

        public EvmContract getEvmContract() {
            return evmContract;
        }

        public void setEvmContract(EvmContract evmContract) {
            this.evmContract = evmContract;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getDeployerTradingFeeShare() {
            return deployerTradingFeeShare;
        }

        public void setDeployerTradingFeeShare(String deployerTradingFeeShare) {
            this.deployerTradingFeeShare = deployerTradingFeeShare;
        }

        public static class EvmContract {
            /** Contract address */
            private String address;
            @JsonProperty("evm_extra_wei_decimals")
            private int evmExtraWeiDecimals; /** Additional Wei precision (contract feature) */

            // Getter and Setter methods
            public String getAddress() {
                return address;
            }

            public void setAddress(String address) {
                this.address = address;
            }

            public int getEvmExtraWeiDecimals() {
                return evmExtraWeiDecimals;
            }

            public void setEvmExtraWeiDecimals(int evmExtraWeiDecimals) {
                this.evmExtraWeiDecimals = evmExtraWeiDecimals;
            }
        }
    }
}