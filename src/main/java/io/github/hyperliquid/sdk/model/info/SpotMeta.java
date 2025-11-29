package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 现货市场元数据（资产与 Token 信息） */
public class SpotMeta {
    /** 现货资产（聚合 Token）集合 */
    private List<Universe> universe;
    /** 现货 Token 列表 */
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
        /** 该现货资产包含的 Token ID 列表 */
        private List<Integer> tokens;
        /** 资产简称（如 "BTC"） */
        private String name;
        /** 资产索引（整数） */
        private int index;
        /** 是否为规范主资产 */
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
        /** Token 名称（如 "WETH"） */
        private String name;
        /** 交易数量精度 */
        private Integer szDecimals;
        /** Wei 精度（EVM 代币最小单位精度） */
        private Integer weiDecimals;
        /** Token 索引（整数） */
        private Integer index;
        /** Token 唯一 ID（字符串） */
        private String tokenId;
        /** 是否为规范主 Token */
        private Boolean isCanonical;
        /** EVM 合约信息（可能为 null） */
        private EvmContract evmContract;
        /** Token 全名（可能为 null） */
        private String fullName;
        /** 部署者交易手续费分成比例（字符串，可能为 null） */
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
            /** 合约地址 */
            private String address;
            @JsonProperty("evm_extra_wei_decimals")
            private int evmExtraWeiDecimals; /** 额外 Wei 精度（合约特性） */

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