import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Market Order Open Example: Demonstrates how to open a position with market orders
 * <p>
 * Market Order Features:
 * 1. Immediate execution (IOC - Immediate Or Cancel)
 * 2. Executes at the best available market price
 * 3. Suitable for quick position entry scenarios
 * </p>
 */
public class ExampleMarketOpen {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank())
            throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Exchange ex = client.getSingleExchange();
        
        // Open long position at market: Buy 0.01 ETH
        OrderRequest req = OrderRequest.Open.market("ETH", true, "0.01");
        try {
            Order order = ex.order(req);
            System.out.println("Order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Failed to open position: " + e.getMessage());
        }
    }
}
