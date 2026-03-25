package io.github.hyperliquid.sdk.examples.exchange.accounts;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.OpenOrder;
import io.github.hyperliquid.sdk.model.order.CancelRequest;

import java.util.List;

/**
 * Cancel Open Orders Example: demonstrates how to query active orders and
 * submit a batch cancel request.
 */
public class CancelOpenOrdersExample {
    public static void main(String[] args) {
        // Load API wallet credentials for signed cancel operations.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS and API_WALLET_PRIVATE_KEY");
        }

        // Build authenticated client on testnet.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        // Read open orders and prepare cancel payloads.
        Info info = client.getInfo();
        Exchange exchange = client.getExchange();
        String user = client.getSingleAddress();

        List<OpenOrder> openOrders = info.openOrders(user);
        List<CancelRequest> cancelRequests = openOrders.stream()
                .map(order -> CancelRequest.of(order.getCoin(), order.getOid()))
                .toList();
        if (cancelRequests.isEmpty()) {
            System.out.println("No open orders");
            return;
        }

        // Execute batch cancellation for all collected open orders.
        System.out.println(exchange.cancels(cancelRequests));
    }
}
