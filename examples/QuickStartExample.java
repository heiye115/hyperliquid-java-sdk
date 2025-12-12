
package io.github.hyperliquid.sdk.examples;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.L2Book;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class QuickStartExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuickStartExample.class);

    public static void main(String[] args) {
        // 1. For security, read the private key from an environment variable
        String privateKey = System.getenv("HYPERLIQUID_TESTNET_PRIVATE_KEY");
        if (privateKey == null || privateKey.isEmpty()) {
            LOGGER.error("Error: Environment variable HYPERLIQUID_TESTNET_PRIVATE_KEY is not set.");
            LOGGER.error("Please set it to your testnet private key: export HYPERLIQUID_TESTNET_PRIVATE_KEY=\"0x...\"");
            return;
        }

        // 2. Build the client for the testnet
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl() // Use the testnet environment
                .addPrivateKey(privateKey) // Add your wallet
                .build();

        Info info = client.getInfo();
        Exchange exchange = client.getSingleExchange(); // Get the exchange instance for the added wallet

        // 3. Query market data: Get the L2 book for "ETH"
        try {
            LOGGER.info("Querying L2 book for ETH...");
            L2Book l2Book = info.l2Book("ETH");
            // Print the top 3 levels of bids and asks
            LOGGER.info("Successfully retrieved L2 book for {}:", l2Book.getCoin());
            l2Book.getLevels().get(0).subList(0, 3).forEach(level ->
                    LOGGER.info("  Ask - Price: {}, Size: {}", level.getPx(), level.getSz())
            );
            l2Book.getLevels().get(1).subList(0, 3).forEach(level ->
                    LOGGER.info("  Bid - Price: {}, Size: {}", level.getPx(), level.getSz())
            );
        } catch (HypeError e) {
            LOGGER.error("Failed to query L2 book. Code: {}, Message: {}", e.getCode(), e.getMessage());
        }

        // 4. Execute a trade: Create a limit buy order for ETH
        try {
            LOGGER.info("Placing a limit buy order for ETH...");
            // Create a limit buy order for 0.01 ETH at a price of $1500
            // This order will be canceled if not filled immediately (IOC)
            OrderRequest orderRequest = OrderRequest.builder()
                    .perp("ETH")
                    .buy("0.01")
                    .limitPrice("1500")
                    .orderType(Tif.IOC) // Immediate Or Cancel
                    .build();

            JsonNode response = exchange.order(orderRequest);
            LOGGER.info("Order placed successfully. Response: {}", JSONUtil.toJson(response));

        } catch (HypeError e) {
            // Example of handling a specific error, e.g., insufficient margin
            LOGGER.error("Order placement failed. Code: {}, Message: {}", e.getCode(), e.getMessage(), e);
        }
    }
}
