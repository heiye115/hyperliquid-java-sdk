package io.github.hyperliquid.sdk.examples.exchange.accounts;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;

/**
 * Agent and Wallet Example: demonstrates how to initialize a signing wallet
 * and perform agent-related configuration actions.
 */
public class AgentAndWalletExample {
    public static void main(String[] args) {
        // Load user private key used for signing exchange actions.
        String privateKey = System.getenv("PRIVATE_KEY");

        // Build a client on testnet for safe example execution.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .build();

        // Register the private key to activate authenticated exchange APIs.
        client.addPrivateKey(privateKey);

        // Obtain the Exchange API facade for state-changing operations.
        Exchange exchange = client.getExchange();

        // Approve an agent key and enable/assign agent abstraction mode.
        System.out.println(exchange.approveAgent("java-agent-demo"));
        System.out.println(exchange.agentEnableDexAbstraction());
        System.out.println(exchange.agentSetAbstraction("u"));
    }
}
