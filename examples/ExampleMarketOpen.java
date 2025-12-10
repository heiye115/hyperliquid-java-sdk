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
        // Recommended: Use API Wallet for better security
        // API Wallet: Sub-wallet authorized by main wallet, with limited permissions, main private key not exposed
        // Main Private Key: Direct use of main wallet private key, full control, higher risk
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");  // Primary wallet address
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");   // API wallet private key
        if (primaryWalletAddress == null || apiWalletPrivateKey == null)
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS and API_WALLET_PRIVATE_KEY");

        // Build client with API Wallet (Recommended)
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();
        
        // Alternative: Build client with main private key (Not recommended for production)
        // String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        // HyperliquidClient client = HyperliquidClient.builder()
        //         .testNetUrl()
        //         .addPrivateKey(pk)
        //         .build();

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
