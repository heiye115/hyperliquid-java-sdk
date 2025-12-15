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
 * Signature and conversion utilities (covering core functions: floating-point conversion, order wire conversion, action hashing, EIP-712 signing).
 * <p>
 * Design objectives:
 * - Implement strict address length validation in addressToBytes method (default 20 bytes), with option to enable compatibility downgrade strategy;
 * - Supplement key methods with detailed documentation, including @return and @throws annotations for IDE-friendly hints and secondary development.
 * <p>
 * Order placement / cancellation / all Exchange trading behaviors: must use sign_l1_action (i.e., L1 action signing process — msgpack(action) → actionHash → construct phantom agent → sign phantom agent with EIP-712).
 * <p>
 * Fund/management operations (USD/USDT transfers, approve agent, withdraw/deposit, etc., requiring explicit user asset authorization): must use sign_user_signed_action (i.e., directly sign action with EIP-712, chainId = 0x66eee, no phantom agent).
 */
public final class Signing {

    /**
     * Address length strict mode switch:
     * - true (default): addressToBytes will strictly require address to be 20 bytes (40 hexadecimal characters), otherwise throw
     * IllegalArgumentException;
     * - false: Perform compatibility downgrade for non-20-byte input (>20 truncate to last 20 bytes; <20 pad with zeros on the left).
     * <p>
     * Can be controlled via system property hyperliquid.address.strict to set default value
     */
    private static volatile boolean STRICT_ADDRESS_LENGTH = Boolean.TRUE;

    private Signing() {
    }

    /**
     * Convert floating-point number to string representation (consistent with Python float_to_wire).
     * Rules:
     * - First format the value to 8 decimal places (rounding rules consistent with Python formatting);
     * - If the difference between formatted and original value >= 1e-12, throw an exception (avoid unacceptable rounding);
     * - Normalize by removing trailing zeros and scientific notation;
     * - Normalize "-0" to "0".
     *
     * @param value Floating-point value
     * @return String representation
     * @throws IllegalArgumentException Thrown when rounding error exceeds threshold
     */
    public static String floatToWire(double value) {
        // Format string to 8 decimal places, simulating Python's f"{x:.8f}"
        String rounded = String.format(java.util.Locale.US, "%.8f", value);
        double roundedDouble = Double.parseDouble(rounded);
        if (Math.abs(roundedDouble - value) >= 1e-12) {
            throw new IllegalArgumentException("floatToWire causes rounding: " + value);
        }
        // Normalize using BigDecimal, removing trailing zeros and scientific notation
        BigDecimal normalized = new BigDecimal(rounded).stripTrailingZeros();
        String s = normalized.toPlainString();
        if ("-0".equals(s)) {
            s = "0";
        }
        return s;
    }

    /**
     * Convert floating-point number to integer for hashing (consistent with Python float_to_int_for_hashing, magnified by 1e8).
     * <p>
     * Rules:
     * - with_decimals = x * 10^8;
     * - If |round(with_decimals) - with_decimals| >= 1e-3, throw an exception (avoid unacceptable rounding);
     * - Return the rounded integer value.
     *
     * @param value Floating-point
     * @return Magnified integer (long)
     * @throws IllegalArgumentException Thrown when rounding error exceeds threshold
     */
    public static long floatToIntForHashing(double value) {
        return floatToInt(value, 8);
    }

    /**
     * USD precision conversion: Magnify by 1e6 and round, used for USD-type transfer signing.
     *
     * @param value Amount
     * @return Magnified integer
     */
    public static long floatToUsdInt(double value) {
        return floatToInt(value, 6);
    }

    /**
     * Generic integer conversion: Magnify by 10^power and round (consistent with Python float_to_int).
     * <p>
     * Rules:
     * - with_decimals = x * 10^power;
     * - If |round(with_decimals) - with_decimals| >= 1e-3, throw an exception;
     * - Return the rounded integer value.
     *
     * @param value Floating-point
     * @param power Exponent of magnification factor (e.g., 8 means 1e8)
     * @return Magnified integer
     * @throws IllegalArgumentException Thrown when rounding error exceeds threshold
     */
    public static long floatToInt(double value, int power) {
        double withDecimals = value * Math.pow(10, power);
        double rounded = Math.rint(withDecimals); // Rounding to nearest even, similar to Python's round behavior
        if (Math.abs(rounded - withDecimals) >= 1e-3) {
            throw new IllegalArgumentException("floatToInt causes rounding: " + value);
        }
        return (long) Math.round(withDecimals);
    }

    /**
     * Get current millisecond timestamp.
     *
     * @return Millisecond timestamp
     */
    public static long getTimestampMs() {
        return System.currentTimeMillis();
    }

    /**
     * Convert order type to wire structure (Map).
     *
     * @param orderType Order type
     * @return Map structure for serialization
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
            trigObj.put("isMarket", orderType.getTrigger().isMarket());
            // Important: triggerPx must also be converted via floatToWire
            String triggerPx = orderType.getTrigger().getTriggerPx();
            if (triggerPx != null && !triggerPx.isEmpty()) {
                trigObj.put("triggerPx", floatToWire(Double.parseDouble(triggerPx)));
            }
            trigObj.put("tpsl", orderType.getTrigger().getTpsl());
            out.put("trigger", trigObj);
        }

        return out.isEmpty() ? null : out;
    }

    /**
     * Convert order request to wire structure (OrderWire).
     * <p>
     * Note: sz and limitPx are string types, but must be converted to floatToWire standard format when signing.
     *
     * @param coinId Integer asset ID
     * @param req    Order request
     * @return OrderWire
     */
    public static OrderWire orderRequestToOrderWire(int coinId, OrderRequest req) {
        // Important: Strings must be converted via floatToWire to ensure signature format consistency with protocol
        // floatToWire removes trailing zeros and avoids scientific notation, complying with Hyperliquid protocol requirements
        String szStr = req.getSz() != null ? floatToWire(Double.parseDouble(req.getSz())) : null;
        String pxStr = req.getLimitPx() != null && !req.getLimitPx().isEmpty() 
                ? floatToWire(Double.parseDouble(req.getLimitPx())) : null;
        Object orderTypeWire = orderTypeToWire(req.getOrderType());
        return new OrderWire(coinId, req.getIsBuy(), szStr, pxStr, orderTypeWire, req.getReduceOnly(), req.getCloid());
    }

    /**
     * Calculate L1 action hash.
     * Structure: msgpack(action) + 8-byte nonce + vaultAddress flag and address + expiresAfter flag and value ->
     * keccak256.
     *
     * @param action       Action object (Map/POJO)
     * @param nonce        Random number/timestamp
     * @param vaultAddress Optional vault address (0x prefixed address or null)
     * @param expiresAfter Optional expiration time (milliseconds)
     * @return 32-byte hash
     */
    public static byte[] actionHash(Object action, long nonce, String vaultAddress, Long expiresAfter) {
        // Fully aligned with Python's action_hash serialization and concatenation rules:
        // 1) MessagePack Map encoding of action (preserving insertion order);
        // 2) Directly append 8-byte big-endian raw bytes of nonce;
        // 3) vaultAddress: null -> append single byte 0x00; non-null -> append 0x01 followed by 20-byte address;
        // 4) expiresAfter: only when non-null, append single byte 0x00 + 8-byte big-endian raw bytes;
        try {
            byte[] actionMsgpack = packAsMsgpack(action);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(actionMsgpack.length + 64);
            // Write msgpack-encoded action
            baos.write(actionMsgpack);

            // Append nonce 8 bytes (big-endian)
            byte[] nonceBytes = java.nio.ByteBuffer.allocate(8).putLong(nonce).array();
            baos.write(nonceBytes);

            // Append vaultAddress flag and address
            if (vaultAddress == null) {
                baos.write(new byte[]{0x00});
            } else {
                baos.write(new byte[]{0x01});
                byte[] addrBytes = addressToBytes(vaultAddress);
                if (addrBytes.length != 20) {
                    throw new IllegalArgumentException("vaultAddress must be 20 bytes");
                }
                baos.write(addrBytes);
            }

            if (expiresAfter != null) {
                baos.write(new byte[]{0x00});
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
     * Encode any Java object in a manner equivalent to Python's msgpack.packb.
     * Supports Map (recommended to use LinkedHashMap to maintain insertion order), List, String, numbers, boolean, and null.
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
                    // Key encoded as string
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

            // Other types (e.g., custom POJOs) uniformly converted to Map or String
            case OrderWire ow -> {
                Map<String, Object> m = new LinkedHashMap<>();
                // Key order strictly aligned with Python: a, b, p, s, r, t, (c last)
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
        // Fallback to string representation
        packer.packString(String.valueOf(obj));
    }

    /**
     * Convert address string to byte array (remove 0x prefix).
     * <p>
     * Behavior description:
     * - Strict mode (enabled by default): Only accepts 20-byte addresses (40 hexadecimal characters), otherwise throws IllegalArgumentException;
     * - Compatibility mode (can be enabled via setStrictAddressLength(false) or -Dhyperliquid.address.strict=false):
     * Non-20-byte input will be downgraded: if length > 20, truncate to last 20 bytes; if length < 20, pad with zeros on the left to 20 bytes.
     *
     * @param address Ethereum address, supports with or without 0x prefix
     * @return 20-byte representation of the address
     * @throws IllegalArgumentException Thrown when address is null, non-hexadecimal, or not 20 bytes in strict mode
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

        // Compatibility mode: >20 bytes take last 20 bytes; <20 bytes pad with zeros on the left
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
     * Sign L1 action for user (EIP-712 Typed Data).
     *
     * @param credentials User credentials (private key)
     * @return r/s/v hexadecimal signature
     * @throws HypeError Thrown when serialization or signing process encounters exceptions (wraps underlying exception information)
     */
    public static Map<String, Object> signTypedData(Credentials credentials, String typedDataJson) {
        // Use standard EIP-712 structured data encoding and signing (consistent with Python eth_account.encode_typed_data).
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
     * Construct Phantom Agent (consistent with Python construct_phantom_agent).
     */
    public static Map<String, Object> constructPhantomAgent(byte[] hash, boolean isMainnet) {
        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("source", isMainnet ? "a" : "b");
        // bytes32 expressed as 0x-prefixed hexadecimal string, compatible with web3j StructuredDataEncoder
        agent.put("connectionId", Numeric.toHexString(hash));
        return agent;
    }

    /**
     * Generate EIP-712 TypedData JSON (consistent with Python l1_payload).
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
        try {
            return JSONUtil.writeValueAsString(full);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new HypeError("Failed to build typed data json: " + e.getMessage());
        }
    }

    /**
     * Sign L1 action (complete process): calculate actionHash -> construct phantom agent -> generate EIP-712 typed data
     * -> sign.
     */
    public static Map<String, Object> signL1Action(Credentials credentials, Object action, String vaultAddress,
                                                   long nonce, Long expiresAfter, boolean isMainnet) {
        byte[] hash = actionHash(action, nonce, vaultAddress, expiresAfter);
        Map<String, Object> agent = constructPhantomAgent(hash, isMainnet);
        String typedJson = l1PayloadJson(agent);
        return signTypedData(credentials, typedJson);
    }

    /**
     * Construct EIP-712 TypedData JSON for user-signed action (consistent with Python user_signed_payload).
     * Description:
     * - primaryType is the specific transaction type, e.g., "HyperliquidTransaction:UsdSend".
     * - payloadTypes is the list of field type definitions for that transaction type, e.g., USD_SEND_SIGN_TYPES.
     * - action is the message body, which must contain "signatureChainId" (hexadecimal string, e.g., 0x66eee) and
     * "hyperliquidChain" ("Mainnet" or "Testnet").
     *
     * @param primaryType  Primary type name
     * @param payloadTypes Field type definition list
     * @param action       Action message (must contain signatureChainId and hyperliquidChain)
     * @return EIP-712 TypedData JSON string
     */
    public static String userSignedPayloadJson(String primaryType, List<Map<String, Object>> payloadTypes,
                                               Map<String, Object> action) {
        // Parse the hexadecimal string of signatureChainId to integer chain ID
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
        try {
            return JSONUtil.writeValueAsString(full);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new HypeError("Failed to build user-signed typed data json: " + e.getMessage());
        }
    }

    /**
     * Sign user-signed action (consistent with Python sign_user_signed_action).
     * Rules:
     * - Automatically set signatureChainId (0x66eee) and hyperliquidChain (based on isMainnet).
     * - Use userSignedPayloadJson to construct EIP-712 TypedData and sign it.
     *
     * @param credentials  User credentials
     * @param action       Action message (will be supplemented with signature-related fields)
     * @param payloadTypes Field type definition list
     * @param primaryType  Primary type name
     * @param isMainnet    Whether it's mainnet
     * @return r/s/v signature
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
     * Generic: Recover address from EIP-712 TypedData JSON and r/s/v signature.
     *
     * @param typedDataJson EIP-712 TypedData JSON string
     * @param signature     r/s/v signature
     * @return 0x address (lowercase)
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
            // Pure EIP-712 non-prefixed recovery: directly perform ecrecover based on digest and r/s/v, avoiding any additional hashing or prefixes.
            // Note: web3j's signedMessageToKey may hash the input or couple with prefix conventions, here we use a lower-level
            // recoverFromSignature.
            int recId;
            if (vInt == 27 || vInt == 28) {
                recId = vInt - 27;
            } else if (vInt == 0 || vInt == 1) {
                recId = vInt;
            } else if (vInt >= 35) {
                // Compatible with EIP-155-style v values (although EIP-712 typically doesn't carry chainId), only for robustness handling
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
     * Public helper method: Recover address from digest and r/s/v (pure EIP-712 without prefix).
     * Can be used for testing and diagnostics, without needing to construct complete typedData JSON.
     *
     * @param digest    32-byte EIP-712 hash
     * @param signature r/s/v signature
     * @return Recovered address (0x lowercase)
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
     * ecrecover implemented using BouncyCastle: Recover uncompressed public key (64 bytes) from r/s/v and message digest.
     * This implementation follows secp256k1 curve parameters, avoiding constraints of web3j high-level API on message hashing or prefixes.
     *
     * @param recId  0 or 1 (obtained by normalizing v)
     * @param r      ECDSA r value
     * @param s      ECDSA s value
     * @param digest 32-byte message digest (EIP-712 hash)
     * @return BigInteger representation of uncompressed public key (64 bytes x||y)
     */
    private static java.math.BigInteger recoverPublicKeyFromSignature(int recId, java.math.BigInteger r,
                                                                      java.math.BigInteger s, byte[] digest) {
        // Using BouncyCastle curve parameters
        org.bouncycastle.asn1.x9.X9ECParameters x9 = org.bouncycastle.crypto.ec.CustomNamedCurves
                .getByName("secp256k1");
        org.bouncycastle.crypto.params.ECDomainParameters curve = new org.bouncycastle.crypto.params.ECDomainParameters(
                x9.getCurve(), x9.getG(), x9.getN(), x9.getH());

        java.math.BigInteger n = curve.getN();
        java.math.BigInteger i = java.math.BigInteger.valueOf(recId / 2);
        java.math.BigInteger x = r.add(i.multiply(n));

        // Determine the parity of compressed point based on recId parity
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
        byte[] pubKeyEncoded = q.getEncoded(false); // 65 bytes, first byte is 0x04
        byte[] pubKeyNoPrefix = java.util.Arrays.copyOfRange(pubKeyEncoded, 1, pubKeyEncoded.length);
        return new java.math.BigInteger(1, pubKeyNoPrefix);
    }

    /**
     * Decompress the given x coordinate and y parity flag into a point on the curve (uncompressed).
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
     * Recover signer address from L1 action signature (consistent with Python recover_agent_or_user_from_l1_action).
     *
     * @param action       L1 action (Map or List)
     * @param vaultAddress Valid vault address (can be null)
     * @param nonce        Millisecond timestamp
     * @param expiresAfter Expiration millisecond offset (can be null)
     * @param isMainnet    Whether it's mainnet
     * @param signature    r/s/v signature
     * @return Recovered 0x address (lowercase)
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
     * Recover user address from user-signed action (consistent with Python recover_user_from_user_signed_action).
     * Note: Will not modify action's signatureChainId, only set hyperliquidChain based on isMainnet.
     *
     * @param action       Action message (must contain signatureChainId)
     * @param signature    r/s/v signature
     * @param payloadTypes Field type definition list
     * @param primaryType  Primary type name
     * @param isMainnet    Whether it's mainnet
     * @return Recovered 0x address (lowercase)
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
     * Convert multiple OrderWires to L1 action object for signing and sending.
     * <p>
     * The generated structure looks like:
     * {
     * "type": "order",
     * "orders": [
     * {"coin": 1, "isBuy": true, "sz": "0.1", "limitPx": "60000", "orderType":
     * {...}, "reduceOnly": false, "cloid": "..."},
     * ...
     * ]
     * }
     *
     * @param orders Order wire list
     * @return Map action object {"type": "order", "orders": [...]}
     */
    public static Map<String, Object> orderWiresToOrderAction(List<OrderWire> orders) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "order");


        List<Map<String, Object>> wires = new ArrayList<>();
        for (OrderWire o : orders) {
            Map<String, Object> w = new LinkedHashMap<>();
            // Key order strictly aligned with Python: a, b, p, s, r, t, (c last)
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
        // Consistent with Python, default grouping is "na" (placed after orders)
        action.put("grouping", "na");
        return action;
    }

    /**
     * Set address length validation strict mode.
     *
     * @param strict Whether to strictly require address to be 20 bytes
     */
    public static void setStrictAddressLength(boolean strict) {
        STRICT_ADDRESS_LENGTH = strict;
    }

    /**
     * Get current address length validation mode.
     *
     * @return true means strict mode; false means compatibility mode
     */
    public static boolean isStrictAddressLength() {
        return STRICT_ADDRESS_LENGTH;
    }

    /**
     * Multi-signature action signing (aligned with Python sign_multi_sig_action).
     * <p>
     * Used for multi-signature accounts to execute operations, requires constructing special multiSigActionHash and signing.
     * </p>
     *
     * @param wallet         Signer wallet
     * @param multiSigAction Multi-signature action object (contains payload and other fields)
     * @param isMainnet      Whether it's mainnet
     * @param vaultAddress   Vault address (can be null)
     * @param nonce          Random number/timestamp
     * @param expiresAfter   Expiration time (milliseconds, can be null)
     * @return EIP-712 signature result (Map containing r, s, v)
     */
    public static Map<String, Object> signMultiSigAction(
            Credentials wallet,
            Map<String, Object> multiSigAction,
            boolean isMainnet,
            String vaultAddress,
            long nonce,
            Long expiresAfter
    ) {
        // 1. Extract payload
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) multiSigAction.get("payload");
        if (payload == null) {
            throw new IllegalArgumentException("multiSigAction must contain 'payload'");
        }

        // 2. Extract inner action
        @SuppressWarnings("unchecked")
        Map<String, Object> innerAction = (Map<String, Object>) payload.get("action");
        if (innerAction == null) {
            throw new IllegalArgumentException("payload must contain 'action'");
        }

        // 3. Calculate hash of inner action (consistent with L1 action)
        byte[] innerActionHash = actionHash(innerAction, nonce, vaultAddress, expiresAfter);

        // 4. Construct multiSigActionHash: put innerActionHash into payload
        Map<String, Object> enrichedPayload = new LinkedHashMap<>(payload);
        enrichedPayload.put("multiSigActionHash", "0x" + Numeric.toHexStringNoPrefix(innerActionHash));

        // 5. Construct EIP-712 TypedData
        String chainId = isMainnet ? "0x66eee" : "0x66eef";
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("name", "Exchange");
        domain.put("version", "1");
        domain.put("chainId", chainId);
        domain.put("verifyingContract", "0x0000000000000000000000000000000000000000");

        // MultiSig EIP-712 types
        Map<String, Object> multiSigType = new LinkedHashMap<>();
        multiSigType.put("name", "multiSigUser");
        multiSigType.put("type", "address");
        Map<String, Object> outerSignerType = new LinkedHashMap<>();
        outerSignerType.put("name", "outerSigner");
        outerSignerType.put("type", "address");
        Map<String, Object> actionType = new LinkedHashMap<>();
        actionType.put("name", "action");
        actionType.put("type", "string");
        Map<String, Object> multiSigHashType = new LinkedHashMap<>();
        multiSigHashType.put("name", "multiSigActionHash");
        multiSigHashType.put("type", "bytes32");

        Map<String, Object> types = new LinkedHashMap<>();
        types.put("EIP712Domain", Arrays.asList(
                Map.of("name", "name", "type", "string"),
                Map.of("name", "version", "type", "string"),
                Map.of("name", "chainId", "type", "string"),
                Map.of("name", "verifyingContract", "type", "address")
        ));
        types.put("HyperliquidTransaction:MultiSig", Arrays.asList(
                multiSigType,
                outerSignerType,
                actionType,
                multiSigHashType
        ));

        Map<String, Object> typedData = new LinkedHashMap<>();
        typedData.put("domain", domain);
        typedData.put("types", types);
        typedData.put("primaryType", "HyperliquidTransaction:MultiSig");
        typedData.put("message", enrichedPayload);

        // 6. EIP-712 signing (convert to JSON string)
        try {
            String typedDataJson = JSONUtil.writeValueAsString(typedData);
            return signTypedData(wallet, typedDataJson);
        } catch (Exception e) {
            throw new HypeError("Failed to sign multi-sig action", e);
        }
    }
}
