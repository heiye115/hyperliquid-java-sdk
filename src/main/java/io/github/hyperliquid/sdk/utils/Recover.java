package io.github.hyperliquid.sdk.utils;

import java.util.List;
import java.util.Map;

/**
 * User/Agent recovery utility class: provides tool methods to recover addresses from signed EIP-712 TypedData.
 *
 * <p>Background and purpose:
 * - When the backend receives /exchange and other requests, it verifies signatures and recovers the signer address from typed data;
 * - This utility class is for client-side "offline recovery/verification" to help developers confirm the signer before submission;
 * - Fully aligned with Python SDK's recover_agent_or_user_from_l1_action / recover_user_from_user_signed_action.
 *
 * <p>Typical use cases:
 * - Quick debugging: given r/s/v and action body, recover address to locate the signer;
 * - Programmatic initialization: recover and print address locally first, then complete initialization via frontend login or submitting valid action.
 */
public final class Recover {

    private Recover() {
    }

    /**
     * Recover signer address from L1 action signature.
     *
     * <p>Consistent with Python recover_agent_or_user_from_l1_action:
     * Calculate actionHash -> construct phantom agent -> generate EIP-712 typed data -> recover address using r/s/v.
     *
     * @param action       L1 action (Map or List), e.g., cancel, order, modify order, etc.
     * @param vaultAddress Vault address (required for some actions, not needed for others like usdClassTransfer/sendAsset)
     * @param nonce        timestamp in milliseconds (must match value used during signing)
     * @param expiresAfter expiration offset in milliseconds (can be null)
     * @param isMainnet    whether mainnet (affects phantom agent's source field)
     * @param signature    r/s/v signature, format: {"r":"0x...","s":"0x...","v":27}
     * @return recovered 0x address (lowercase), throws HypeError if failure
     *
     * <p>Example:
     * <pre>
     * Map<String, Object> action = Map.of(
     *     "type", "cancel",
     *     "cancels", List.of(Map.of("a", 87, "o", 28800768235L))
     * );
     * Map<String, Object> signature = Map.of(
     *     "r", "0xd088ce...",
     *     "s", "0x425d84...",
     *     "v", 27
     * );
     * String addr = Recover.recoverAgentOrUserFromL1Action(action, "0xc64c...918d8", 1745532560074L, null, false, signature);
     * System.out.println("Recovered: " + addr);
     * </pre>
     */
    public static String recoverAgentOrUserFromL1Action(Object action, String vaultAddress,
                                                        long nonce, Long expiresAfter,
                                                        boolean isMainnet,
                                                        Map<String, Object> signature) {
        return Signing.recoverAgentOrUserFromL1Action(action, vaultAddress, nonce, expiresAfter, isMainnet, signature);
    }

    /**
     * Recover user address from user-signed action.
     *
     * <p>Consistent with Python recover_user_from_user_signed_action:
     * Construct EIP-712 typed data based on primaryType and payloadTypes;
     * Set action.hyperliquidChain to Mainnet/Testnet (without modifying signatureChainId);
     * Recover address using r/s/v.
     *
     * @param action       action message (must contain signatureChainId, hex string, e.g., 0xa4b1)
     * @param signature    r/s/v signature, format: {"r":"0x...","s":"0x...","v":27}
     * @param payloadTypes field type definition list, e.g., TokenDelegate:
     *                     [{name:"hyperliquidChain",type:"string"},
     *                      {name:"validator",type:"address"},
     *                      {name:"wei",type:"uint64"},
     *                      {name:"isUndelegate",type:"bool"},
     *                      {name:"nonce",type:"uint64"}]
     * @param primaryType  main type name, e.g., "HyperliquidTransaction:TokenDelegate"
     * @param isMainnet    whether mainnet
     * @return recovered 0x address (lowercase), throws HypeError if failure
     *
     * <p>Example:
     * <pre>
     * Map<String, Object> action = new java.util.LinkedHashMap<>();
     * action.put("type", "tokenDelegate");
     * action.put("signatureChainId", "0xa4b1");
     * action.put("hyperliquidChain", "Mainnet");
     * action.put("validator", "0x5ac99df645f3414876c816caa18b2d234024b487");
     * action.put("wei", 100163871320L);
     * action.put("isUndelegate", true);
     * action.put("nonce", 1744932112279L);
     * List<Map<String, Object>> types = java.util.List.of(
     *     java.util.Map.of("name", "hyperliquidChain", "type", "string"),
     *     java.util.Map.of("name", "validator", "type", "address"),
     *     java.util.Map.of("name", "wei", "type", "uint64"),
     *     java.util.Map.of("name", "isUndelegate", "type", "bool"),
     *     java.util.Map.of("name", "nonce", "type", "uint64")
     * );
     * Map<String, Object> signature = java.util.Map.of(
     *     "r", "0xa00406...",
     *     "s", "0x34cf47...",
     *     "v", 27
     * );
     * String addr = Recover.recoverUserFromUserSignedAction(action, signature, types,
     *         "HyperliquidTransaction:TokenDelegate", true);
     * </pre>
     */
    public static String recoverUserFromUserSignedAction(Map<String, Object> action,
                                                         Map<String, Object> signature,
                                                         List<Map<String, Object>> payloadTypes,
                                                         String primaryType,
                                                         boolean isMainnet) {
        return Signing.recoverUserFromUserSignedAction(action, signature, payloadTypes, primaryType, isMainnet);
    }
}

