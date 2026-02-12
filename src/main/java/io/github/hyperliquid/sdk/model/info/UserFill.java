package io.github.hyperliquid.sdk.model.info;

import lombok.Data;

/**
 * Retrieve a user's fills
 * User recent trades
 **/
@Data
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
}