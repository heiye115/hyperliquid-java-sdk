package io.github.hyperliquid.sdk.model.info;

import lombok.Data;

/***
 * Funding rate history.
 **/
@Data
public class FundingHistory {

    /**
     * Currency name
     **/
    private String coin;

    /***
     * Funding rate
     **/
    private String fundingRate;

    /***
     * Premium rate
     **/
    private String premium;

    /***
     * Timestamp (milliseconds)
     **/
    private Long time;
}