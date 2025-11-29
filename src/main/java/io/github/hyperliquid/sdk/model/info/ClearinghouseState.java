package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/** 永续清算所状态封装（账户与仓位概览） */
public class ClearinghouseState {
    /** 各资产的仓位信息列表 */
    private List<AssetPositions> assetPositions;
    /** 交叉保证金维持保证金占用 */
    private String crossMaintenanceMarginUsed;
    /** 交叉保证金汇总 */
    private CrossMarginSummary crossMarginSummary;
    /** 单币种保证金汇总 */
    private MarginSummary marginSummary;
    /** 状态时间戳（毫秒） */
    private Long time;
    /** 可提余额（字符串） */
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
        /** 历史累计资金费率影响 */
        private String allTime;
        /** 自上次杠杆/模式变更以来累计 */
        private String sinceChange;
        /** 自开仓以来累计 */
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
        /** 原始美元规模（用于计算） */
        private String rawUsd;
        /** 杠杆类型（cross/isolated） */
        private String type;
        /** 杠杆倍数值 */
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
        /** 币种名称 */
        private String coin;
        /** 累计资金费率影响 */
        private CumFunding cumFunding;
        /** 开仓均价 */
        private String entryPx;
        /** 杠杆信息 */
        private Leverage leverage;
        /** 预估强平价 */
        private String liquidationPx;
        /** 保证金占用 */
        private String marginUsed;
        /** 最大允许杠杆 */
        private int maxLeverage;
        /** 仓位名义价值 */
        private String positionValue;
        /** 账户收益率（ROE） */
        private String returnOnEquity;
        /** 仓位签名数量（正多负空，字符串） */
        private String szi;
        /** 未实现盈亏 */
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
        /** 仓位详情 */
        private Position position;
        /** 类型（如 perp） */
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
        /** 账户总价值 */
        private String accountValue;
        /** 总保证金占用 */
        private String totalMarginUsed;
        /** 总名义仓位 */
        private String totalNtlPos;
        /** 总原始美元规模 */
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
        /** 账户总价值 */
        private String accountValue;
        /** 总保证金占用 */
        private String totalMarginUsed;
        /** 总名义仓位 */
        private String totalNtlPos;
        /** 总原始美元规模 */
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