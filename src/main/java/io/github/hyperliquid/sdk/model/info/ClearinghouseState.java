package io.github.hyperliquid.sdk.model.info;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Perpetual clearinghouse state encapsulation (account and position overview)
 */
@Setter
@Getter
public class ClearinghouseState {
    /**
     * List of position information for each asset
     */
    private List<AssetPositions> assetPositions;
    /**
     * Cross margin maintenance margin usage
     */
    private String crossMaintenanceMarginUsed;
    /**
     * Cross margin summary
     */
    private CrossMarginSummary crossMarginSummary;
    /**
     * Single currency margin summary
     */
    private MarginSummary marginSummary;
    /**
     * Status timestamp (milliseconds)
     */
    private Long time;
    /**
     * Withdrawable balance (string)
     */
    private String withdrawable;

    @Data
    public static class CumFunding {
        /**
         * Historical cumulative funding rate impact
         */
        private String allTime;
        /**
         * Cumulative since last leverage/mode change
         */
        private String sinceChange;
        /**
         * Cumulative since position opening
         */
        private String sinceOpen;

        @Override
        public String toString() {
            return "CumFunding{" +
                    "allTime='" + allTime + '\'' +
                    ", sinceChange='" + sinceChange + '\'' +
                    ", sinceOpen='" + sinceOpen + '\'' +
                    '}';
        }
    }

    @Data
    public static class Leverage {
        /**
         * Original dollar scale (used for calculation)
         */
        private String rawUsd;
        /**
         * Leverage type (cross/isolated)
         */
        private String type;
        /**
         * Leverage multiplier value
         */
        private int value;

        @Override
        public String toString() {
            return "Leverage{" +
                    "rawUsd='" + rawUsd + '\'' +
                    ", type='" + type + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    @Data
    public static class Position {
        /**
         * Currency name
         */
        private String coin;
        /**
         * Cumulative funding rate impact
         */
        private CumFunding cumFunding;
        /**
         * Average opening price
         */
        private String entryPx;
        /**
         * Leverage information
         */
        private Leverage leverage;
        /**
         * Estimated liquidation price
         */
        private String liquidationPx;
        /**
         * Margin usage
         */
        private String marginUsed;
        /**
         * Maximum allowed leverage
         */
        private int maxLeverage;
        /**
         * Position notional value
         */
        private String positionValue;
        /**
         * Account return on equity (ROE)
         */
        private String returnOnEquity;
        /**
         * Position signed quantity (positive long, negative short, string)
         */
        private String szi;
        /**
         * Unrealized profit and loss
         */
        private String unrealizedPnl;

        @Override
        public String toString() {
            return "Position{" +
                    "coin='" + coin + '\'' +
                    ", cumFunding=" + cumFunding +
                    ", entryPx='" + entryPx + '\'' +
                    ", leverage=" + leverage +
                    ", liquidationPx='" + liquidationPx + '\'' +
                    ", marginUsed='" + marginUsed + '\'' +
                    ", maxLeverage=" + maxLeverage +
                    ", positionValue='" + positionValue + '\'' +
                    ", returnOnEquity='" + returnOnEquity + '\'' +
                    ", szi='" + szi + '\'' +
                    ", unrealizedPnl='" + unrealizedPnl + '\'' +
                    '}';
        }
    }

    @Data
    public static class AssetPositions {
        /**
         * Position details
         */
        private Position position;
        /**
         * Type (e.g., perp)
         */
        private String type;

        @Override
        public String toString() {
            return "AssetPositions{" +
                    "position=" + position +
                    ", type='" + type + '\'' +
                    '}';
        }
    }

    @Data
    public static class CrossMarginSummary {
        /**
         * Account total value
         */
        private String accountValue;
        /**
         * Total margin usage
         */
        private String totalMarginUsed;
        /**
         * Total notional position
         */
        private String totalNtlPos;
        /**
         * Total original dollar scale
         */
        private String totalRawUsd;

        @Override
        public String toString() {
            return "CrossMarginSummary{" +
                    "accountValue='" + accountValue + '\'' +
                    ", totalMarginUsed='" + totalMarginUsed + '\'' +
                    ", totalNtlPos='" + totalNtlPos + '\'' +
                    ", totalRawUsd='" + totalRawUsd + '\'' +
                    '}';
        }
    }

    @Data
    public static class MarginSummary {
        /**
         * Account total value
         */
        private String accountValue;
        /**
         * Total margin usage
         */
        private String totalMarginUsed;
        /**
         * Total notional position
         */
        private String totalNtlPos;
        /**
         * Total original dollar scale
         */
        private String totalRawUsd;

        @Override
        public String toString() {
            return "MarginSummary{" +
                    "accountValue='" + accountValue + '\'' +
                    ", totalMarginUsed='" + totalMarginUsed + '\'' +
                    ", totalNtlPos='" + totalNtlPos + '\'' +
                    ", totalRawUsd='" + totalRawUsd + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ClearinghouseState{" +
                "assetPositions=" + assetPositions +
                ", crossMaintenanceMarginUsed='" + crossMaintenanceMarginUsed + '\'' +
                ", crossMarginSummary=" + crossMarginSummary +
                ", marginSummary=" + marginSummary +
                ", time=" + time +
                ", withdrawable='" + withdrawable + '\'' +
                '}';
    }
}