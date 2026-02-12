package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/** Asset context (perpetual) indicator collection */
@Data
@ToString
public class AssetCtx {

    /** Current funding rate (string decimal) */
    @JsonProperty("funding")
    private String funding;

    /** Open interest (nominal USD scale, string) */
    @JsonProperty("openInterest")
    private String openInterest;

    /** Previous day closing price (string) */
    @JsonProperty("prevDayPx")
    private String prevDayPx;

    /** Daily nominal trading volume (USD, string) */
    @JsonProperty("dayNtlVlm")
    private String dayNtlVlm;

    /** Perpetual premium (string) */
    @JsonProperty("premium")
    private String premium;

    /** Oracle price (string) */
    @JsonProperty("oraclePx")
    private String oraclePx;

    /** Mark price (string) */
    @JsonProperty("markPx")
    private String markPx;

    /** Mid price (buy/sell mid price, may be null) */
    @JsonProperty("midPx")
    private String midPx;

    /** Impact prices (estimated buy/sell direction execution impact prices, length is 2) */
    @JsonProperty("impactPxs")
    private List<String> impactPxs;

    /** Daily base quantity trading volume (coin quantity, string) */
    @JsonProperty("dayBaseVlm")
    private String dayBaseVlm;
}