package io.github.hyperliquid.sdk.examples.exchange.orders;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.*;

/**
 * Order With Cloid Example: demonstrates placing and modifying an order by
 * client order identifier for idempotent order tracking.
 */
public class OrderWithCloidExample {
    public static void main(String[] args) {
        // Load API wallet credentials for authenticated trading calls.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS and API_WALLET_PRIVATE_KEY");
        }

        // Build testnet client and access Exchange API.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        Exchange exchange = client.getExchange();

        // Generate a cloid, then place a limit order bound to that cloid.
        Cloid cloid = Cloid.auto();
        OrderRequest request = OrderRequest.Open.limit(Tif.GTC, "ETH", true, "0.01", "1500", cloid);
        Order order = exchange.order(request);
        System.out.println(order);

        // Modify the same order by referencing the same cloid.
        ModifyOrderRequest modify = new ModifyOrderRequest();
        modify.setCoin("ETH");
        modify.setCloid(cloid);
        modify.setBuy(true);
        modify.setSz("0.01");
        modify.setLimitPx("1490");
        ModifyOrder modified = exchange.modifyOrder(modify);
        System.out.println(modified);
    }
}
