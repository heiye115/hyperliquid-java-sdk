package io.github.hyperliquid.sdk.model.info;

/**
 * Retrieve a user's fills
 * User recent trades
 **/
public class UserFill {

    /** Currency (e.g., "BTC" or Spot index "@107") */
    private String coin;
    /** Execution price (string) */
    private String px;
    /** Execution quantity (string) */
    private String sz;
    /** Direction (A/B or Buy/Sell) */
    private String side;
    /** Execution timestamp (milliseconds) */
    private Long time;
    /** Starting position size at execution (string) */
    private String startPosition;
    /** Direction description (e.g., open/close, etc.) */
    private String dir;
    /** Closed profit and loss (string) */
    private String closedPnl;
    /** Execution hash */
    private String hash;
    /** Order ID */
    private Long oid;
    /** Whether it is a crossed execution */
    private Boolean crossed;
    /** Fee (string) */
    private String fee;
    /** Execution sequence number (tid) */
    private Long tid;
    /** Fee token identifier */
    private String feeToken;
    /** TWAP strategy ID (if sliced execution) */
    private String twapId;
    /** Builder fee (string, if applicable) */
    private String builderFee;


    // Utility method - determine if it is a spot trade
    public boolean isSpotTrade() {
        return coin != null && coin.startsWith("@");
    }

    // Utility method - determine if it is a perpetual contract trade
    public boolean isPerpTrade() {
        return coin != null && !coin.startsWith("@");
    }

    // Utility method - get asset ID (if it is a spot trade)
    public String getAssetId() {
        if (isSpotTrade() && coin != null) {
            return coin.substring(1); // Remove "@" symbol
        }
        return coin;
    }

    // Getter and Setter methods
    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

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

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(String startPosition) {
        this.startPosition = startPosition;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getClosedPnl() {
        return closedPnl;
    }

    public void setClosedPnl(String closedPnl) {
        this.closedPnl = closedPnl;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getOid() {
        return oid;
    }

    public void setOid(Long oid) {
        this.oid = oid;
    }

    public Boolean getCrossed() {
        return crossed;
    }

    public void setCrossed(Boolean crossed) {
        this.crossed = crossed;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public Long getTid() {
        return tid;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

    public String getFeeToken() {
        return feeToken;
    }

    public void setFeeToken(String feeToken) {
        this.feeToken = feeToken;
    }

    public String getTwapId() {
        return twapId;
    }

    public void setTwapId(String twapId) {
        this.twapId = twapId;
    }

    public String getBuilderFee() {
        return builderFee;
    }

    public void setBuilderFee(String builderFee) {
        this.builderFee = builderFee;
    }
}