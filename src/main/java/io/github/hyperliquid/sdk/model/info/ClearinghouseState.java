package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/**
 * 永续清算所状态封装。
 */
public class ClearinghouseState {

    private List<AssetPositions> assetPositions;
    private String crossMaintenanceMarginUsed;
    private CrossMarginSummary crossMarginSummary;
    private MarginSummary marginSummary;
    private Long time;
    private String withdrawable;

    public void setAssetPositions(List<AssetPositions> assetPositions) {
        this.assetPositions = assetPositions;
    }

    public List<AssetPositions> getAssetPositions() {
        return assetPositions;
    }

    public void setCrossMaintenanceMarginUsed(String crossMaintenanceMarginUsed) {
        this.crossMaintenanceMarginUsed = crossMaintenanceMarginUsed;
    }

    public String getCrossMaintenanceMarginUsed() {
        return crossMaintenanceMarginUsed;
    }

    public void setCrossMarginSummary(CrossMarginSummary crossMarginSummary) {
        this.crossMarginSummary = crossMarginSummary;
    }

    public CrossMarginSummary getCrossMarginSummary() {
        return crossMarginSummary;
    }

    public void setMarginSummary(MarginSummary marginSummary) {
        this.marginSummary = marginSummary;
    }

    public MarginSummary getMarginSummary() {
        return marginSummary;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setWithdrawable(String withdrawable) {
        this.withdrawable = withdrawable;
    }

    public String getWithdrawable() {
        return withdrawable;
    }

    public static class CumFunding {

        private String allTime;
        private String sinceChange;
        private String sinceOpen;

        public void setAllTime(String allTime) {
            this.allTime = allTime;
        }

        public String getAllTime() {
            return allTime;
        }

        public void setSinceChange(String sinceChange) {
            this.sinceChange = sinceChange;
        }

        public String getSinceChange() {
            return sinceChange;
        }

        public void setSinceOpen(String sinceOpen) {
            this.sinceOpen = sinceOpen;
        }

        public String getSinceOpen() {
            return sinceOpen;
        }

    }


    public static class Leverage {

        private String rawUsd;
        private String type;
        private int value;

        public void setRawUsd(String rawUsd) {
            this.rawUsd = rawUsd;
        }

        public String getRawUsd() {
            return rawUsd;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    public static class Position {

        private String coin;
        private CumFunding cumFunding;
        private String entryPx;
        private Leverage leverage;
        private String liquidationPx;
        private String marginUsed;
        private int maxLeverage;
        private String positionValue;
        private String returnOnEquity;
        private String szi;
        private String unrealizedPnl;

        public void setCoin(String coin) {
            this.coin = coin;
        }

        public String getCoin() {
            return coin;
        }

        public void setCumFunding(CumFunding cumFunding) {
            this.cumFunding = cumFunding;
        }

        public CumFunding getCumFunding() {
            return cumFunding;
        }

        public void setEntryPx(String entryPx) {
            this.entryPx = entryPx;
        }

        public String getEntryPx() {
            return entryPx;
        }

        public void setLeverage(Leverage leverage) {
            this.leverage = leverage;
        }

        public Leverage getLeverage() {
            return leverage;
        }

        public void setLiquidationPx(String liquidationPx) {
            this.liquidationPx = liquidationPx;
        }

        public String getLiquidationPx() {
            return liquidationPx;
        }

        public void setMarginUsed(String marginUsed) {
            this.marginUsed = marginUsed;
        }

        public String getMarginUsed() {
            return marginUsed;
        }

        public void setMaxLeverage(int maxLeverage) {
            this.maxLeverage = maxLeverage;
        }

        public int getMaxLeverage() {
            return maxLeverage;
        }

        public void setPositionValue(String positionValue) {
            this.positionValue = positionValue;
        }

        public String getPositionValue() {
            return positionValue;
        }

        public void setReturnOnEquity(String returnOnEquity) {
            this.returnOnEquity = returnOnEquity;
        }

        public String getReturnOnEquity() {
            return returnOnEquity;
        }

        public void setSzi(String szi) {
            this.szi = szi;
        }

        public String getSzi() {
            return szi;
        }

        public void setUnrealizedPnl(String unrealizedPnl) {
            this.unrealizedPnl = unrealizedPnl;
        }

        public String getUnrealizedPnl() {
            return unrealizedPnl;
        }

    }

    public static class AssetPositions {

        private Position position;
        private String type;

        public void setPosition(Position position) {
            this.position = position;
        }

        public Position getPosition() {
            return position;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

    }

    public static class CrossMarginSummary {

        private String accountValue;
        private String totalMarginUsed;
        private String totalNtlPos;
        private String totalRawUsd;

        public void setAccountValue(String accountValue) {
            this.accountValue = accountValue;
        }

        public String getAccountValue() {
            return accountValue;
        }

        public void setTotalMarginUsed(String totalMarginUsed) {
            this.totalMarginUsed = totalMarginUsed;
        }

        public String getTotalMarginUsed() {
            return totalMarginUsed;
        }

        public void setTotalNtlPos(String totalNtlPos) {
            this.totalNtlPos = totalNtlPos;
        }

        public String getTotalNtlPos() {
            return totalNtlPos;
        }

        public void setTotalRawUsd(String totalRawUsd) {
            this.totalRawUsd = totalRawUsd;
        }

        public String getTotalRawUsd() {
            return totalRawUsd;
        }

    }

    public static class MarginSummary {

        private String accountValue;
        private String totalMarginUsed;
        private String totalNtlPos;
        private String totalRawUsd;

        public void setAccountValue(String accountValue) {
            this.accountValue = accountValue;
        }

        public String getAccountValue() {
            return accountValue;
        }

        public void setTotalMarginUsed(String totalMarginUsed) {
            this.totalMarginUsed = totalMarginUsed;
        }

        public String getTotalMarginUsed() {
            return totalMarginUsed;
        }

        public void setTotalNtlPos(String totalNtlPos) {
            this.totalNtlPos = totalNtlPos;
        }

        public String getTotalNtlPos() {
            return totalNtlPos;
        }

        public void setTotalRawUsd(String totalRawUsd) {
            this.totalRawUsd = totalRawUsd;
        }

        public String getTotalRawUsd() {
            return totalRawUsd;
        }

    }

}
