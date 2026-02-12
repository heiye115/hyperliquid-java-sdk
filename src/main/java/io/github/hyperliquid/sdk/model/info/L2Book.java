package io.github.hyperliquid.sdk.model.info;

import lombok.Data;

import java.util.List;

/**
 * L2 order book snapshot (top 10 bid/ask levels)
 */
@Data
public class L2Book {

    /**
     * Currency name (e.g., "BTC")
     */
    private String coin;
    /**
     * Snapshot timestamp (milliseconds)
     */
    private Long time;
    /**
     * Bid/ask list: index 0 for bids, index 1 for asks
     */
    private List<List<Levels>> levels;

    @Data
    public static class Levels {
        /**
         * Price at this level (string)
         */
        private String px;
        /**
         * Total order quantity at this level (string)
         */
        private String sz;
        /**
         * Number of orders/level count at this price
         */
        private Integer n;

        @Override
        public String toString() {
            return "Levels{" +
                    "px='" + px + '\'' +
                    ", sz='" + sz + '\'' +
                    ", n=" + n +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "L2Book{" +
                "coin='" + coin + '\'' +
                ", time=" + time +
                ", levels=" + levels +
                '}';
    }
}