package io.github.hyperliquid.sdk.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.api.API;
import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.OrderWire;
import io.github.hyperliquid.sdk.utils.Signing;
import org.web3j.crypto.Credentials;

import java.util.*;

/**
 * Exchange 客户端，负责下单、撤单、转账等 L1/L2 操作。
 * 当前版本实现核心下单与批量下单，其他 L1 操作将在后续补充。
 */
public class Exchange extends API {
    
    private final Credentials wallet;
    private final Info info;

    /**
     * 构造 Exchange 客户端。
     *
     * @param baseUrl API 根地址
     * @param timeout 超时秒数
     * @param wallet  用户钱包凭证（包含私钥与地址）
     * @param info    Info 客户端（用于名称到资产映射与行情辅助）
     */
    public Exchange(String baseUrl, int timeout, Credentials wallet, Info info) {
        super(baseUrl, timeout);
        this.wallet = wallet;
        this.info = info;
    }

    /**
     * 单笔下单。
     *
     * @param req 下单请求（包含币种名、方向、数量、价格、类型、reduceOnly、cloid）
     * @return 交易接口响应 JSON
     */
    public JsonNode order(OrderRequest req) {
        int assetId = info.nameToAsset(req.getCoin());
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, req);
        Map<String, Object> action = Signing.orderWiresToOrderAction(List.of(wire));

        return postAction(action, null, null);
    }

    /**
     * 批量下单。
     *
     * @param requests 下单请求列表
     * @return 交易接口响应
     */
    public JsonNode bulkOrders(List<OrderRequest> requests) {
        List<OrderWire> wires = new ArrayList<>();
        for (OrderRequest r : requests) {
            int assetId = info.nameToAsset(r.getCoin());
            wires.add(Signing.orderRequestToOrderWire(assetId, r));
        }
        Map<String, Object> action = Signing.orderWiresToOrderAction(wires);
        return postAction(action, null, null);
    }

    /**
     * 更改杠杆
     * @param coinName 币种名
     * @param crossed 是否全仓
     * @param leverage 杠杆倍数
     * @return 响应 JSON
     * */
    public JsonNode updateLeverage(String coinName, boolean crossed, int leverage) {
        int assetId = info.nameToAsset(coinName);

        Map<String, Object> actions = new LinkedHashMap<>() {{
            this.put("type", "updateLeverage");
            this.put("asset", assetId);
            this.put("isCross", crossed);
            this.put("leverage", leverage);
        }};

        return this.postAction(actions, null, null);
    }

    /**
     * 根据 OID 撤单。
     *
     * @param coinName 币种名
     * @param oid      订单 OID
     * @return 响应 JSON
     */
    public JsonNode cancel(String coinName, int oid) {
        int assetId = info.nameToAsset(coinName);
        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("a", assetId);
        cancel.put("o", oid);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "cancel");
        action.put("cancels", List.of(cancel));
        return postAction(action, null, null);
    }

    /**
     * 根据 Cloid 撤单。
     *
     * @param coinName 币种名
     * @param cloid    客户端订单 ID
     * @return 响应 JSON
     */
    public JsonNode cancelByCloid(String coinName, String cloid) {
        int assetId = info.nameToAsset(coinName);
        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("asset", assetId);
        cancel.put("cloid", cloid);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "cancelByCloid");
        action.put("cancels", List.of(cancel));
        return postAction(action, null, null);
    }

    /**
     * 修改订单（通过 OID）。
     *
     * @param coinName 币种名
     * @param oid      原订单 OID
     * @param newReq   新订单请求（价格/数量/类型等）
     * @return 响应 JSON
     */
    public JsonNode modifyOrder(String coinName, int oid, OrderRequest newReq) {
        int assetId = info.nameToAsset(coinName);
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, newReq);
        Map<String, Object> modify = new LinkedHashMap<>();
        modify.put("coin", assetId);
        modify.put("oid", oid);
        modify.put("order", wire);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "modifyOrder");
        action.put("modifies", List.of(modify));
        return postAction(action, null, null);
    }

    /**
     * 统一 L1 动作发送封装（签名并 POST 到 /exchange）。
     *
     * @param action       L1 动作（Map）
     * @param vaultAddress 可选 vault 地址
     * @param expiresAfter 可选过期时间
     * @return JSON 响应
     */
    public JsonNode postAction(Map<String, Object> action, String vaultAddress, Long expiresAfter) {
        long nonce = Signing.getTimestampMs();
        byte[] hash = Signing.actionHash(action, nonce, vaultAddress, expiresAfter);

        // EIP-712 签名域与类型（占位，后续将与后端规范完全对齐）
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("name", "Exchange");
        domain.put("version", "1");

        Map<String, Object> types = new LinkedHashMap<>();
        Map<String, Object> eip712Domain = new LinkedHashMap<>();
        eip712Domain.put("name", List.of(Map.of("name", "name", "type", "string")));
        eip712Domain.put("version", List.of(Map.of("name", "version", "type", "string")));
        types.put("EIP712Domain", eip712Domain);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("primaryType", "Action");
        message.put("actionHash", Base64.getEncoder().encodeToString(hash));
        Map<String, String> sig = Signing.signTypedData(wallet, domain, types, message);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("nonce", nonce);
        payload.put("signature", sig);
        if (vaultAddress != null) payload.put("vaultAddress", vaultAddress);
        if (expiresAfter != null) payload.put("expiresAfter", expiresAfter);
        payload.put("signatureChainId", 1);

        return post("/exchange", payload);
    }
}
