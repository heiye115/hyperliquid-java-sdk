package io.github.hyperliquid.sdk.utils;

import java.util.List;
import java.util.Map;

/**
 * 用户/Agent 恢复功能类：提供从签名的 EIP-712 TypedData 中恢复地址的工具方法。
 *
 * <p>背景与用途：
 * - 后端在接收 /exchange 等请求时，会校验签名并从 typed data 中恢复签名者地址；
 * - 本工具类面向客户端侧的“离线恢复/验证”，帮助开发者在提交前确认签名者是谁；
 * - 与 Python SDK 的 recover_agent_or_user_from_l1_action / recover_user_from_user_signed_action 完全对齐。
 *
 * <p>典型使用场景：
 * - 快速调试：给定 r/s/v 与动作体，恢复地址以定位签名者；
 * - 程序化初始化：先在本地恢复并打印地址，然后通过前端登录或提交有效动作完成初始化。
 */
public final class Recover {

    private Recover() {}

    /**
     * 从 L1 动作签名中恢复签名者地址。
     *
     * <p>与 Python recover_agent_or_user_from_l1_action 一致：
     * 计算 actionHash -> 构造 phantom agent -> 生成 EIP-712 typed data -> 使用 r/s/v 恢复地址。
     *
     * @param action       L1 动作（Map 或 List），例如撤单、下单、修改订单等
     * @param vaultAddress Vault 地址（部分动作需要，某些如 usdClassTransfer/sendAsset 不需要）
     * @param nonce        毫秒时间戳（与签名时使用的值一致）
     * @param expiresAfter 过期毫秒偏移（可空）
     * @param isMainnet    是否主网（影响 phantom agent 的 source 字段）
     * @param signature    r/s/v 签名，格式：{"r":"0x...","s":"0x...","v":27}
     * @return 恢复得到的 0x 地址（小写），若失败将抛出 HypeError
     *
     * <p>示例：
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
     * 从用户签名动作中恢复用户地址。
     *
     * <p>与 Python recover_user_from_user_signed_action 一致：
     * 根据 primaryType 与 payloadTypes 构造 EIP-712 typed data；
     * 将 action.hyperliquidChain 设置为 Mainnet/Testnet（不改动 signatureChainId）；
     * 使用 r/s/v 恢复地址。
     *
     * @param action       动作消息（须包含 signatureChainId，hex 字符串，例如 0xa4b1）
     * @param signature    r/s/v 签名，格式：{"r":"0x...","s":"0x...","v":27}
     * @param payloadTypes 字段类型定义列表，例如 TokenDelegate：
     *                     [{name:"hyperliquidChain",type:"string"},
     *                      {name:"validator",type:"address"},
     *                      {name:"wei",type:"uint64"},
     *                      {name:"isUndelegate",type:"bool"},
     *                      {name:"nonce",type:"uint64"}]
     * @param primaryType  主类型名称，例如 "HyperliquidTransaction:TokenDelegate"
     * @param isMainnet    是否主网
     * @return 恢复得到的 0x 地址（小写），若失败将抛出 HypeError
     *
     * <p>示例：
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

