import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Advanced Close Position OrderRequest Examples.
 * <p>
 * This example demonstrates advanced close-position helpers in
 * {@link OrderRequest.Close}, including:
 * <ul>
 *     <li>Market close-all orders</li>
 *     <li>Market close orders with explicit direction</li>
 *     <li>Limit close orders with custom TIF strategies and Cloid</li>
 * </ul>
 * It complements {@link io.github.hyperliquid.sdk.examples.ExampleCloseAll}
 * by showing how to construct close orders directly via {@link OrderRequest}.
 * </p>
 */
public class ExampleCloseAdvanced {

    /**
     * Entry point for advanced close order examples.
     * <p>
     * Environment variables required:
     * <ul>
     *     <li>PRIMARY_WALLET_ADDRESS: Main wallet address</li>
     *     <li>API_WALLET_PRIVATE_KEY: API wallet private key</li>
     * </ul>
     * The example uses an API wallet on testnet and demonstrates multiple
     * ways to close positions using {@link OrderRequest.Close} helpers.
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

        // ==================== 1. Market close-all using OrderRequest.Close.marketAll ====================
        System.out.println("\n--- Close: marketAll entire position ---");
        try {
            // Close entire ETH position at market price
            OrderRequest marketAll = OrderRequest.Close.marketAll("ETH");
            Order order = exchange.order(marketAll);
            System.out.println("Market close-all status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Market close-all failed: " + e.getMessage());
        }

        // ==================== 2. Market close with explicit direction ====================
        System.out.println("\n--- Close: market close with explicit direction ---");
        try {
            // Close part of a short position on ETH (isBuy=true to buy back)
            OrderRequest marketCloseShort = OrderRequest.Close.market("ETH", true, "0.01");
            Order order = exchange.order(marketCloseShort);
            System.out.println("Market close (explicit direction) status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Market close (explicit direction) failed: " + e.getMessage());
        }

        // ==================== 3. Limit close with custom TIF and Cloid ====================
        System.out.println("\n--- Close: limit close with TIF and Cloid ---");
        try {
            // Close part of a long position at limit price with IOC TIF
            OrderRequest limitClose = OrderRequest.Close.limit(
                    Tif.IOC,
                    "ETH",
                    false, // Sell to close a long position
                    "0.01",
                    "3800.0",
                    Cloid.auto()
            );
            Order order = exchange.order(limitClose);
            System.out.println("Limit close status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Limit close failed: " + e.getMessage());
        }

        System.out.println("\n=== ExampleCloseAdvanced execution completed ===");
    }
}

