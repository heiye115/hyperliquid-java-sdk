package io.github.hyperliquid.sdk.examples.exchange.accounts;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;

/**
 * Sub-Account and Vault Example: demonstrates account hierarchy operations,
 * including sub-account creation, balance transfer, and vault transfer.
 */
public class SubAccountAndVaultExample {
    public static void main(String[] args) {
        // Load account and target routing parameters from environment.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        String subAccountUser = System.getenv("SUB_ACCOUNT_USER");
        String vaultAddress = System.getenv("VAULT_ADDRESS");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null || subAccountUser == null || vaultAddress == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS, API_WALLET_PRIVATE_KEY, SUB_ACCOUNT_USER, VAULT_ADDRESS");
        }

        // Initialize authenticated client on testnet.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        // Execute sub-account and vault related operations.
        Exchange exchange = client.getExchange();
        System.out.println(exchange.createSubAccount("java-sub-account"));
        System.out.println(exchange.subAccountTransfer(subAccountUser, true, 1L));
        System.out.println(exchange.subAccountSpotTransfer(subAccountUser, true, "USDC", "1"));
        System.out.println(exchange.vaultTransfer(vaultAddress, true, 1L));
    }
}
