import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.JSONUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Bulk Orders Example: Demonstrates how to submit bulk orders for improved efficiency
 * <p>
 * Bulk Order Advantages:
 * 1. Reduces number of network requests
 * 2. Improves order submission efficiency
 * 3. Supports atomic operations (order groups)
 * </p>
 */
public class ExampleBulkOrders {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank())
            throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Exchange ex = client.getSingleExchange();

        // Example 1: Bulk limit orders to open positions for multiple coins
        List<OrderRequest> openOrders = Arrays.asList(
                OrderRequest.Open.limit(Tif.GTC, "BTC", true, 0.001, 95000.0),
                OrderRequest.Open.limit(Tif.GTC, "ETH", true, 0.01, 3500.0),
                OrderRequest.Open.limit(Tif.GTC, "SOL", true, 1.0, 200.0)
        );

        try {
            JsonNode result = ex.bulkOrders(openOrders);
            System.out.println("Bulk open result: " + JSONUtil.writeValueAsString(result));
        } catch (HypeError e) {
            System.err.println("Bulk open failed: " + e.getMessage());
        }

        // Example 2: Bulk market orders to close positions
        List<OrderRequest> closeOrders = Arrays.asList(
                OrderRequest.Close.market("BTC", 0.001, null),
                OrderRequest.Close.market("ETH", 0.01, null),
                OrderRequest.Close.market("SOL", 1.0, null)
        );

        try {
            JsonNode result = ex.bulkOrders(closeOrders);
            System.out.println("Bulk close result: " + JSONUtil.writeValueAsString(result));
        } catch (HypeError e) {
            System.err.println("Bulk close failed: " + e.getMessage());
        }
    }
}
