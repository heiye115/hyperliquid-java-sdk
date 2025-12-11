package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/** L2 order book snapshot (top 10 bid/ask levels) */
public class L2Book {

    /** Currency name (e.g., "BTC") */
    private String coin;
    /** Snapshot timestamp (milliseconds) */
    private Long time;
    /** Bid/ask list: index 0 for bids, index 1 for asks */
    private List<List<Levels>> levels;

    // Getter and Setter methods
    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public List<List<Levels>> getLevels() {
        return levels;
    }

    public void setLevels(List<List<Levels>> levels) {
        this.levels = levels;
    }

    public static class Levels {
        /** Price at this level (string) */
        private String px;
        /** Total order quantity at this level (string) */
        private String sz;
        /** Number of orders/level count at this price */
        private Integer n;

        // Getter and Setter methods
        public String getPx() {
            return px;
        }

        public void setPx(String px) {
            this.px = px;
        }

        public String getSz() {
            return sz;
        }

        public void setSz(String sz) {
            this.sz = sz;
        }

        public Integer getN() {
            return n;
        }

        public void setN(Integer n) {
            this.n = n;
        }
    }
}