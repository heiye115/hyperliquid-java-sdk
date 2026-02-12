package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * openOrders returned unexecuted order entity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OpenOrder {

    /**
     * Currency name or Spot index (e.g., "BTC", "@107")
     */
    private String coin;
    /**
     * Limit price, string format, e.g., "29792.0"
     */
    private String limitPx;
    /**
     * Order ID
     */
    private Long oid;
    /**
     * Direction string (e.g., "A"/"B", or "Buy"/"Sell", etc., may vary across platforms), keep as is
     */
    private String side;
    /**
     * Order quantity, string format
     */
    private String sz;
    /**
     * Creation timestamp (milliseconds)
     */
    private Long timestamp;
}