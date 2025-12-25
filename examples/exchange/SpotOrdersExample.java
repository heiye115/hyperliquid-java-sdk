package io.github.hyperliquid.sdk.examples.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.utils.HypeError;

/**
 * Spot Trading OrderRequest Examples.
 * <p>
 * This example focuses on spot trading convenience methods in
 * {@link OrderRequest.Open}, including:
 * <ul>
 *     <li>Spot market buy and sell orders</li>
 *     <li>Spot limit buy and sell orders</li>
 * </ul>
 * It demonstrates how to submit spot orders via the same unified
 * {@link Exchange#order(OrderRequest)} interface used for perpetual
 * contracts.
 * </p>
 */
public class SpotOrdersExample {

    /**
     * Entry point for spot trading examples.
     * <p>
     * Environment variables required:
     * <ul>
     *     <li>PRIMARY_WALLET_ADDRESS: Main wallet address</li>
     *     <li>API_WALLET_PRIVATE_KEY: API wallet private key</li>
     * </ul>
     * The example uses an API wallet on testnet and sends several
     * spot orders using {@link OrderRequest.Open} helpers.
     * </p>
     *
     * @param args Command line arguments (unused)
     * @throws JsonProcessingException Thrown when printing JSON responses fails
     */
    public static void main(String[] args) throws JsonProcessingException {
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

        // ==================== 1. Spot market buy ====================
        System.out.println("\n--- Spot market buy example ---");
        try {
            OrderRequest spotBuy = OrderRequest.Open.spotMarketBuy("PURR", "100.0");
            Order order = exchange.order(spotBuy);
            System.out.println("Spot market buy status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Spot market buy failed: " + e.getMessage());
        }

        // ==================== 2. Spot market sell ====================
        System.out.println("\n--- Spot market sell example ---");
        try {
            OrderRequest spotSell = OrderRequest.Open.spotMarketSell("PURR", "50.0");
            Order order = exchange.order(spotSell);
            System.out.println("Spot market sell status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Spot market sell failed: " + e.getMessage());
        }

        // ==================== 3. Spot limit buy ====================
        System.out.println("\n--- Spot limit buy example ---");
        try {
            OrderRequest spotLimitBuy = OrderRequest.Open.spotLimitBuy("PURR", "100.0", "0.05");
            Order order = exchange.order(spotLimitBuy);
            System.out.println("Spot limit buy status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Spot limit buy failed: " + e.getMessage());
        }

        // ==================== 4. Spot limit sell ====================
        System.out.println("\n--- Spot limit sell example ---");
        try {
            OrderRequest spotLimitSell = OrderRequest.Open.spotLimitSell("PURR", "100.0", "0.06");
            Order order = exchange.order(spotLimitSell);
            System.out.println("Spot limit sell status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Spot limit sell failed: " + e.getMessage());
        }

        // Optional: demonstrate how to inspect raw JSON via bulkOrders
        // (in real scenarios you would likely submit multiple spot orders together)
        System.out.println("\n=== SpotOrdersExample execution completed ===");
    }
}

