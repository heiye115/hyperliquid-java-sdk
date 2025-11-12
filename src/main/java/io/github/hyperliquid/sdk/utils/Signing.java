package io.github.hyperliquid.sdk.utils;

import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.OrderType;
import io.github.hyperliquid.sdk.model.order.OrderWire;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.util.*;

/**
 * 签名与转换工具（覆盖核心功能：浮点转换、订单 wire 转换、动作哈希、EIP-712 签名）。
 * <p>
 * 设计目标：
 * - 统一使用 API 层共享的 ObjectMapper，避免多处配置不一致与重复实例化；
 * - 在 addressToBytes 方法中实现严格的地址长度校验（默认 20 字节），并可通过开关启用兼容性降级策略；
 * - 为关键方法补充详细文档，包括 @return 与 @throws 注释，便于 IDE 友好提示与二次开发。
 */
public final class Signing {

    /**
     * 地址长度严格模式开关：
     * - true（默认）：addressToBytes 会严格要求地址为 20 字节（40 个十六进制字符），否则抛出
     * IllegalArgumentException；
     * - false：对非 20 字节输入执行兼容性降级（>20 截取末尾 20 字节；<20 左侧补零）。
     * <p>
     * 可通过系统属性 hyperliquid.address.strict 控制默认值，例如：
     * -Dhyperliquid.address.strict=true/false
     */
    private static volatile boolean STRICT_ADDRESS_LENGTH = Boolean
            .parseBoolean(System.getProperty("hyperliquid.address.strict", "true"));

    private Signing() {
    }

    /**
     * 将浮点数转换为字符串表示（与 Python float_to_wire 一致）。
     * 规则：
     * - 先将值格式化为 8 位小数（四舍五入规则与 Python 格式化一致）；
     * - 若格式化后与原值差异 >= 1e-12，则抛出异常（避免不可接受的舍入）；
     * - 规范化去除尾随 0 与科学计数法；
     * - "-0" 正规化为 "0"。
     *
     * @param value 浮点数值
     * @return 字符串表示
     * @throws IllegalArgumentException 当舍入误差超过阈值时抛出
     */
    public static String floatToWire(double value) {
        // 使用字符串格式化为 8 位小数，模拟 Python 的 f"{x:.8f}"
        String rounded = String.format(java.util.Locale.US, "%.8f", value);
        double roundedDouble = Double.parseDouble(rounded);
        if (Math.abs(roundedDouble - value) >= 1e-12) {
            throw new IllegalArgumentException("floatToWire causes rounding: " + value);
        }
        // 使用 BigDecimal 规范化，去除尾随 0 与科学计数法
        BigDecimal normalized = new BigDecimal(rounded).stripTrailingZeros();
        String s = normalized.toPlainString();
        if ("-0".equals(s)) {
            s = "0";
        }
        return s;
    }

    /**
     * 将浮点数转换为用于哈希的整数（与 Python float_to_int_for_hashing 一致，放大 1e8）。
     *
     * 规则：
     * - with_decimals = x * 10^8；
     * - 若 |round(with_decimals) - with_decimals| >= 1e-3，抛出异常（避免不可接受的舍入）；
     * - 返回四舍五入后的整数值。
     *
     * @param value 浮点
     * @return 放大后的整数（long）
     * @throws IllegalArgumentException 当舍入误差超过阈值时抛出
     */
    public static long floatToIntForHashing(double value) {
        return floatToInt(value, 8);
    }

    /**
     * USD 精度转换：按 1e6 放大并取整，用于 USD 类转账签名。
     *
     * @param value 金额
     * @return 放大后的整数
     */
    public static long floatToUsdInt(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        BigDecimal scaled = bd.multiply(BigDecimal.valueOf(1_000_000L));
        return scaled.longValue();
    }

    /**
     * 通用整数转换：按 10^power 放大并取整（与 Python float_to_int 一致）。
     *
     * 规则：
     * - with_decimals = x * 10^power；
     * - 若 |round(with_decimals) - with_decimals| >= 1e-3，抛出异常；
     * - 返回四舍五入后的整数值。
     *
     * @param value 浮点
     * @param power 放大倍数的指数（例如 8 表示 1e8）
     * @return 放大后的整数
     * @throws IllegalArgumentException 当舍入误差超过阈值时抛出
     */
    public static long floatToInt(double value, int power) {
        double withDecimals = value * Math.pow(10, power);
        double rounded = Math.rint(withDecimals); // 最接近偶数的舍入，与 Python round 行为接近
        if (Math.abs(rounded - withDecimals) >= 1e-3) {
            throw new IllegalArgumentException("floatToInt causes rounding: " + value);
        }
        return (long) Math.round(withDecimals);
    }

    /**
     * 获取当前毫秒时间戳。
     *
     * @return 毫秒时间戳
     */
    public static long getTimestampMs() {
        return System.currentTimeMillis();
    }

    /**
     * 将订单类型转换为 wire 结构（Map）。
     *
     * @param orderType 订单类型
     * @return Map 结构用于序列化
     */
    public static Object orderTypeToWire(OrderType orderType) {
        if (orderType == null)
            return null;
        Map<String, Object> out = new LinkedHashMap<>();
        if (orderType.getLimit() != null) {
            Map<String, Object> limitObj = new LinkedHashMap<>();
            limitObj.put("tif", orderType.getLimit().getTif().getValue());
            out.put("limit", limitObj);
        }

        if (orderType.getTrigger() != null) {
            Map<String, Object> trigObj = new LinkedHashMap<>();
            trigObj.put("triggerPx", floatToWire(orderType.getTrigger().getTriggerPx()));
            trigObj.put("isMarket", orderType.getTrigger().isMarket());
            trigObj.put("tpsl", orderType.getTrigger().getTpsl());
            out.put("trigger", trigObj);
        }

        return out.isEmpty() ? null : out;
    }

    /**
     * 将下单请求转换为 wire 结构（OrderWire），其中 coin 需为整数资产 ID，
     * sz/limitPx 转为字符串，orderType 转为 Map。
     *
     * @param coinId 整数资产 ID
     * @param req    下单请求
     * @return OrderWire
     */
    public static OrderWire orderRequestToOrderWire(int coinId, OrderRequest req) {
        String szStr = floatToWire(req.getSz());
        String pxStr = req.getLimitPx() == null ? null : floatToWire(req.getLimitPx());
        Object orderTypeWire = orderTypeToWire(req.getOrderType());
        return new OrderWire(coinId, req.getIsBuy(), szStr, pxStr, orderTypeWire, req.getReduceOnly(), req.getCloid());
    }

    /**
     * 计算 L1 动作的哈希。
     * 结构：msgpack(action) + 8字节nonce + vaultAddress标志与地址 + expiresAfter标志与值 ->
     * keccak256。
     *
     * @param action       动作对象（Map/POJO）
     * @param nonce        随机数/时间戳
     * @param vaultAddress 可选 vault 地址（0x 前缀地址 或 null）
     * @param expiresAfter 可选过期时间（毫秒）
     * @return 32字节哈希
     */
    public static byte[] actionHash(Object action, long nonce, String vaultAddress, Long expiresAfter) {
        // 完全对齐 Python 的 action_hash 序列化与拼接规则：
        // 1) 对 action 进行 MessagePack Map 编码（保持插入顺序）；
        // 2) 直接追加 nonce 的 8 字节大端原始字节；
        // 3) vaultAddress：null -> 追加单字节 0x00；非 null -> 追加 0x01 后紧接 20 字节地址；
        // 4) expiresAfter：仅当非 null 时，追加单字节 0x00 + 8 字节大端原始字节；
        try {
            byte[] actionMsgpack = packAsMsgpack(action);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(actionMsgpack.length + 64);
            // 写入 msgpack 编码的 action
            baos.write(actionMsgpack);

            // 追加 nonce 8 字节（大端）
            byte[] nonceBytes = java.nio.ByteBuffer.allocate(8).putLong(nonce).array();
            baos.write(nonceBytes);

            // 追加 vaultAddress 标记及地址
            if (vaultAddress == null) {
                baos.write(new byte[] { 0x00 });
            } else {
                baos.write(new byte[] { 0x01 });
                byte[] addrBytes = addressToBytes(vaultAddress);
                if (addrBytes.length != 20) {
                    throw new IllegalArgumentException("vaultAddress must be 20 bytes");
                }
                baos.write(addrBytes);
            }

            if (expiresAfter != null) {
                baos.write(new byte[] { 0x00 });
                byte[] expBytes = java.nio.ByteBuffer.allocate(8).putLong(expiresAfter).array();
                baos.write(expBytes);
            }

            byte[] preimage = baos.toByteArray();
            return Hash.sha3(preimage);
        } catch (Exception e) {
            throw new HypeError("Failed to compute action hash: " + e.getMessage());
        }
    }

    /**
     * 将任意 Java 对象以 Python msgpack.packb 的等价方式编码。
     * 支持 Map（建议使用 LinkedHashMap 保持插入顺序）、List、String、数字、布尔和 null。
     */
    private static byte[] packAsMsgpack(Object obj) throws java.io.IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        writeMsgpack(packer, obj);
        packer.close();
        return packer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static void writeMsgpack(MessageBufferPacker packer, Object obj) throws java.io.IOException {
        switch (obj) {
            case null -> {
                packer.packNil();
                return;
            }
            case Map<?, ?> ignored -> {
                Map<Object, Object> map = (Map<Object, Object>) obj;
                packer.packMapHeader(map.size());
                for (Map.Entry<Object, Object> e : map.entrySet()) {
                    // 键按字符串编码
                    packer.packString(String.valueOf(e.getKey()));
                    writeMsgpack(packer, e.getValue());
                }
                return;
            }
            case List<?> ignored -> {
                List<Object> list = (List<Object>) obj;
                packer.packArrayHeader(list.size());
                for (Object o : list) {
                    writeMsgpack(packer, o);
                }
                return;
            }
            case String s -> {
                packer.packString(s);
                return;
            }
            case Integer i -> {
                packer.packInt(i);
                return;
            }
            case Long l -> {
                packer.packLong(l);
                return;
            }
            case Double v -> {
                packer.packDouble(v);
                return;
            }
            case Float v -> {
                packer.packDouble(v.doubleValue());
                return;
            }
            case Boolean b -> {
                packer.packBoolean(b);
                return;
            }

            // 其它类型（如自定义 POJO）统一转 Map 或 String
            case OrderWire ow -> {
                Map<String, Object> m = new LinkedHashMap<>();
                // 键顺序严格对齐 Python：a, b, p, s, r, t, (c 最后)
                m.put("a", ow.coin);
                m.put("b", ow.isBuy);
                if (ow.limitPx != null)
                    m.put("p", ow.limitPx);
                m.put("s", ow.sz);
                m.put("r", ow.reduceOnly);
                if (ow.orderType != null)
                    m.put("t", ow.orderType);
                if (ow.cloid != null)
                    m.put("c", ow.cloid.getRaw());
                writeMsgpack(packer, m);
                return;
            }
            default -> {
            }
        }
        // 回退为字符串表示
        packer.packString(String.valueOf(obj));
    }

    /**
     * 地址字符串转字节数组（去除 0x 前缀）。
     * <p>
     * 行为说明：
     * - 严格模式（默认启用）：仅接受 20 字节地址（40 个十六进制字符），否则抛出 IllegalArgumentException；
     * - 兼容模式（可通过 setStrictAddressLength(false) 或 -Dhyperliquid.address.strict=false
     * 开启）：
     * 非 20 字节输入将执行降级处理：长度大于 20 截取末尾 20 字节；长度不足 20 则在左侧补零至 20 字节。
     *
     * @param address 以太坊地址，支持带或不带 0x 前缀
     * @return 地址的 20 字节表示
     * @throws IllegalArgumentException 当地址为空、非十六进制或在严格模式下长度不为 20 字节时抛出
     */
    public static byte[] addressToBytes(String address) {
        if (address == null) {
            throw new IllegalArgumentException("address must not be null");
        }
        String clean = Numeric.cleanHexPrefix(address);
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("address must not be empty");
        }
        final byte[] full;
        try {
            full = Numeric.hexStringToByteArray(clean);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("address contains non-hex characters: " + address, e);
        }

        if (STRICT_ADDRESS_LENGTH) {
            if (full.length != 20) {
                throw new IllegalArgumentException(
                        "address must be exactly 20 bytes (40 hex chars), got " + full.length + " bytes");
            }
            return full;
        }

        // 兼容模式：>20 截取末尾 20 字节；<20 左侧补零
        if (full.length > 20) {
            return Arrays.copyOfRange(full, full.length - 20, full.length);
        }
        if (full.length == 20) {
            return full;
        }
        byte[] out = new byte[20];
        System.arraycopy(full, 0, out, 20 - full.length, full.length);
        return out;
    }

    /**
     * 为用户签名 L1 动作（EIP-712 Typed Data）。
     *
     * @param credentials 用户凭证（私钥）
     * @return r/s/v 十六进制签名
     * @throws HypeError 当序列化或签名过程出现异常时抛出（封装底层异常信息）
     */
    public static Map<String, Object> signTypedData(Credentials credentials, String typedDataJson) {
        // 使用标准 EIP-712 结构化数据编码与签名（与 Python eth_account.encode_typed_data 一致）。
        try {
            org.web3j.crypto.StructuredDataEncoder encoder = new org.web3j.crypto.StructuredDataEncoder(typedDataJson);
            byte[] digest = encoder.hashStructuredData();
            // 纯 EIP-712 非前缀实现：直接对 EIP-712 结构化数据的 digest 进行签名，不再做任何额外哈希或前缀。
            // 说明：Sign.signMessage(byte[], ECKeyPair, false) 会跳过内置哈希步骤，直接对输入进行 ECDSA 签名。
            Sign.SignatureData sig = Sign.signMessage(digest, credentials.getEcKeyPair(), false);
            String r = Numeric.toHexString(sig.getR());
            String s = Numeric.toHexString(sig.getS());
            int vInt = new java.math.BigInteger(1, sig.getV()).intValue();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("r", r);
            out.put("s", s);
            out.put("v", vInt);
            return out;
        } catch (Exception e) {
            throw new HypeError("Failed to sign typed data: " + e.getMessage());
        }
    }

    /**
     * 构造 Phantom Agent（与 Python construct_phantom_agent 一致）。
     */
    public static Map<String, Object> constructPhantomAgent(byte[] hash, boolean isMainnet) {
        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("source", isMainnet ? "a" : "b");
        // bytes32 以 0x 前缀十六进制字符串表达，兼容 web3j StructuredDataEncoder
        agent.put("connectionId", Numeric.toHexString(hash));
        return agent;
    }

    /**
     * 生成 EIP-712 TypedData JSON（与 Python l1_payload 一致）。
     */
    public static String l1PayloadJson(Map<String, Object> phantomAgent) {
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("chainId", 1337);
        domain.put("name", "Exchange");
        domain.put("verifyingContract", "0x0000000000000000000000000000000000000000");
        domain.put("version", "1");

        List<Map<String, Object>> agentTypes = new ArrayList<>();
        agentTypes.add(Map.of("name", "source", "type", "string"));
        agentTypes.add(Map.of("name", "connectionId", "type", "bytes32"));

        List<Map<String, Object>> eipTypes = new ArrayList<>();
        eipTypes.add(Map.of("name", "name", "type", "string"));
        eipTypes.add(Map.of("name", "version", "type", "string"));
        eipTypes.add(Map.of("name", "chainId", "type", "uint256"));
        eipTypes.add(Map.of("name", "verifyingContract", "type", "address"));

        Map<String, Object> types = new LinkedHashMap<>();
        types.put("Agent", agentTypes);
        types.put("EIP712Domain", eipTypes);

        Map<String, Object> full = new LinkedHashMap<>();
        full.put("domain", domain);
        full.put("types", types);
        full.put("primaryType", "Agent");
        full.put("message", phantomAgent);

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            return om.writeValueAsString(full);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new HypeError("Failed to build typed data json: " + e.getMessage());
        }
    }

    /**
     * 对 L1 动作进行签名（完整流程）：计算 actionHash -> 构造 phantom agent -> 生成 EIP-712 typed data
     * -> 签名。
     */
    public static Map<String, Object> signL1Action(Credentials credentials, Object action, String vaultAddress,
            long nonce, Long expiresAfter, boolean isMainnet) {
        byte[] hash = actionHash(action, nonce, vaultAddress, expiresAfter);
        Map<String, Object> agent = constructPhantomAgent(hash, isMainnet);
        String typedJson = l1PayloadJson(agent);
        return signTypedData(credentials, typedJson);
    }

    /**
     * 构造用户签名动作的 EIP-712 TypedData JSON（与 Python user_signed_payload 一致）。
     * 说明：
     * - primaryType 为具体事务类型，例如 "HyperliquidTransaction:UsdSend"。
     * - payloadTypes 为该事务类型的字段类型列表，例如 USD_SEND_SIGN_TYPES。
     * - action 为消息体，其中必须包含 "signatureChainId"（16 进制字符串，如 0x66eee）与
     * "hyperliquidChain"（"Mainnet" 或 "Testnet"）。
     *
     * @param primaryType  主类型名称
     * @param payloadTypes 字段类型定义列表
     * @param action       动作消息（必须包含 signatureChainId 与 hyperliquidChain）
     * @return EIP-712 TypedData 的 JSON 字符串
     */
    public static String userSignedPayloadJson(String primaryType, List<Map<String, Object>> payloadTypes,
            Map<String, Object> action) {
        // 将 signatureChainId 的 16 进制字符串解析为整型链 ID
        Object sigChainIdObj = action.get("signatureChainId");
        if (sigChainIdObj == null) {
            throw new HypeError("signatureChainId missing in user-signed action");
        }
        String sigChainIdHex = String.valueOf(sigChainIdObj);
        int chainId;
        try {
            chainId = new java.math.BigInteger(sigChainIdHex.replace("0x", ""), 16).intValue();
        } catch (Exception e) {
            throw new HypeError("Invalid signatureChainId: " + sigChainIdHex);
        }

        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("name", "HyperliquidSignTransaction");
        domain.put("version", "1");
        domain.put("chainId", chainId);
        domain.put("verifyingContract", "0x0000000000000000000000000000000000000000");

        List<Map<String, Object>> eipTypes = new ArrayList<>();
        eipTypes.add(Map.of("name", "name", "type", "string"));
        eipTypes.add(Map.of("name", "version", "type", "string"));
        eipTypes.add(Map.of("name", "chainId", "type", "uint256"));
        eipTypes.add(Map.of("name", "verifyingContract", "type", "address"));

        Map<String, Object> types = new LinkedHashMap<>();
        types.put(primaryType, payloadTypes);
        types.put("EIP712Domain", eipTypes);

        Map<String, Object> full = new LinkedHashMap<>();
        full.put("domain", domain);
        full.put("types", types);
        full.put("primaryType", primaryType);
        full.put("message", action);

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            return om.writeValueAsString(full);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new HypeError("Failed to build user-signed typed data json: " + e.getMessage());
        }
    }

    /**
     * 对用户签名动作进行签名（与 Python sign_user_signed_action 一致）。
     * 规则：
     * - 自动设置 signatureChainId（0x66eee）与 hyperliquidChain（根据 isMainnet）。
     * - 使用 userSignedPayloadJson 构造 EIP-712 TypedData 并进行签名。
     *
     * @param credentials  用户凭证
     * @param action       动作消息（会补全签名相关字段）
     * @param payloadTypes 字段类型定义列表
     * @param primaryType  主类型名称
     * @param isMainnet    是否主网
     * @return r/s/v 签名
     */
    public static Map<String, Object> signUserSignedAction(Credentials credentials,
            Map<String, Object> action,
            List<Map<String, Object>> payloadTypes,
            String primaryType,
            boolean isMainnet) {
        action.put("signatureChainId", "0x66eee");
        action.put("hyperliquidChain", isMainnet ? "Mainnet" : "Testnet");
        String typedJson = userSignedPayloadJson(primaryType, payloadTypes, action);
        return signTypedData(credentials, typedJson);
    }

    /**
     * 通用：从 EIP-712 TypedData JSON 与 r/s/v 签名恢复地址。
     *
     * @param typedDataJson EIP-712 TypedData JSON 字符串
     * @param signature     r/s/v 签名
     * @return 0x 地址（小写）
     */
    public static String recoverFromTypedData(String typedDataJson, Map<String, Object> signature) {
        try {
            org.web3j.crypto.StructuredDataEncoder encoder = new org.web3j.crypto.StructuredDataEncoder(typedDataJson);
            byte[] digest = encoder.hashStructuredData();
            String rHex = String.valueOf(signature.get("r"));
            String sHex = String.valueOf(signature.get("s"));
            int vInt = Integer.parseInt(String.valueOf(signature.get("v")));
            byte[] r = Numeric.hexStringToByteArray(rHex);
            byte[] s = Numeric.hexStringToByteArray(sHex);
            byte vByte = (byte) vInt;
            // 纯 EIP-712 非前缀恢复：直接基于 digest 与 r/s/v 进行 ecrecover，避免任何额外哈希或前缀。
            // 注意：web3j 的 signedMessageToKey 可能会对输入进行哈希或与前缀约定耦合，这里使用更底层的
            // recoverFromSignature。
            int recId;
            if (vInt == 27 || vInt == 28) {
                recId = vInt - 27;
            } else if (vInt == 0 || vInt == 1) {
                recId = vInt;
            } else if (vInt >= 35) {
                // 兼容 EIP-155 风格 v 值（尽管 EIP-712 通常不携带 chainId），仅用于健壮性处理
                recId = (vInt - 35) % 2;
            } else {
                throw new HypeError("Unsupported v value for recovery: " + vInt);
            }
            java.math.BigInteger rBI = new java.math.BigInteger(1, r);
            java.math.BigInteger sBI = new java.math.BigInteger(1, s);
            java.math.BigInteger pubKey = recoverPublicKeyFromSignature(recId, rBI, sBI, digest);
            String addr = org.web3j.crypto.Keys.getAddress(pubKey);
            return "0x" + addr.toLowerCase();
        } catch (Exception e) {
            throw new HypeError("Failed to recover address: " + e.getMessage());
        }
    }

    /**
     * 公共辅助方法：从 digest 与 r/s/v 恢复地址（纯 EIP-712 非前缀）。
     * 可用于测试与诊断，无需构造完整 typedData JSON。
     *
     * @param digest    32 字节 EIP-712 哈希
     * @param signature r/s/v 签名
     * @return 恢复出的地址（0x 小写）
     */
    public static String recoverAddressFromDigest(byte[] digest, Map<String, Object> signature) {
        String rHex = String.valueOf(signature.get("r"));
        String sHex = String.valueOf(signature.get("s"));
        int vInt = Integer.parseInt(String.valueOf(signature.get("v")));
        byte[] r = Numeric.hexStringToByteArray(rHex);
        byte[] s = Numeric.hexStringToByteArray(sHex);
        int recId;
        if (vInt == 27 || vInt == 28) {
            recId = vInt - 27;
        } else if (vInt == 0 || vInt == 1) {
            recId = vInt;
        } else if (vInt >= 35) {
            recId = (vInt - 35) % 2;
        } else {
            throw new HypeError("Unsupported v value for recovery: " + vInt);
        }
        java.math.BigInteger rBI = new java.math.BigInteger(1, r);
        java.math.BigInteger sBI = new java.math.BigInteger(1, s);
        java.math.BigInteger pubKey = recoverPublicKeyFromSignature(recId, rBI, sBI, digest);
        String addr = org.web3j.crypto.Keys.getAddress(pubKey);
        return "0x" + addr.toLowerCase();
    }

    /**
     * 使用 BouncyCastle 实现的 ecrecover：根据 r/s/v 与消息摘要（digest）恢复未压缩公钥（64字节）。
     * 该实现遵循 secp256k1 曲线参数，避免 web3j 高层 API 对消息哈希或前缀的约束。
     *
     * @param recId  0 或 1（由 v 归一化得到）
     * @param r      ECDSA r 值
     * @param s      ECDSA s 值
     * @param digest 32 字节消息摘要（EIP-712 哈希）
     * @return 未压缩公钥的 BigInteger 表示（64 字节 x||y）
     */
    private static java.math.BigInteger recoverPublicKeyFromSignature(int recId, java.math.BigInteger r,
            java.math.BigInteger s, byte[] digest) {
        // 使用 BouncyCastle 曲线参数
        org.bouncycastle.asn1.x9.X9ECParameters x9 = org.bouncycastle.crypto.ec.CustomNamedCurves
                .getByName("secp256k1");
        org.bouncycastle.crypto.params.ECDomainParameters curve = new org.bouncycastle.crypto.params.ECDomainParameters(
                x9.getCurve(), x9.getG(), x9.getN(), x9.getH());

        java.math.BigInteger n = curve.getN();
        java.math.BigInteger i = java.math.BigInteger.valueOf(recId / 2);
        java.math.BigInteger x = r.add(i.multiply(n));

        // 根据 recId 的奇偶确定压缩点的奇偶性
        boolean yBit = (recId % 2) == 1;
        org.bouncycastle.math.ec.ECPoint R = decompressKey(x, yBit, curve.getCurve());
        if (R == null || !R.multiply(n).isInfinity()) {
            throw new HypeError("Invalid R point during public key recovery");
        }

        java.math.BigInteger e = new java.math.BigInteger(1, digest);
        java.math.BigInteger rInv = r.modInverse(n);
        java.math.BigInteger srInv = s.multiply(rInv).mod(n);
        java.math.BigInteger eInv = e.negate().mod(n);
        java.math.BigInteger eInvRInv = eInv.multiply(rInv).mod(n);

        org.bouncycastle.math.ec.ECPoint q = org.bouncycastle.math.ec.ECAlgorithms.sumOfTwoMultiplies(
                curve.getG(), eInvRInv, R, srInv);
        byte[] pubKeyEncoded = q.getEncoded(false); // 65 字节，首字节为 0x04
        byte[] pubKeyNoPrefix = java.util.Arrays.copyOfRange(pubKeyEncoded, 1, pubKeyEncoded.length);
        return new java.math.BigInteger(1, pubKeyNoPrefix);
    }

    /**
     * 将给定 x 坐标与 y 奇偶标志，解压为曲线上点（未压缩）。
     */
    private static org.bouncycastle.math.ec.ECPoint decompressKey(java.math.BigInteger xBN, boolean yBit,
            org.bouncycastle.math.ec.ECCurve curve) {
        byte[] compEnc = new byte[33];
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        byte[] xBytes = xBN.toByteArray();
        int start = Math.max(0, xBytes.length - 32);
        int length = Math.min(32, xBytes.length);
        System.arraycopy(xBytes, start, compEnc, 33 - length, length);
        return curve.decodePoint(compEnc);
    }

    /**
     * 从 L1 动作签名中恢复签名者地址（与 Python recover_agent_or_user_from_l1_action 一致）。
     *
     * @param action       L1 动作（Map 或 List）
     * @param vaultAddress 有效的 vault 地址（可为空）
     * @param nonce        毫秒时间戳
     * @param expiresAfter 过期毫秒偏移（可空）
     * @param isMainnet    是否主网
     * @param signature    r/s/v 签名
     * @return 恢复得到的 0x 地址（小写）
     */
    public static String recoverAgentOrUserFromL1Action(Object action, String vaultAddress,
            long nonce, Long expiresAfter,
            boolean isMainnet,
            Map<String, Object> signature) {
        byte[] hash = actionHash(action, nonce, vaultAddress, expiresAfter);
        Map<String, Object> agent = constructPhantomAgent(hash, isMainnet);
        String typedJson = l1PayloadJson(agent);
        return recoverFromTypedData(typedJson, signature);
    }

    /**
     * 从用户签名动作中恢复用户地址（与 Python recover_user_from_user_signed_action 一致）。
     * 注意：不会改动 action 的 signatureChainId，仅会根据 isMainnet 设置 hyperliquidChain。
     *
     * @param action       动作消息（须包含 signatureChainId）
     * @param signature    r/s/v 签名
     * @param payloadTypes 字段类型定义列表
     * @param primaryType  主类型名称
     * @param isMainnet    是否主网
     * @return 恢复得到的 0x 地址（小写）
     */
    public static String recoverUserFromUserSignedAction(Map<String, Object> action,
            Map<String, Object> signature,
            List<Map<String, Object>> payloadTypes,
            String primaryType,
            boolean isMainnet) {
        action.put("hyperliquidChain", isMainnet ? "Mainnet" : "Testnet");
        String typedJson = userSignedPayloadJson(primaryType, payloadTypes, action);
        return recoverFromTypedData(typedJson, signature);
    }

    /**
     * 将多个 OrderWire 转换为 L1 动作对象，用于签名与发送。
     * <p>
     * 生成的结构形如：
     * {
     * "type": "order",
     * "orders": [
     * {"coin": 1, "isBuy": true, "sz": "0.1", "limitPx": "60000", "orderType":
     * {...}, "reduceOnly": false, "cloid": "..."},
     * ...
     * ]
     * }
     *
     * @param orders 订单 wire 列表
     * @return Map 动作对象 {"type": "order", "orders": [...]}
     */
    public static Map<String, Object> orderWiresToOrderAction(List<OrderWire> orders) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "order");
        List<Map<String, Object>> wires = new ArrayList<>();
        for (OrderWire o : orders) {
            Map<String, Object> w = new LinkedHashMap<>();
            // 键顺序严格对齐 Python：a, b, p, s, r, t, (c 最后)
            w.put("a", o.coin);
            w.put("b", o.isBuy);
            if (o.limitPx != null) {
                w.put("p", o.limitPx);
            }
            w.put("s", o.sz);
            w.put("r", o.reduceOnly);
            if (o.orderType != null) {
                w.put("t", o.orderType);
            }
            if (o.cloid != null) {
                w.put("c", o.cloid.getRaw());
            }
            wires.add(w);
        }
        action.put("orders", wires);
        // 与 Python 一致，默认分组为 "na"（置于 orders 之后）
        action.put("grouping", "na");
        return action;
    }

    /**
     * 设置地址长度校验严格模式。
     *
     * @param strict 是否严格要求地址为 20 字节
     */
    public static void setStrictAddressLength(boolean strict) {
        STRICT_ADDRESS_LENGTH = strict;
    }

    /**
     * 获取当前地址长度校验模式。
     *
     * @return true 表示严格模式；false 表示兼容模式
     */
    public static boolean isStrictAddressLength() {
        return STRICT_ADDRESS_LENGTH;
    }
}
