package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/**
 * Perpetual clearinghouse state encapsulation (account and position overview)
 */
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

    // Getter and Setter methods
    public List<AssetPositions> getAssetPositions() {
        return assetPositions;
    }

    public void setAssetPositions(List<AssetPositions> assetPositions) {
        this.assetPositions = assetPositions;
    }

    public String getCrossMaintenanceMarginUsed() {
        return crossMaintenanceMarginUsed;
    }

    public void setCrossMaintenanceMarginUsed(String crossMaintenanceMarginUsed) {
        this.crossMaintenanceMarginUsed = crossMaintenanceMarginUsed;
    }

    public CrossMarginSummary getCrossMarginSummary() {
        return crossMarginSummary;
    }

    public void setCrossMarginSummary(CrossMarginSummary crossMarginSummary) {
        this.crossMarginSummary = crossMarginSummary;
    }

    public MarginSummary getMarginSummary() {
        return marginSummary;
    }

    public void setMarginSummary(MarginSummary marginSummary) {
        this.marginSummary = marginSummary;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getWithdrawable() {
        return withdrawable;
    }

    public void setWithdrawable(String withdrawable) {
        this.withdrawable = withdrawable;
    }

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

        // Getter and Setter methods
        public String getAllTime() {
            return allTime;
        }

        public void setAllTime(String allTime) {
            this.allTime = allTime;
        }

        public String getSinceChange() {
            return sinceChange;
        }

        public void setSinceChange(String sinceChange) {
            this.sinceChange = sinceChange;
        }

        public String getSinceOpen() {
            return sinceOpen;
        }

        public void setSinceOpen(String sinceOpen) {
            this.sinceOpen = sinceOpen;
        }
    }


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

        // Getter and Setter methods
        public String getRawUsd() {
            return rawUsd;
        }

        public void setRawUsd(String rawUsd) {
            this.rawUsd = rawUsd;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

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

        // Getter and Setter methods
        public String getCoin() {
            return coin;
        }

        public void setCoin(String coin) {
            this.coin = coin;
        }

        public CumFunding getCumFunding() {
            return cumFunding;
        }

        public void setCumFunding(CumFunding cumFunding) {
            this.cumFunding = cumFunding;
        }

        public String getEntryPx() {
            return entryPx;
        }

        public void setEntryPx(String entryPx) {
            this.entryPx = entryPx;
        }

        public Leverage getLeverage() {
            return leverage;
        }

        public void setLeverage(Leverage leverage) {
            this.leverage = leverage;
        }

        public String getLiquidationPx() {
            return liquidationPx;
        }

        public void setLiquidationPx(String liquidationPx) {
            this.liquidationPx = liquidationPx;
        }

        public String getMarginUsed() {
            return marginUsed;
        }

        public void setMarginUsed(String marginUsed) {
            this.marginUsed = marginUsed;
        }

        public int getMaxLeverage() {
            return maxLeverage;
        }

        public void setMaxLeverage(int maxLeverage) {
            this.maxLeverage = maxLeverage;
        }

        public String getPositionValue() {
            return positionValue;
        }

        public void setPositionValue(String positionValue) {
            this.positionValue = positionValue;
        }

        public String getReturnOnEquity() {
            return returnOnEquity;
        }

        public void setReturnOnEquity(String returnOnEquity) {
            this.returnOnEquity = returnOnEquity;
        }

        public String getSzi() {
            return szi;
        }

        public void setSzi(String szi) {
            this.szi = szi;
        }

        public String getUnrealizedPnl() {
            return unrealizedPnl;
        }

        public void setUnrealizedPnl(String unrealizedPnl) {
            this.unrealizedPnl = unrealizedPnl;
        }
    }

    public static class AssetPositions {
        /**
         * Position details
         */
        private Position position;
        /**
         * Type (e.g., perp)
         */
        private String type;

        // Getter and Setter methods
        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

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

        // Getter and Setter methods
        public String getAccountValue() {
            return accountValue;
        }

        public void setAccountValue(String accountValue) {
            this.accountValue = accountValue;
        }

        public String getTotalMarginUsed() {
            return totalMarginUsed;
        }

        public void setTotalMarginUsed(String totalMarginUsed) {
            this.totalMarginUsed = totalMarginUsed;
        }

        public String getTotalNtlPos() {
            return totalNtlPos;
        }

        public void setTotalNtlPos(String totalNtlPos) {
            this.totalNtlPos = totalNtlPos;
        }

        public String getTotalRawUsd() {
            return totalRawUsd;
        }

        public void setTotalRawUsd(String totalRawUsd) {
            this.totalRawUsd = totalRawUsd;
        }
    }

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

        // Getter and Setter methods
        public String getAccountValue() {
            return accountValue;
        }

        public void setAccountValue(String accountValue) {
            this.accountValue = accountValue;
        }

        public String getTotalMarginUsed() {
            return totalMarginUsed;
        }

        public void setTotalMarginUsed(String totalMarginUsed) {
            this.totalMarginUsed = totalMarginUsed;
        }

        public String getTotalNtlPos() {
            return totalNtlPos;
        }

        public void setTotalNtlPos(String totalNtlPos) {
            this.totalNtlPos = totalNtlPos;
        }

        public String getTotalRawUsd() {
            return totalRawUsd;
        }

        public void setTotalRawUsd(String totalRawUsd) {
            this.totalRawUsd = totalRawUsd;
        }
    }
}