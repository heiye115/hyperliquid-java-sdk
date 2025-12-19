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
 * It complements {@link io.github.hyperliquid.sdk.examples.CloseAllExample}
 * by showing how to construct close orders directly via {@link OrderRequest}.
 * </p>
 */
public class CloseAdvancedExample {

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

        // ==================== 4. Take-profit helpers for long/short positions ====================
        System.out.println("\n--- Close: take-profit helpers for long/short positions ---");
        try {
            // Take-profit for a long position: internally sends a sell order when trigger price is reached
            OrderRequest tpLong = OrderRequest.Close.takeProfitForLong(
                    "ETH",
                    "0.01",
                    "3900.0"
            );
            Order tpLongOrder = exchange.order(tpLong);
            System.out.println("Take-profit for long status: " + tpLongOrder.getStatus());

            // Take-profit for a short position: internally sends a buy order when trigger price is reached
            OrderRequest tpShort = OrderRequest.Close.takeProfitForShort(
                    "ETH",
                    "0.01",
                    "3500.0"
            );
            Order tpShortOrder = exchange.order(tpShort);
            System.out.println("Take-profit for short status: " + tpShortOrder.getStatus());
        } catch (HypeError e) {
            System.err.println("Take-profit helpers failed: " + e.getMessage());
        }

        // ==================== 5. Stop-loss helpers for long/short positions ====================
        System.out.println("\n--- Close: stop-loss helpers for long/short positions ---");
        try {
            // Stop-loss for a long position: internally sends a sell order when trigger price is reached
            OrderRequest slLong = OrderRequest.Close.stopLossForLong(
                    "ETH",
                    "0.01",
                    "3400.0"
            );
            Order slLongOrder = exchange.order(slLong);
            System.out.println("Stop-loss for long status: " + slLongOrder.getStatus());

            // Stop-loss for a short position: internally sends a buy order when trigger price is reached
            OrderRequest slShort = OrderRequest.Close.stopLossForShort(
                    "ETH",
                    "0.01",
                    "3650.0"
            );
            Order slShortOrder = exchange.order(slShort);
            System.out.println("Stop-loss for short status: " + slShortOrder.getStatus());
        } catch (HypeError e) {
            System.err.println("Stop-loss helpers failed: " + e.getMessage());
        }

        // ==================== 6. Conditional take-profit orders ====================
        System.out.println("\n--- Close: conditional take-profit orders ---");
        try {
            // Conditional limit take profit order
            // Places a limit order when the market price moves above the trigger price
            OrderRequest conditionalTp = OrderRequest.builder()
                    .perp("ETH")
                    .sell("0.01") // Sell to close a long position, or .buy("0.01") to close a short position
                    .limitPrice("3000.0") // Take profit price
                    .stopAbove("3000.0") // Trigger price
                    .reduceOnly() // Ensures the order only reduces an existing position
                    .build();
            Order conditionalTpOrder = exchange.order(conditionalTp);
            System.out.println("Conditional take-profit status: " + conditionalTpOrder.getStatus());
        } catch (HypeError e) {
            System.err.println("Conditional take-profit order failed: " + e.getMessage());
        }

        // ==================== 7. Conditional stop-loss orders ====================
        System.out.println("\n--- Close: conditional stop-loss orders ---");
        try {
            // Conditional limit stop loss order
            // Places a limit order when the market price moves below the trigger price
            OrderRequest conditionalSl = OrderRequest.builder()
                    .perp("ETH")
                    .sell("0.01") // Sell to close a long position, or .buy("0.01") to close a short position
                    .limitPrice("2900.0") // Stop loss price
                    .stopBelow("2900.0") // Trigger price
                    .reduceOnly() // Ensures the order only reduces an existing position
                    .build();
            Order conditionalSlOrder = exchange.order(conditionalSl);
            System.out.println("Conditional stop-loss status: " + conditionalSlOrder.getStatus());
        } catch (HypeError e) {
            System.err.println("Conditional stop-loss order failed: " + e.getMessage());
        }

        System.out.println("\n=== CloseAdvancedExample execution completed ===");
    }
}
