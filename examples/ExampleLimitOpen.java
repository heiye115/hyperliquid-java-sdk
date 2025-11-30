import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Limit Order Open Example: Demonstrates how to open a position with limit orders
 * <p>
 * Limit Order Features:
 * 1. Executes at specified price
 * 2. Supports multiple TIF strategies (GTC, IOC, ALO)
 * 3. Provides precise price control
 * </p>
 */
public class ExampleLimitOpen {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank())
            throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Exchange ex = client.getSingleExchange();
        
        // Open long position with limit order: Buy 0.01 ETH at 1800.0 (GTC - Good Till Cancel)
        OrderRequest req = OrderRequest.Open.limit(Tif.GTC, "ETH", true, 0.01, 1800.0);
        try {
            Order order = ex.order(req);
            System.out.println("Order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Failed to open position: " + e.getMessage());
        }
    }
}
