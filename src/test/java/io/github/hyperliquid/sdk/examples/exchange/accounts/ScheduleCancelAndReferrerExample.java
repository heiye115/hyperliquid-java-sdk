package io.github.hyperliquid.sdk.examples.exchange.accounts;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;

/**
 * Schedule Cancel and Referrer Example: demonstrates risk controls and referral
 * configuration calls in the Exchange API.
 */
public class ScheduleCancelAndReferrerExample {
    public static void main(String[] args) {
        // Load API wallet credentials from environment variables.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS and API_WALLET_PRIVATE_KEY");
        }

        // Initialize testnet client with API wallet authentication.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        // Use Exchange API for signed account-level operations.
        Exchange exchange = client.getExchange();

        // Schedule a global cancel guard one minute in the future.
        long triggerTime = System.currentTimeMillis() + 60_000L;
        System.out.println(exchange.scheduleCancel(triggerTime));

        // Bind a referral code and approve builder fee policy.
        System.out.println(exchange.setReferrer("JAVA_DEMO_REF"));
        System.out.println(exchange.approveBuilderFee("0x0000000000000000000000000000000000000000", "0.0001"));
    }
}
