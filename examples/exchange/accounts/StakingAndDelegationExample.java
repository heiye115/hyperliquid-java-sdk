package io.github.hyperliquid.sdk.examples.exchange.accounts;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;

/**
 * Staking and Delegation Example: demonstrates delegation and undelegation
 * workflows for validator staking operations.
 */
public class StakingAndDelegationExample {
    public static void main(String[] args) {
        // Load signer credentials and validator target.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        String validator = System.getenv("VALIDATOR_ADDRESS");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null || validator == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS, API_WALLET_PRIVATE_KEY, VALIDATOR_ADDRESS");
        }

        // Build authenticated testnet client.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        // Delegate and then undelegate a sample amount.
        Exchange exchange = client.getExchange();
        System.out.println(exchange.tokenDelegate(validator, 1_000_000L, false));
        System.out.println(exchange.tokenDelegate(validator, 1_000_000L, true));
    }
}
