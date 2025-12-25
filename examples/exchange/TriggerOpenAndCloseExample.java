package io.github.hyperliquid.sdk.examples.exchange;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Trigger Entry and Close Order Examples.
 * <p>
 * This example demonstrates how to use trigger-related convenience methods in
 * {@link OrderRequest.Open} and {@link OrderRequest.Close}, including:
 * <ul>
 *     <li>Breakout entry orders (breakoutAbove / breakoutBelow)</li>
 *     <li>Take-profit close orders</li>
 *     <li>Stop-loss close orders</li>
 * </ul>
 * These methods are useful for building breakout strategies and automatic
 * take-profit / stop-loss rules around existing positions.
 * </p>
 */
public class TriggerOpenAndCloseExample {

    /**
     * Entry point for trigger order examples.
     * <p>
     * Environment variables required:
     * <ul>
     *     <li>PRIMARY_WALLET_ADDRESS: Main wallet address</li>
     *     <li>API_WALLET_PRIVATE_KEY: API wallet private key</li>
     * </ul>
     * Note: For take-profit and stop-loss close orders to be effective, you
     * should already hold corresponding positions on the specified symbol.
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

        // ==================== 1. Breakout long entry (breakoutAbove) ====================
        System.out.println("\n--- Trigger: breakoutAbove long entry ---");
        try {
            // Enter long when price breaks above 3500.0
            OrderRequest breakoutLong = OrderRequest.Open.breakoutAbove("ETH", "0.01", "3500.0");
            Order order = exchange.order(breakoutLong);
            System.out.println("Breakout long order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Breakout long order failed: " + e.getMessage());
        }

        // ==================== 2. Breakout short entry (breakoutBelow) ====================
        System.out.println("\n--- Trigger: breakoutBelow short entry ---");
        try {
            // Enter short when price breaks below 3200.0
            OrderRequest breakoutShort = OrderRequest.Open.breakoutBelow("ETH", "0.01", "3200.0");
            Order order = exchange.order(breakoutShort);
            System.out.println("Breakout short order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Breakout short order failed: " + e.getMessage());
        }

        // ==================== 3. Take-profit close order ====================
        System.out.println("\n--- Trigger: take-profit close order ---");
        try {
            // Close part of a long position when price breaks above 3600.0
            OrderRequest takeProfit = OrderRequest.Close.takeProfitForLong("ETH", "0.01", "3600.0");
            Order order = exchange.order(takeProfit);
            System.out.println("Take-profit close order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Take-profit close order failed: " + e.getMessage());
        }

        // ==================== 4. Stop-loss close order ====================
        System.out.println("\n--- Trigger: stop-loss close order ---");
        try {
            // Close part of a long position when price breaks below 3400.0
            OrderRequest stopLoss = OrderRequest.Close.stopLossForLong("ETH", "0.01", "3400.0");
            Order order = exchange.order(stopLoss);
            System.out.println("Stop-loss close order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Stop-loss close order failed: " + e.getMessage());
        }

        System.out.println("\n=== TriggerOpenAndCloseExample execution completed ===");
    }
}
