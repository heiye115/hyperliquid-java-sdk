import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Advanced OrderRequest Builder Examples.
 * <p>
 * This example demonstrates how to use {@link OrderRequest#builder()} to build
 * different types of orders, including:
 * <ul>
 * <li>Perpetual limit orders with TIF strategies</li>
 * <li>Perpetual market orders with custom slippage</li>
 * <li>Trigger orders with stop-above conditions and reduce-only flags</li>
 * <li>Client order ID (cloid) and custom expiration time (expiresAfter)</li>
 * </ul>
 * It is especially useful when you want more fine-grained control compared to
 * the static convenience methods in {@link OrderRequest.Open} and
 * {@link OrderRequest.Close}.
 * </p>
 */
public class ExampleBuilderOrders {

    /**
     * Entry point for the advanced builder examples.
     * <p>
     * Environment variables required:
     * <ul>
     * <li>PRIMARY_WALLET_ADDRESS: Main wallet address</li>
     * <li>API_WALLET_PRIVATE_KEY: API wallet private key</li>
     * </ul>
     * The example uses an API wallet on testnet and submits several sample
     * orders using the builder pattern.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS and API_WALLET_PRIVATE_KEY");
        }

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        Exchange exchange = client.getExchange();

        // ==================== 1. Perpetual limit order via builder
        // ====================
        System.out.println("\n--- Builder: Perpetual limit order (GTC) ---");
        try {
            OrderRequest limitOrder = OrderRequest.builder()
                    .perp("ETH")
                    .buy("0.01")
                    .limitPrice("1800.0")
                    .gtc()
                    .autoCloid()
                    .build();

            Order result = exchange.order(limitOrder);
            System.out.println("Limit order status: " + result.getStatus());
        } catch (HypeError e) {
            System.err.println("Builder limit order failed: " + e.getMessage());
        }

        // ==================== 2. Perpetual market order with custom slippage
        // ====================
        System.out.println("\n--- Builder: Perpetual market order with slippage ---");
        try {
            OrderRequest marketWithSlippage = OrderRequest.builder()
                    .perp("ETH")
                    .sell("0.01")
                    .market("0.05") // 5% slippage tolerance
                    .expiresAfter(60_000L) // expire after 60 seconds
                    .build();

            Order result = exchange.order(marketWithSlippage);
            System.out.println("Market order with slippage status: " + result.getStatus());
        } catch (HypeError e) {
            System.err.println("Builder market order with slippage failed: " + e.getMessage());
        }

        // ==================== 3. Trigger close order with stop-above + reduce-only
        // ====================
        System.out.println("\n--- Builder: Take-profit trigger close order ---");
        try {
            OrderRequest tpTrigger = OrderRequest.builder()
                    .perp("ETH")
                    .sell("0.01") // Close part of a long position
                    .stopAbove("3600.0") // Trigger when price breaks above 3600
                    .marketTrigger() // Execute at market price after trigger
                    .reduceOnly() // Ensure it only reduces existing position
                    .cloid(Cloid.auto())
                    .build();

            Order result = exchange.order(tpTrigger);
            System.out.println("Take-profit trigger order status: " + result.getStatus());
        } catch (HypeError e) {
            System.err.println("Builder trigger close order failed: " + e.getMessage());
        }

        System.out.println("\n=== ExampleBuilderOrders execution completed ===");
    }
}
