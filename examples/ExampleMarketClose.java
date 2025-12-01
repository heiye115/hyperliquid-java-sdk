import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Market Order Close Example: Demonstrates how to close a position with market orders
 * <p>
 * Market Close Features:
 * 1. Fast execution at market price
 * 2. Supports partial or full position closure
 * 3. Can use Cloid for order tracking
 * </p>
 */
public class ExampleMarketClose {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank())
            throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Exchange ex = client.getSingleExchange();
        
        // Close position at market: Close 0.01 ETH (using auto-generated Cloid for order tracking)
        OrderRequest req = OrderRequest.Close.market("ETH", "0.01", Cloid.auto());
        try {
            Order order = ex.order(req);
            System.out.println("Order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Failed to close position: " + e.getMessage());
        }
    }
}
