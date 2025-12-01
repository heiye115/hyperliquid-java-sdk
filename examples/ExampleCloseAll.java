import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Close Position Examples: Demonstrates different ways to close positions
 * <p>
 * Examples include:
 * 1. Close entire position at market price for a single coin
 * 2. Close entire position at limit price for a single coin
 * 3. Close all positions with one click
 * </p>
 */
public class ExampleCloseAll {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank())
            throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Exchange ex = client.getSingleExchange();
        
        // Example 1: Close entire ETH position at market (auto-infer direction and size)
        try {
            Order mkt = ex.closePositionMarket("ETH");
            System.out.println("Market close status: " + mkt.getStatus());
        } catch (HypeError e) {
            System.err.println("Market close failed: " + e.getMessage());
        }

        // Example 2: Close entire ETH position at limit price
        try {
            Order lmt = ex.closePositionLimit(Tif.GTC, "ETH", "4000.0", Cloid.auto());
            System.out.println("Limit close status: " + lmt.getStatus());
        } catch (HypeError e) {
            System.err.println("Limit close failed: " + e.getMessage());
        }

        // Example 3: Close all positions with one click (batch close)
        try {
            JsonNode result = ex.closeAllPositions();
            System.out.println("Close all positions result: " + result);
        } catch (HypeError e) {
            System.err.println("Close all positions failed: " + e.getMessage());
        }
    }
}
