package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/** 现货清算所状态：用户代币余额列表 */

public class SpotClearinghouseState {
    /** 余额列表 */
    private List<Balance> balances;

    public static class Balance {
        /** Token 名称或索引前缀形式（如 "@107"） */
        private String coin;
        /** Token 整数 ID */
        private Integer token;
        /** 冻结/占用数量（字符串） */
        private String hold;
        /** 总余额数量（字符串） */
        private String total;
        /** 名义美元价值（字符串） */
        private String entryNtl;

        // Getter and Setter methods
        public String getCoin() {
            return coin;
        }

        public void setCoin(String coin) {
            this.coin = coin;
        }

        public Integer getToken() {
            return token;
        }

        public void setToken(Integer token) {
            this.token = token;
        }

        public String getHold() {
            return hold;
        }

        public void setHold(String hold) {
            this.hold = hold;
        }

        public String getTotal() {
            return total;
        }

        public void setTotal(String total) {
            this.total = total;
        }

        public String getEntryNtl() {
            return entryNtl;
        }

        public void setEntryNtl(String entryNtl) {
            this.entryNtl = entryNtl;
        }
    }

    // Getter and Setter methods
    public List<Balance> getBalances() {
        return balances;
    }

    public void setBalances(List<Balance> balances) {
        this.balances = balances;
    }
}