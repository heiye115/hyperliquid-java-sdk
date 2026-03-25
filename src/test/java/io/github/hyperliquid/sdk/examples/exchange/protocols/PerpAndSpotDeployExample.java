package io.github.hyperliquid.sdk.examples.exchange.protocols;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Perp and Spot Deploy Example: demonstrates builder-deployed DEX flows for
 * registering assets/tokens and placing an order routed to the target DEX.
 */
public class PerpAndSpotDeployExample {
    public static void main(String[] args) {
        // Load deploy target and signer credentials.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        String deployDex = System.getenv("DEPLOY_DEX");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null || deployDex == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS, API_WALLET_PRIVATE_KEY, DEPLOY_DEX");
        }

        // Build authenticated testnet client.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        Exchange exchange = client.getExchange();

        // Register a perp asset definition under the builder-deployed dex.
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("fullName", "Demo Perp");
        schema.put("collateralToken", null);
        schema.put("oracleUpdater", null);
        System.out.println(exchange.perpDeployRegisterAsset(
                deployDex, null, "DEMO", 2, "1", 0, false, schema));

        // Register spot token/pair definitions.
        System.out.println(exchange.spotDeployRegisterToken("DEMOSPOT", 2, 6, 1_000_000, null));
        System.out.println(exchange.spotDeployRegisterSpot(0, 1));

        // Route a sample order to the builder-deployed dex via builder payload.
        Map<String, Object> builder = Map.of("b", deployDex, "f", 1);
        OrderRequest request = OrderRequest.Open.limit(Tif.GTC, "ETH", true, "0.01", "1500");
        Order order = exchange.order(request, builder);
        System.out.println(order);
    }
}
