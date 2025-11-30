import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.ClearinghouseState;
import io.github.hyperliquid.sdk.model.info.L2Book;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.JSONUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive Example: Demonstrates main features of Hyperliquid Java SDK
 * <p>
 * Features included:
 * 1. Client initialization and configuration
 * 2. Market data queries (order book, user state)
 * 3. Various order types (limit, market, bulk)
 * 4. Position management (partial close, full close, close all)
 * 5. Leverage adjustment
 * </p>
 */
public class ExampleComprehensive {
    
    public static void main(String[] args) throws JsonProcessingException {
        // ==================== 1. Client Initialization ====================
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank()) {
            throw new IllegalStateException("Please set HYPERLIQUID_PRIVATE_KEY environment variable");
        }

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()  // Use testnet
                .addPrivateKey(pk)
                .timeout(15)   // Set timeout to 15 seconds
                .build();

        Info info = client.getInfo();
        Exchange exchange = client.getSingleExchange();
        String address = client.getSingleAddress();

        System.out.println("=== Hyperliquid Java SDK Comprehensive Example ===");
        System.out.println("Wallet address: " + address + "\n");

        // ==================== 2. Query Market Data ====================
        System.out.println("--- 2. Market Data Query ---");
        
        // Query ETH order book
        L2Book book = info.l2Book("ETH");
        if (book != null && book.getLevels() != null && !book.getLevels().isEmpty()) {
            System.out.println("ETH Order Book:");
            System.out.println("  Best bid price: " + book.getLevels().get(0).get(0).getPx());
            System.out.println("  Best bid size: " + book.getLevels().get(0).get(0).getSz());
            System.out.println("  Best ask price: " + book.getLevels().get(1).get(0).getPx());
            System.out.println("  Best ask size: " + book.getLevels().get(1).get(0).getSz());
        }

        // Query account state
        ClearinghouseState state = info.userState(address.toLowerCase());
        if (state != null) {
            System.out.println("\nAccount Info:");
            System.out.println("  Account value: $" + state.getMarginSummary().getAccountValue());
            System.out.println("  Total margin used: $" + state.getMarginSummary().getTotalMarginUsed());
        }

        // ==================== 3. Limit Order Open ====================
        System.out.println("\n--- 3. Limit Order Open Example ---");
        try {
            // Buy 0.01 ETH at 1800.0
            OrderRequest limitReq = OrderRequest.Open.limit(Tif.GTC, "ETH", true, 0.01, 1800.0);
            Order limitOrder = exchange.order(limitReq);
            System.out.println("Limit order status: " + limitOrder.getStatus());
        } catch (HypeError e) {
            System.err.println("Limit order failed: " + e.getMessage());
        }

        // ==================== 4. Market Order Open ====================
        System.out.println("\n--- 4. Market Order Open Example ---");
        try {
            // Buy 0.01 ETH at market price
            OrderRequest marketReq = OrderRequest.Open.market("ETH", true, 0.01);
            Order marketOrder = exchange.order(marketReq);
            System.out.println("Market order status: " + marketOrder.getStatus());
        } catch (HypeError e) {
            System.err.println("Market order failed: " + e.getMessage());
        }

        // ==================== 5. Bulk Orders ====================
        System.out.println("\n--- 5. Bulk Orders Example ---");
        try {
            List<OrderRequest> bulkReqs = Arrays.asList(
                    OrderRequest.Open.limit(Tif.GTC, "BTC", true, 0.001, 95000.0),
                    OrderRequest.Open.limit(Tif.GTC, "SOL", true, 1.0, 200.0)
            );
            JsonNode bulkResult = exchange.bulkOrders(bulkReqs);
            System.out.println("Bulk orders result: " + JSONUtil.writeValueAsString(bulkResult));
        } catch (HypeError e) {
            System.err.println("Bulk orders failed: " + e.getMessage());
        }

        // ==================== 6. Adjust Leverage ====================
        System.out.println("\n--- 6. Adjust Leverage Example ---");
        try {
            // Set ETH to 10x cross margin
            exchange.updateLeverage("ETH", true, 10);
            System.out.println("Leverage adjusted to 10x cross margin");
        } catch (HypeError e) {
            System.err.println("Leverage adjustment failed: " + e.getMessage());
        }

        // ==================== 7. Partial Close ====================
        System.out.println("\n--- 7. Partial Close Example ---");
        try {
            // Close 0.005 ETH at market (partial close, 3% custom slippage)
            Order partialClose = exchange.closePositionMarket("ETH", 0.005, 0.03, Cloid.auto());
            System.out.println("Partial close order status: " + partialClose.getStatus());
        } catch (HypeError e) {
            System.err.println("Partial close failed: " + e.getMessage());
        }

        // ==================== 8. Full Close Single Coin ====================
        System.out.println("\n--- 8. Full Close Example ---");
        try {
            // Close entire ETH position at market
            Order fullClose = exchange.closePositionMarket("ETH");
            System.out.println("Full close order status: " + fullClose.getStatus());
        } catch (HypeError e) {
            System.err.println("Full close failed: " + e.getMessage());
        }

        // ==================== 9. Limit Close ====================
        System.out.println("\n--- 9. Limit Close Example ---");
        try {
            // Close BTC position at limit price 96000.0
            Order limitClose = exchange.closePositionLimit(Tif.GTC, "BTC", 96000.0, Cloid.auto());
            System.out.println("Limit close order status: " + limitClose.getStatus());
        } catch (HypeError e) {
            System.err.println("Limit close failed: " + e.getMessage());
        }

        // ==================== 10. Close All Positions ====================
        System.out.println("\n--- 10. Close All Positions Example ---");
        try {
            // Close all positions for all coins in batch
            JsonNode closeAllResult = exchange.closeAllPositions();
            System.out.println("Close all positions result: " + JSONUtil.writeValueAsString(closeAllResult));
        } catch (HypeError e) {
            System.err.println("Close all positions failed: " + e.getMessage());
        }

        System.out.println("\n=== Example execution completed ===");
    }
}
