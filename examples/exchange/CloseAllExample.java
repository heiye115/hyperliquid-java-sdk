package io.github.hyperliquid.sdk.examples.exchange;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.BulkOrder;
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
public class CloseAllExample {
    public static void main(String[] args) {
        // Recommended: Use API Wallet for better security
        // API Wallet: Sub-wallet authorized by main wallet, with limited permissions,
        // main private key not exposed
        // Main Private Key: Direct use of main wallet private key, full control, higher
        // risk
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS"); // Primary wallet address
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY"); // API wallet private key
        if (primaryWalletAddress == null || apiWalletPrivateKey == null)
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS and API_WALLET_PRIVATE_KEY");

        // Build client with API Wallet (Recommended)
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        Exchange ex = client.getExchange();

        // Example 1: Close entire ETH position at market (auto-infer direction and
        // size)
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
            BulkOrder result = ex.closeAllPositions();
            System.out.println("Close all positions result: " + result);
        } catch (HypeError e) {
            System.err.println("Close all positions failed: " + e.getMessage());
        }
    }
}
