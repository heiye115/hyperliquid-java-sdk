package io.github.hyperliquid.sdk.apis;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.model.info.Meta;
import io.github.hyperliquid.sdk.model.info.UpdateLeverage;
import io.github.hyperliquid.sdk.model.order.*;
import io.github.hyperliquid.sdk.utils.*;
import lombok.Setter;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExchangeClient 客户端，负责下单、撤单、转账等 L1/L2 操作。
 * 当前版本实现核心下单与批量下单，其他 L1 操作将在后续补充。
 */
public class Exchange {

    /**
     * 用户钱包凭证（包含私钥与地址）
     */
    private final Credentials wallet;

    /**
     * HTTP 客户端
     */
    private final HypeHttpClient hypeHttpClient;


    /**
     * Info 客户端实例
     */
    private final Info info;


    /**
     * 以太坊地址（0x 前缀）
     */
    @Setter
    private String vaultAddress;

    /**
     * 过期时间
     */
    @Setter
    private Long expiresAfter;

    /**
     * 构造 Exchange 客户端。
     *
     * @param hypeHttpClient HTTP 客户端实例
     * @param wallet         用户钱包凭证
     * @param info           Info 客户端实例
     */
    public Exchange(HypeHttpClient hypeHttpClient, Credentials wallet, Info info) {
        this.hypeHttpClient = hypeHttpClient;
        this.wallet = wallet;
        this.info = info;

    }

    /**
     * 更改杠杆
     *
     * @param coinName 币种名
     * @param crossed  是否全仓
     * @param leverage 杠杆倍数
     * @return 响应 JSON
     *
     */
    public UpdateLeverage updateLeverage(String coinName, boolean crossed, int leverage) {
        int assetId = ensureAssetId(coinName);
        Map<String, Object> actions = new LinkedHashMap<>() {{
            this.put("type", "updateLeverage");
            this.put("asset", assetId);
            this.put("isCross", crossed);
            this.put("leverage", leverage);
        }};
        return JSONUtil.convertValue(postAction(actions), UpdateLeverage.class);
    }

    /**
     * 单笔下单（支持 builder）。
     * <p>
     * - 普通下单场景 ：当用户不指定 builder 参数时，订单会默认使用 Hyperliquid 平台的核心撮合引擎进行交易处理。
     * - Builder 参数的专用用途 ：仅在用户希望将订单路由到特定的 Builder-deployed perp
     * dex（由第三方开发者部署的永续合约去中心化交易所）时才需要传递该参数。
     * - 例如：当用户想利用某个 Builder 提供的定制化流动性、特定交易策略或支付 Builder 费用时，才需要设置 builder 参数。
     *
     * @param req     下单请求
     * @param builder 可选 builder 信息（可包含键 "b"）
     * @return 交易接口响应 JSON
     */
    public Order order(OrderRequest req, Map<String, Object> builder) {
        // 方法注释：单笔下单（支持 builder）。
        // 修复说明：避免重复调用 postAction(action) 导致二次签名与不同 nonce，从而引发
        // "本地签名恢复地址与钱包地址不一致" 等问题。改为仅调用一次 postAction 并复用返回节点。
        // 市价下单转换
        marketOpenTransition(req);
        int assetId = ensureAssetId(req.getCoin());
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, req);
        Map<String, Object> action = buildOrderAction(List.of(wire), builder);
        // 先拿原始响应，若为错误字符串结构（status=err 且 response 为字符串），直接抛出 HypeError
        com.fasterxml.jackson.databind.JsonNode node = postAction(action);
        if (node != null && node.has("status") && "err".equals(node.get("status").asText())) {
            com.fasterxml.jackson.databind.JsonNode resp = node.get("response");
            if (resp != null && resp.isTextual()) {
                // 与服务端保持一致：错误字符串直接向上抛出，避免 Jackson 反序列化失败
                throw new HypeError(resp.asText());
            }
        }

        // 导致重新生成 nonce 并且进行第二次签名与提交，从而与 Python SDK 的一次性提交行为不一致。
        return JSONUtil.convertValue(node, Order.class);
    }


    /**
     * 单笔下单（普通下单场景）
     *
     * @param req 下单请求
     * @return 交易接口响应 JSON
     */
    public Order order(OrderRequest req) {
        return order(req, null);
    }

    /**
     * 批量下单（支持 builder）。
     *
     * @param requests 下单请求列表
     * @param builder  可选 builder
     * @return 响应 JSON
     */
    public JsonNode bulkOrders(List<OrderRequest> requests, Map<String, Object> builder) {
        List<OrderWire> wires = new ArrayList<>();
        for (OrderRequest r : requests) {
            int assetId = ensureAssetId(r.getCoin());
            wires.add(Signing.orderRequestToOrderWire(assetId, r));
        }
        Map<String, Object> action = buildOrderAction(wires, builder);
        return postAction(action);
    }

    /**
     * 批量下单（无 builder 便捷重载）。
     *
     * @param requests 下单请求列表
     * @return 响应 JSON
     */
    public JsonNode bulkOrders(List<OrderRequest> requests) {
        return bulkOrders(requests, null);
    }

    /**
     * 根据 OID 撤单（保持与 Python cancel 行为一致）。
     *
     * @param coinName 币种名
     * @param oid      订单 OID
     * @return 响应 JSON
     */
    public JsonNode cancel(String coinName, long oid) {
        int assetId = ensureAssetId(coinName);
        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("coin", assetId);
        cancel.put("oid", oid);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "cancel");
        action.put("cancels", List.of(cancel));
        return postAction(action);
    }

    /**
     * 根据 Cloid 撤单（保持与 Python cancel_by_cloid 行为一致）。
     *
     * @param coinName 币种名
     * @param cloid    客户端订单 ID
     * @return 响应 JSON
     */
    public JsonNode cancelByCloid(String coinName, Cloid cloid) {
        int assetId = ensureAssetId(coinName);
        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("coin", assetId);
        cancel.put("cloid", cloid == null ? null : cloid.getRaw());
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "cancelByCloid");
        action.put("cancels", List.of(cancel));
        return postAction(action);
    }

    /**
     * 修改订单（通过 OID）。
     *
     * @param coinName 币种名
     * @param oid      原订单 OID
     * @param newReq   新订单请求（价格/数量/类型等）
     * @return 响应 JSON
     */
    public JsonNode modifyOrder(String coinName, long oid, OrderRequest newReq) {
        int assetId = ensureAssetId(coinName);
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, newReq);
        Map<String, Object> modify = new LinkedHashMap<>();
        modify.put("coin", assetId);
        modify.put("oid", oid);
        modify.put("order", wire);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "modifyOrder");
        action.put("modifies", List.of(modify));
        return postAction(action);
    }

    /**
     * 构建下单动作（包含 grouping:"na" 与可选 builder）。
     *
     * @param wires   订单 wire 列表
     * @param builder 可选 builder
     * @return L1 动作 Map
     */
    private Map<String, Object> buildOrderAction(List<OrderWire> wires, Map<String, Object> builder) {
        Map<String, Object> action = Signing.orderWiresToOrderAction(wires);
        // 插入 Python 对齐字段：grouping = "na"
        action.put("grouping", "na");

        if (builder != null && !builder.isEmpty()) {
            // 仅保留官方文档允许的字段：b（地址）与 f（费用）；其余键将被忽略，避免 422 反序列化失败
            Map<String, Object> filtered = new LinkedHashMap<>();
            if (builder.containsKey("b")) {
                Object bVal = builder.get("b");
                if (bVal instanceof String s) {
                    String addr = s.toLowerCase();
                    // 校验地址格式：必须为 0x 开头的 42 字符长度十六进制地址
                    if (!addr.matches("^0x[0-9a-f]{40}$")) {
                        throw new HypeError("builder.b 必须是 42 位 0x 开头的十六进制地址，例如 0x000...000");
                    }
                    filtered.put("b", addr);
                }
            }
            if (builder.containsKey("f")) {
                Object fVal = builder.get("f");
                if (!(fVal instanceof Number)) {
                    throw new HypeError("builder.f 必须是非负整数（数值类型）");
                }
                long f = ((Number) fVal).longValue();
                if (f < 0) {
                    throw new HypeError("builder.f 不能为负数");
                }
                // 限制一个合理上限，避免误传超大数导致后端拒绝（可根据业务调整）
                if (f > 1_000_000L) {
                    throw new HypeError("builder.f 过大，请确认单位与取值范围");
                }
                filtered.put("f", f);
            }
            if (!filtered.isEmpty()) {
                action.put("builder", filtered);
            }
        }
        return action;
    }

    /**
     * 统一 L1 动作发送封装（签名并 POST 到 /exchange）。
     *
     * <p>
     * 规则：
     * - nonce 使用毫秒时间戳（与 Python get_timestamp_ms 一致）；
     * - usdClassTransfer/sendAsset 类型动作不附带 vaultAddress（保持 Python 行为一致）；
     * - 其它动作使用已设置的 vaultAddress 与 expiresAfter；
     * - 使用 Signing.signL1Action 完成 TypedData 构造与签名。
     *
     * @param action L1 动作（Map）
     * @return JSON 响应
     */
    public JsonNode postAction(Map<String, Object> action) {
        long nonce = Signing.getTimestampMs();

        String type = String.valueOf(action.getOrDefault("type", ""));
        String effectiveVault = ("usdClassTransfer".equals(type) || "sendAsset".equals(type)) ? null : vaultAddress;

        Map<String, Object> signature = Signing.signL1Action(
                wallet,
                action,
                effectiveVault,
                nonce,
                expiresAfter,
                isMainnet());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("nonce", nonce);
        payload.put("signature", signature);
        payload.put("vaultAddress", effectiveVault);
        payload.put("expiresAfter", expiresAfter);

        return hypeHttpClient.post("/exchange", payload);
    }

    /**
     * 校验并返回资产 ID。
     *
     * @param coinName 币种名
     * @return 资产 ID
     * @throws HypeError 当无法映射时抛出
     */
    private int ensureAssetId(String coinName) {
        Integer assetId = info.nameToAsset(coinName);
        if (assetId == null) {
            throw new HypeError("Unknown coin name: " + coinName);
        }
        return assetId;
    }

    private void marketOpenTransition(OrderRequest req) {
        if (req.getLimitPx() == null && req.getOrderType().getLimit() != null &&
                req.getOrderType().getLimit().getTif() == Tif.IOC) {
            double slipPx = computeSlippagePrice(req.getCoin(), req.getIsBuy(), req.getSlippage());
            req.setLimitPx(slipPx);
        }
    }

    /**
     * 计算带滑点的价格，并进行与 Python SDK 一致的精度处理。
     * 规则：
     * - 若未指定 px，则从 allMids 获取中间价；
     * - 根据 isBuy 调整为进取价：buy 使用 (1+slippage)，sell 使用 (1-slippage)；
     * - 使用 5 位有效数字进行初步四舍五入；
     * - 对于永续（perp）价格，最终按 (6 - szDecimals) 小数位进行四舍五入（与 Python 一致）。
     * 注意：当前实现默认针对永续（perp）资产。Spot 的 (8 - szDecimals) 规则可在后续扩展。
     *
     * @param coin     币种名称，例如 "ETH"
     * @param isBuy    是否买入（用于确定滑点方向）
     * @param slippage 滑点比例，例如 0.05 代表 5%
     * @return 处理后的价格（double）
     */
    public double computeSlippagePrice(String coin, boolean isBuy, double slippage) {
        double basePx;
        Map<String, String> mids = info.allMids();
        String midStr = mids.get(coin);
        if (midStr == null) {
            throw new HypeError("Failed to get mid price for coin " + coin + " (allMids returned empty or does not contain the coin)");
        }
        try {
            basePx = Double.parseDouble(midStr);
        } catch (NumberFormatException e) {
            throw new HypeError("Mid price format exception: " + midStr, e);
        }

        double adjusted = basePx * (isBuy ? (1.0 + slippage) : (1.0 - slippage));

        // 5 位有效数字处理
        BigDecimal bd = BigDecimal.valueOf(adjusted);
        bd = bd.round(new MathContext(5, RoundingMode.HALF_UP));

        // 根据 perp 的精度规则进一步四舍五入：decimals = 6 - szDecimals
        Meta.Universe metaUniverse = info.getMetaUniverse(coin);
        Integer szDecimals = metaUniverse.getSzDecimals();
        if (szDecimals == null) {
            throw new HypeError("Failed to get szDecimals from Meta.Universe, coin: " + coin);
        }
        int decimals = 6 - szDecimals;
        if (decimals < 0) {
            // 防御：避免出现负小数位导致异常，直接不再缩放
            decimals = 0;
        }
        bd = bd.setScale(decimals, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * 判断当前是否主网。
     *
     * @return 主网返回 true，否则 false
     */
    private boolean isMainnet() {
        return Constants.MAINNET_API_URL.equals(hypeHttpClient.getBaseUrl());
    }
}
