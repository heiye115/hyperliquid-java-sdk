package io.github.hyperliquid.sdk.examples.exchange.accounts;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.userabstraction.UserAbstractionMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User Abstraction and Multi-Sig Example: demonstrates toggling abstraction
 * modes and submitting a minimal multi-signature action payload.
 */
public class UserAbstractionAndMultiSigExample {
    public static void main(String[] args) {
        // Load signer wallet and multi-sig user identifier.
        String primaryWalletAddress = System.getenv("PRIMARY_WALLET_ADDRESS");
        String apiWalletPrivateKey = System.getenv("API_WALLET_PRIVATE_KEY");
        String multiSigUser = System.getenv("MULTI_SIG_USER");
        if (primaryWalletAddress == null || apiWalletPrivateKey == null || multiSigUser == null) {
            throw new IllegalStateException("Set PRIMARY_WALLET_ADDRESS, API_WALLET_PRIVATE_KEY, MULTI_SIG_USER");
        }

        // Build authenticated testnet client.
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        // Get service facades and current account address.
        Exchange exchange = client.getExchange();
        Info info = client.getInfo();
        String user = client.getSingleAddress();

        // Enable dex abstraction and set the preferred abstraction mode.
        System.out.println(exchange.userDexAbstraction(user, true));
        System.out.println(exchange.userSetAbstraction(user, UserAbstractionMode.UNIFIED_ACCOUNT, null));
        System.out.println(info.userAbstraction(user));

        // Build a minimal inner action and send a multi-sig envelope.
        Map<String, Object> innerAction = new LinkedHashMap<>();
        innerAction.put("type", "noop");
        List<Map<String, Object>> signatures = new ArrayList<>();
        long nonce = System.currentTimeMillis();
        System.out.println(exchange.multiSig(multiSigUser, innerAction, signatures, nonce));
    }
}
