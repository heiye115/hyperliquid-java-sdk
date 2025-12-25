package io.github.hyperliquid.sdk.examples.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.BulkOrder;
import io.github.hyperliquid.sdk.model.order.OrderGroup;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.JSONUtil;

/**
 * Entry With Take-Profit and Stop-Loss Order Group Examples.
 * <p>
 * This example demonstrates how to use {@link OrderRequest#entryWithTpSl()}
 * and {@link io.github.hyperliquid.sdk.model.order.OrderWithTpSlBuilder} to
 * build order groups for bulk submission via
 * {@link Exchange#bulkOrders(OrderGroup)}.
 * <p>
 * It covers two main scenarios:
 * <ul>
 *     <li>normalTpsl: Open a new position with attached TP/SL orders</li>
 *     <li>positionTpsl: Add TP/SL orders to an existing position</li>
 * </ul>
 * </p>
 */
public class EntryWithTpSlExample {

    /**
     * Entry point for entry-with-TP/SL group examples.
     * <p>
     * Environment variables required:
     * <ul>
     *     <li>PRIMARY_WALLET_ADDRESS: Main wallet address</li>
     *     <li>API_WALLET_PRIVATE_KEY: API wallet private key</li>
     * </ul>
     * The example uses an API wallet on testnet and submits two order
     * groups to demonstrate normalTpsl and positionTpsl flows.
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

        // ==================== 1. normalTpsl: entry + TP + SL ====================
        System.out.println("\n--- entryWithTpSl: normalTpsl example (entry + TP + SL) ---");
        try {
            OrderGroup normalTpsl = OrderRequest.entryWithTpSl()
                    .perp("ETH")
                    .buy("0.01")
                    .entryPrice("3500.0")
                    .takeProfit("3600.0")
                    .stopLoss("3400.0")
                    .entryTif(Tif.GTC)
                    .buildNormalTpsl();

            BulkOrder result = exchange.bulkOrders(normalTpsl);
            System.out.println("normalTpsl bulkOrders result: " + JSONUtil.writeValueAsString(result));
        } catch (HypeError e) {
            System.err.println("normalTpsl bulkOrders failed: " + e.getMessage());
        }

        // ==================== 2. positionTpsl: add TP/SL to existing position ====================
        System.out.println("\n--- entryWithTpSl: positionTpsl example (TP/SL for existing position) ---");
        try {
            // Close 0.01 ETH long position with TP/SL
            OrderGroup positionTpsl = OrderRequest.entryWithTpSl()
                    .perp("ETH")
                    .closePosition("0.01", true)
                    .takeProfit("3600.0")
                    .stopLoss("3400.0")
                    .buildPositionTpsl();

            BulkOrder result = exchange.bulkOrders(positionTpsl);
            System.out.println("positionTpsl bulkOrders result: " + JSONUtil.writeValueAsString(result));
        } catch (HypeError e) {
            System.err.println("positionTpsl bulkOrders failed: " + e.getMessage());
        }

        System.out.println("\n=== EntryWithTpSlExample execution completed ===");
    }
}

