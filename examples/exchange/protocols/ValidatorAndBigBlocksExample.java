package io.github.hyperliquid.sdk.examples.exchange.protocols;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;

/**
 * Validator and BigBlocks Example: demonstrates advanced validator/signer
 * operations and EVM big-block preference configuration.
 */
public class ValidatorAndBigBlocksExample {
    public static void main(String[] args) {
        // Load validator-related runtime inputs from environment.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        String signerAddress = System.getenv("SIGNER_ADDRESS");
        String nodeIp = System.getenv("VALIDATOR_NODE_IP");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null || signerAddress == null || nodeIp == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS, API_WALLET_PRIVATE_KEY, SIGNER_ADDRESS, VALIDATOR_NODE_IP");
        }

        // Build authenticated testnet client.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        // Execute advanced feature calls: big blocks, signer state, validator registration.
        Exchange exchange = client.getExchange();
        System.out.println(exchange.useBigBlocks(true));
        System.out.println(exchange.cSignerJailSelf());
        System.out.println(exchange.cSignerUnjailSelf());
        System.out.println(exchange.cValidatorRegister(
                nodeIp,
                "java-validator",
                "java example validator",
                false,
                100,
                signerAddress,
                false,
                1_000_000L));
    }
}
