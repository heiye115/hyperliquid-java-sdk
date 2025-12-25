package io.github.hyperliquid.sdk.examples.exchange;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.ModifyOrder;
import io.github.hyperliquid.sdk.model.order.ModifyOrderRequest;
import io.github.hyperliquid.sdk.model.order.TriggerOrderType;

import java.util.List;

/**
 * Modify Order Example: Demonstrates how to modify existing open orders.
 * <p>
 * Modification Features:
 * 1. Modify a single order's parameters (price, size, type)
 * 2. Batch modify multiple orders in a single request
 * 3. Support for both standard and trigger orders
 * </p>
 */

public class ModifyOrderExample {

    public static void main(String[] args) {
        // Load configuration from environment variables
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");

        if (primaryWalletAddress == null || apiWalletPrivateKey == null) {
            throw new IllegalStateException("Missing required environment variables");
        }

        // Initialize the client
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        Exchange exchange = client.getExchange();

        // Example 1: Modify a single order
        // Note: You need a valid Order ID (OID) from an existing open order
        long existingOid = 123456789L; // Replace with a real OID
        modifySingleOrder(exchange, existingOid);

        // Example 2: Batch modify multiple orders
        List<Long> existingOids = List.of(123456789L, 987654321L); // Replace with real OIDs
        batchModifyOrders(exchange, existingOids);
    }

    /**
     * Demonstrates how to modify a single order.
     *
     * @param exchange The exchange API instance
     * @param oid      The Order ID of the order to modify
     */
    public static void modifySingleOrder(Exchange exchange, long oid) {
        System.out.println("--- Example 1: Modifying single order (OID: " + oid + ") ---");

        try {
            // Create a modification request by OID
            ModifyOrderRequest req = ModifyOrderRequest.byOid("BTC", oid);

            // Set new parameters
            req.setBuy(false);              // Change side if needed
            req.setLimitPx("86200.0");      // New limit price
            req.setSz("0.001");             // New size
            req.setReduceOnly(true);        // Set reduce only

            // Optionally change the order type (e.g., to a stop loss)
            req.setOrderType(TriggerOrderType.sl("86200.0", true));

            // Execute modification
            ModifyOrder result = exchange.modifyOrder(req);
            System.out.println("Modification successful. Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Demonstrates how to modify multiple orders in a single batch request.
     *
     * @param exchange The exchange API instance
     * @param oids     A list of Order IDs to modify
     */
    public static void batchModifyOrders(Exchange exchange, List<Long> oids) {
        System.out.println("--- Example 2: Batch modifying " + oids.size() + " orders ---");
        if (oids.size() < 2) {
            System.out.println("Batch modification requires at least 2 OIDs for demonstration");
            return;
        }

        try {
            // Prepare modification requests
            ModifyOrderRequest req1 = ModifyOrderRequest.byOid("BTC", oids.getFirst());
            req1.setLimitPx("85100.0");
            req1.setSz("0.002");
            req1.setOrderType(TriggerOrderType.sl("85100.0", true));

            ModifyOrderRequest req2 = ModifyOrderRequest.byOid("BTC", oids.get(1));
            req2.setLimitPx("89100.0");
            req2.setSz("0.002");
            req2.setOrderType(TriggerOrderType.tp("89100.0", true));

            // Execute batch modification
            ModifyOrder result = exchange.modifyOrders(List.of(req1, req2));
            System.out.println("Batch modification successful. Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
