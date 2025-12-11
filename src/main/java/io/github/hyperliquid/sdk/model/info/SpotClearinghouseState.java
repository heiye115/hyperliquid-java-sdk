package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/** Spot clearinghouse state: user token balance list */

public class SpotClearinghouseState {
    /** Balance list */
    private List<Balance> balances;

    public static class Balance {
        /** Token name or index prefix form (e.g., "@107") */
        private String coin;
        /** Token integer ID */
        private Integer token;
        /** Frozen/occupied quantity (string) */
        private String hold;
        /** Total balance quantity (string) */
        private String total;
        /** Nominal USD value (string) */
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