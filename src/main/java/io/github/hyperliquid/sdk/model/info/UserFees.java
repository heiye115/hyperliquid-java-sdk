package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/**
 * Hyperliquid user fees response
 * This class maps the JSON response from:
 * POST <a href="https://api.hyperliquid.xyz/info">...</a>
 * type = userFees
 */
public class UserFees {
    /**
     * Daily trading volume of the user
     */
    private List<DailyUserVlm> dailyUserVlm;

    /**
     * Fee schedule and VIP/MM tier structure
     */
    private FeeSchedule feeSchedule;

    /**
     * User actual taker fee rate (perpetual futures)
     */
    private String userCrossRate;

    /**
     * User actual maker fee rate (perpetual futures)
     */
    private String userAddRate;

    /**
     * User actual taker fee rate (spot)
     */
    private String userSpotCrossRate;

    /**
     * User actual maker fee rate (spot)
     */
    private String userSpotAddRate;

    /**
     * Active referral discount applied to the user
     */
    private String activeReferralDiscount;

    /**
     * Trial status (if user is in fee trial program)
     */
    private Object trial;

    /**
     * Trial fee escrow amount
     */
    private String feeTrialEscrow;

    /**
     * Timestamp when next trial becomes available
     */
    private Object nextTrialAvailableTimestamp;

    /**
     * Staking link info (if user linked to a staking account)
     */
    private Object stakingLink;

    /**
     * Active staking discount currently applied
     */
    private ActiveStakingDiscount activeStakingDiscount;

    public List<DailyUserVlm> getDailyUserVlm() {
        return dailyUserVlm;
    }

    public void setDailyUserVlm(List<DailyUserVlm> dailyUserVlm) {
        this.dailyUserVlm = dailyUserVlm;
    }

    public FeeSchedule getFeeSchedule() {
        return feeSchedule;
    }

    public void setFeeSchedule(FeeSchedule feeSchedule) {
        this.feeSchedule = feeSchedule;
    }

    public String getUserCrossRate() {
        return userCrossRate;
    }

    public void setUserCrossRate(String userCrossRate) {
        this.userCrossRate = userCrossRate;
    }

    public String getUserAddRate() {
        return userAddRate;
    }

    public void setUserAddRate(String userAddRate) {
        this.userAddRate = userAddRate;
    }

    public String getUserSpotCrossRate() {
        return userSpotCrossRate;
    }

    public void setUserSpotCrossRate(String userSpotCrossRate) {
        this.userSpotCrossRate = userSpotCrossRate;
    }

    public String getUserSpotAddRate() {
        return userSpotAddRate;
    }

    public void setUserSpotAddRate(String userSpotAddRate) {
        this.userSpotAddRate = userSpotAddRate;
    }

    public String getActiveReferralDiscount() {
        return activeReferralDiscount;
    }

    public void setActiveReferralDiscount(String activeReferralDiscount) {
        this.activeReferralDiscount = activeReferralDiscount;
    }

    public Object getTrial() {
        return trial;
    }

    public void setTrial(Object trial) {
        this.trial = trial;
    }

    public String getFeeTrialEscrow() {
        return feeTrialEscrow;
    }

    public void setFeeTrialEscrow(String feeTrialEscrow) {
        this.feeTrialEscrow = feeTrialEscrow;
    }

    public Object getNextTrialAvailableTimestamp() {
        return nextTrialAvailableTimestamp;
    }

    public void setNextTrialAvailableTimestamp(Object nextTrialAvailableTimestamp) {
        this.nextTrialAvailableTimestamp = nextTrialAvailableTimestamp;
    }

    public Object getStakingLink() {
        return stakingLink;
    }

    public void setStakingLink(Object stakingLink) {
        this.stakingLink = stakingLink;
    }

    public ActiveStakingDiscount getActiveStakingDiscount() {
        return activeStakingDiscount;
    }

    public void setActiveStakingDiscount(ActiveStakingDiscount activeStakingDiscount) {
        this.activeStakingDiscount = activeStakingDiscount;
    }

    /**
     * Daily user trading volume record
     */
    public static class DailyUserVlm {

        /**
         * Date (YYYY-MM-DD)
         */
        private String date;

        /**
         * User taker volume for the day
         */
        private String userCross;

        /**
         * User maker volume for the day
         */
        private String userAdd;

        /**
         * Total exchange volume for the day
         */
        private String exchange;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getUserCross() {
            return userCross;
        }

        public void setUserCross(String userCross) {
            this.userCross = userCross;
        }

        public String getUserAdd() {
            return userAdd;
        }

        public void setUserAdd(String userAdd) {
            this.userAdd = userAdd;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }
    }


    /**
     * Fee schedule structure (base fee + VIP tiers + MM tiers)
     */
    public static class FeeSchedule {

        /**
         * Base taker fee rate
         */
        private String cross;

        /**
         * Base maker fee rate
         */
        private String add;

        /**
         * Base spot taker fee rate
         */
        private String spotCross;

        /**
         * Base spot maker fee rate
         */
        private String spotAdd;

        /**
         * VIP tiers and Market Maker tiers
         */
        private Tiers tiers;

        /**
         * Maximum referral discount
         */
        private String referralDiscount;

        /**
         * Staking discount tiers
         */
        private List<StakingDiscountTier> stakingDiscountTiers;

        public String getCross() {
            return cross;
        }

        public void setCross(String cross) {
            this.cross = cross;
        }

        public String getAdd() {
            return add;
        }

        public void setAdd(String add) {
            this.add = add;
        }

        public String getSpotCross() {
            return spotCross;
        }

        public void setSpotCross(String spotCross) {
            this.spotCross = spotCross;
        }

        public String getSpotAdd() {
            return spotAdd;
        }

        public void setSpotAdd(String spotAdd) {
            this.spotAdd = spotAdd;
        }

        public Tiers getTiers() {
            return tiers;
        }

        public void setTiers(Tiers tiers) {
            this.tiers = tiers;
        }

        public String getReferralDiscount() {
            return referralDiscount;
        }

        public void setReferralDiscount(String referralDiscount) {
            this.referralDiscount = referralDiscount;
        }

        public List<StakingDiscountTier> getStakingDiscountTiers() {
            return stakingDiscountTiers;
        }

        public void setStakingDiscountTiers(List<StakingDiscountTier> stakingDiscountTiers) {
            this.stakingDiscountTiers = stakingDiscountTiers;
        }
    }


    /**
     * VIP tiers and Market Maker tiers container
     */
    public static class Tiers {

        /**
         * VIP tier list based on 30-day trading volume
         */
        private List<VipTier> vip;

        /**
         * Market maker rebate tiers
         */
        private List<MmTier> mm;

        public List<VipTier> getVip() {
            return vip;
        }

        public void setVip(List<VipTier> vip) {
            this.vip = vip;
        }

        public List<MmTier> getMm() {
            return mm;
        }

        public void setMm(List<MmTier> mm) {
            this.mm = mm;
        }
    }


    /**
     * VIP tier definition
     */
    public static class VipTier {

        /**
         * 30-day notional volume cutoff to reach this VIP tier
         */
        private String ntlCutoff;

        /**
         * Taker fee rate at this VIP tier
         */
        private String cross;

        /**
         * Maker fee rate at this VIP tier
         */
        private String add;

        /**
         * Spot taker fee rate at this VIP tier
         */
        private String spotCross;

        /**
         * Spot maker fee rate at this VIP tier
         */
        private String spotAdd;

        public String getNtlCutoff() {
            return ntlCutoff;
        }

        public void setNtlCutoff(String ntlCutoff) {
            this.ntlCutoff = ntlCutoff;
        }

        public String getCross() {
            return cross;
        }

        public void setCross(String cross) {
            this.cross = cross;
        }

        public String getAdd() {
            return add;
        }

        public void setAdd(String add) {
            this.add = add;
        }

        public String getSpotCross() {
            return spotCross;
        }

        public void setSpotCross(String spotCross) {
            this.spotCross = spotCross;
        }

        public String getSpotAdd() {
            return spotAdd;
        }

        public void setSpotAdd(String spotAdd) {
            this.spotAdd = spotAdd;
        }
    }


    /**
     * Market maker rebate tier
     */
    public static class MmTier {

        /**
         * Maker volume fraction cutoff to qualify for rebate
         */
        private String makerFractionCutoff;

        /**
         * Maker rebate (negative fee means rebate)
         */
        private String add;

        public String getMakerFractionCutoff() {
            return makerFractionCutoff;
        }

        public void setMakerFractionCutoff(String makerFractionCutoff) {
            this.makerFractionCutoff = makerFractionCutoff;
        }

        public String getAdd() {
            return add;
        }

        public void setAdd(String add) {
            this.add = add;
        }
    }


    /**
     * Staking discount tier definition
     */
    public static class StakingDiscountTier {

        /**
         * Percentage of max token supply staked
         */
        private String bpsOfMaxSupply;

        /**
         * Fee discount at this staking level
         */
        private String discount;

        public String getBpsOfMaxSupply() {
            return bpsOfMaxSupply;
        }

        public void setBpsOfMaxSupply(String bpsOfMaxSupply) {
            this.bpsOfMaxSupply = bpsOfMaxSupply;
        }

        public String getDiscount() {
            return discount;
        }

        public void setDiscount(String discount) {
            this.discount = discount;
        }
    }


    /**
     * Active staking discount applied to the user
     */

    public static class ActiveStakingDiscount {

        /**
         * Current staking level (percentage of max supply)
         */
        private String bpsOfMaxSupply;

        /**
         * Current fee discount
         */
        private String discount;

        public String getBpsOfMaxSupply() {
            return bpsOfMaxSupply;
        }

        public void setBpsOfMaxSupply(String bpsOfMaxSupply) {
            this.bpsOfMaxSupply = bpsOfMaxSupply;
        }

        public String getDiscount() {
            return discount;
        }

        public void setDiscount(String discount) {
            this.discount = discount;
        }
    }
}
