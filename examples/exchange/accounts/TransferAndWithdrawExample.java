package io.github.hyperliquid.sdk.examples.exchange.accounts;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;

/**
 * Transfer and Withdraw Example: demonstrates common balance movement actions
 * such as USD transfer, spot transfer, cross-class transfer, and bridge
 * withdrawal.
 */
public class TransferAndWithdrawExample {
    public static void main(String[] args) {
        // Load sender credentials and destination account from environment.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        String destination = System.getenv("DESTINATION_ADDRESS");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null || destination == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS, API_WALLET_PRIVATE_KEY, DESTINATION_ADDRESS");
        }

        // Initialize authenticated Exchange client on testnet.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        // Execute transfer and withdrawal scenarios in sequence.
        Exchange exchange = client.getExchange();
        System.out.println(exchange.usdTransfer("1", destination));
        System.out.println(exchange.spotTransfer("0.1", destination, "USDC"));
        System.out.println(exchange.sendAsset(destination, "", "", "USDC", "1", null));
        System.out.println(exchange.usdClassTransfer(true, "1"));
        System.out.println(exchange.withdrawFromBridge("1", destination));
    }
}
