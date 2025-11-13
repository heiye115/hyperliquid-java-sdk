package io.github.hyperliquid.sdk.apis;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.hyperliquid.sdk.model.approve.ApproveAgentResult;
import io.github.hyperliquid.sdk.model.info.ClearinghouseState;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

    private final Cache<String, Boolean> dexEnabledCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("^0x[0-9a-f]{40}$");

    private final Cache<String, Integer> szDecimalsCache = Caffeine.newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final java.util.Map<String, Double> defaultSlippageByCoin = new java.util.concurrent.ConcurrentHashMap<>();
    private Double defaultSlippage = 0.05;

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
     * 计划撤单（scheduleCancel）。
     *
     * @param timeMs 指定撤单执行的毫秒时间戳；null 表示立即执行
     * @return JSON 响应
     */
    public JsonNode scheduleCancel(Long timeMs) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "scheduleCancel");
        if (timeMs != null) {
            action.put("time", timeMs);
        }
        return postAction(action);
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
        Map<String, Object> actions = new LinkedHashMap<>() {
            {
                this.put("type", "updateLeverage");
                this.put("asset", assetId);
                this.put("isCross", crossed);
                this.put("leverage", leverage);
            }
        };
        return JSONUtil.convertValue(postAction(actions), UpdateLeverage.class);
    }

    /**
     * 单笔下单（支持 builder）。
     * <p>
     * - 普通下单场景 ：当用户不指定 builder 参数时，订单会默认使用 Hyperliquid 平台的核心撮合引擎进行交易处理。
     * - Builder 参数的专用用途 ：仅在用户希望将订单路由到特定的 Builder-deployed perp
     * dex（由第三方开发者部署的永续合约去中心化交易所）时才需要传递该参数。
     * - 例如：当用户想利用某个 Builder 提供的定制化流动性、特定交易策略或支付 Builder 费用时，才需要设置 builder 参数。
     */
    public Order order(OrderRequest req, Map<String, Object> builder) {
        Long prevExpires = this.expiresAfter;
        if (prevExpires == null) {
            this.expiresAfter = 120_000L;
        }
        try {
            return innerOrder(req, builder);
        } finally {
            this.expiresAfter = prevExpires;
        }
    }

    /**
     * 内部下单实现（统一规范化→签名→提交）。
     *
     * <p>
     * 流程：
     * 1) 校验并启用 Dex Abstraction；
     * 2) {@link #prepareRequest(OrderRequest)} 推断市价平仓方向/数量；
     * 3) {@link #marketOpenTransition(OrderRequest)} 应用默认滑点计算市价占位价；
     * 4) {@link #normalizePerpLimitPx(OrderRequest)} 与
     * {@link #normalizePerpTriggerPx(OrderRequest)} 统一价格精度；
     * 5) 转换为 wire，构造动作并发送；
     * 6) 将服务端 JSON 映射为 {@link Order}。
     *
     * @param req     下单请求
     * @param builder 可选 builder（包含 b/f），为空时走平台默认引擎
     * @return 服务端返回的订单响应
     * @throws HypeError 当服务端返回 status=err 时抛出（携带原始响应）
     */
    private Order innerOrder(OrderRequest req, Map<String, Object> builder) {
        ensureDexAbstractionEnabled();
        OrderRequest effective = prepareRequest(req);
        marketOpenTransition(effective);
        normalizePerpLimitPx(effective);
        normalizePerpTriggerPx(effective);
        int assetId = ensureAssetId(effective.getCoin());
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, effective);
        Map<String, Object> action = buildOrderAction(List.of(wire), builder);
        com.fasterxml.jackson.databind.JsonNode node = postAction(action);
        if (node != null && node.has("status") && "err".equals(node.get("status").asText())) {
            com.fasterxml.jackson.databind.JsonNode resp = node.get("response");
            if (resp != null && resp.isTextual()) {
                throw new HypeError(resp.asText());
            }
        }
        return JSONUtil.convertValue(node, Order.class);
    }

    /**
     * 规范化永续（PERP）限价价格精度。
     *
     * <p>
     * 规则与 Python SDK 对齐：先按 5 位有效数字四舍五入，再按 (6 - szDecimals) 小数位四舍五入。
     * </p>
     *
     * @param req 下单请求（仅当 instrumentType=PERP 且 limitPx 非空时生效）
     */
    private void normalizePerpLimitPx(OrderRequest req) {
        if (req == null)
            return;
        if (req.getInstrumentType() != InstrumentType.PERP)
            return;
        Double px = req.getLimitPx();
        if (px == null)
            return;
        String coin = req.getCoin();
        Integer szDecimals = szDecimalsCache.getIfPresent(coin);
        if (szDecimals == null) {
            Meta.Universe mu = info.getMetaUniverse(coin);
            szDecimals = mu.getSzDecimals();
            if (szDecimals != null) {
                szDecimalsCache.put(coin, szDecimals);
            }
        }
        if (szDecimals == null)
            return;
        int decimals = 6 - szDecimals;
        if (decimals < 0)
            decimals = 0;
        BigDecimal bd = BigDecimal.valueOf(px).round(new MathContext(5, RoundingMode.HALF_UP))
                .setScale(decimals, RoundingMode.HALF_UP);
        req.setLimitPx(bd.doubleValue());
    }

    /**
     * 准备下单请求：推断市价平仓方向与数量。
     *
     * <p>
     * 当检测为“市价平仓占位”（IOC + reduceOnly=true 且 limitPx 为空）时：
     * - 若传入 isBuy 与 sz，则原样返回；
     * - 否则根据当前仓位签名尺寸推断方向（szi&lt;0 → 买入/平空；szi&gt;0 → 卖出/平多），
     * 并将数量设为传入 sz 或绝对仓位大小；
     * - 返回一个规范化的市价平仓请求。
     * </p>
     *
     * @param req 原始下单请求
     * @return 规范化后的下单请求
     * @throws HypeError 当无可平仓位时抛出
     */
    private OrderRequest prepareRequest(OrderRequest req) {
        if (isClosePositionMarket(req)) {
            if (req.getIsBuy() != null && req.getSz() != null) {
                return req;
            }
            double szi = inferSignedPosition(req.getCoin());
            if (szi == 0.0) {
                throw new HypeError("No position to close for coin " + req.getCoin());
            }
            boolean isBuy = szi < 0.0;
            double sz = (req.getSz() != null && req.getSz() > 0.0) ? req.getSz() : Math.abs(szi);
            return OrderRequest.Close.market(req.getCoin(), isBuy, sz, req.getCloid());
        }
        return req;
    }

    /**
     * 判定是否为“市价平仓占位”请求。
     *
     * @param req 下单请求
     * @return 是则返回 true，否则 false
     */
    private boolean isClosePositionMarket(OrderRequest req) {
        return req != null
                && req.getInstrumentType() == InstrumentType.PERP
                && req.getOrderType() != null
                && req.getOrderType().getLimit() != null
                && req.getOrderType().getLimit().getTif() == Tif.IOC
                && Boolean.TRUE.equals(req.getReduceOnly())
                && req.getLimitPx() == null;
    }

    /**
     * 推断当前账户在指定币种的“签名仓位尺寸”。
     *
     * <p>
     * 正数表示多仓，负数表示空仓；当无仓位或解析失败时返回 0.0。
     * </p>
     *
     * @param coin 币种名称
     * @return 签名尺寸（double）
     */
    private double inferSignedPosition(String coin) {
        ClearinghouseState state = info.userState(wallet.getAddress().toLowerCase());
        if (state == null || state.getAssetPositions() == null)
            return 0.0;
        for (ClearinghouseState.AssetPositions ap : state.getAssetPositions()) {
            ClearinghouseState.Position pos = ap.getPosition();
            if (pos != null && coin.equalsIgnoreCase(pos.getCoin())) {
                try {
                    return Double.parseDouble(pos.getSzi());
                } catch (Exception ignored) {
                    return 0.0;
                }
            }
        }
        return 0.0;
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
     * 更新隔离保证金
     *
     * @param amount   金额（USD，浮点，内部按微单位转换）
     * @param coinName 币种名
     * @return JSON 响应
     */
    public JsonNode updateIsolatedMargin(double amount, String coinName) {
        int assetId = ensureAssetId(coinName);
        long ntli = Signing.floatToUsdInt(amount);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "updateIsolatedMargin");
        action.put("asset", assetId);
        action.put("isBuy", true);
        action.put("ntli", ntli);
        return postAction(action);
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
        ensureDexAbstractionEnabled();
        int assetId = ensureAssetId(coinName);
        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("a", assetId);
        cancel.put("o", oid);
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
        cancel.put("asset", assetId);
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
        ensureDexAbstractionEnabled();
        int assetId = ensureAssetId(coinName);
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, newReq);
        Map<String, Object> modify = new LinkedHashMap<>();
        modify.put("oid", oid);
        modify.put("order", wire);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "batchModify");
        action.put("modifies", List.of(modify));
        return postAction(action);
    }

    public JsonNode modifyOrder(String coinName, Cloid cloid, OrderRequest newReq) {
        ensureDexAbstractionEnabled();
        int assetId = ensureAssetId(coinName);
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, newReq);
        Map<String, Object> modify = new LinkedHashMap<>();
        modify.put("oid", cloid == null ? null : cloid.getRaw());
        modify.put("order", wire);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "batchModify");
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
        // 保持 Signing.orderWiresToOrderAction 中的默认分组 "na"，不覆写

        if (builder != null && !builder.isEmpty()) {
            // 仅保留官方文档允许的字段：b（地址）与 f（费用）；其余键将被忽略，避免 422 反序列化失败
            Map<String, Object> filtered = new LinkedHashMap<>();
            if (builder.containsKey("b")) {
                Object bVal = builder.get("b");
                if (bVal instanceof String s) {
                    String addr = s.toLowerCase();
                    if (!ETH_ADDRESS_PATTERN.matcher(addr).matches()) {
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
     * 启用 Agent 侧 Dex Abstraction（与 Python exchange.agent_enable_dex_abstraction
     * 一致）。
     * 说明：
     * - 服务端会基于该动作创建/启用 API Wallet（Agent），以用于 L1 下单等操作。
     * - 此为 L1 动作，直接使用 signL1Action 进行签名与提交。
     *
     * @return JSON 响应
     */
    public JsonNode agentEnableDexAbstraction() {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "agentEnableDexAbstraction");
        // 直接复用 L1 发送逻辑
        return postAction(action);
    }

    /**
     * 用户侧 Dex Abstraction 开关（与 Python exchange.user_dex_abstraction 一致）。
     *
     * @param user    用户地址（0x 前缀）
     * @param enabled 是否启用
     * @return JSON 响应
     */
    public JsonNode userDexAbstraction(String user, boolean enabled) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "userDexAbstraction");
        action.put("user", user == null ? null : user.toLowerCase());
        action.put("enabled", enabled);
        action.put("nonce", nonce);

        // 构造与 Python 完全一致的 payloadTypes
        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "user", "type", "address"),
                Map.of("name", "enabled", "type", "bool"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:UserDexAbstraction",
                isMainnet());

        // 与 _post_action 一致进行发送（不重新进行 L1 签名）
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * 创建子账户。
     *
     * @param name 子账户名称
     * @return JSON 响应
     */
    public JsonNode createSubAccount(String name) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "createSubAccount");
        action.put("name", name);
        return postAction(action);
    }

    /**
     * 子账户资金划转。
     *
     * @param subAccountUser 子账户地址（0x 前缀）
     * @param isDeposit      true 表示存入；false 表示取出
     * @param usd            金额（微 USDC 单位）
     * @return JSON 响应
     */
    public JsonNode subAccountTransfer(String subAccountUser, boolean isDeposit, long usd) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "subAccountTransfer");
        action.put("subAccountUser", subAccountUser == null ? null : subAccountUser.toLowerCase());
        action.put("isDeposit", isDeposit);
        action.put("usd", usd);
        return postAction(action);
    }

    /**
     * USD 余额转账（用户签名）。
     *
     * @param amount      金额（USD，内部转换为微单位字符串）
     * @param destination 目标地址（0x 前缀）
     * @return JSON 响应
     */
    public JsonNode usdTransfer(double amount, String destination) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "usdSend");
        action.put("destination", destination);
        action.put("amount", String.valueOf(amount));
        action.put("time", time);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "destination", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "time", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:UsdSend",
                isMainnet());
        return postActionWithSignature(action, signature, time);
    }

    /**
     * 现货 Token 转账（用户签名）。
     *
     * @param amount      转账数量
     * @param destination 目标地址（0x 前缀）
     * @param token       Token 名称（如 "HL"）
     * @return JSON 响应
     */
    public JsonNode spotTransfer(double amount, String destination, String token) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotSend");
        action.put("destination", destination);
        action.put("token", token);
        action.put("amount", String.valueOf(amount));
        action.put("time", time);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "destination", "type", "string"),
                Map.of("name", "token", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "time", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:SpotSend",
                isMainnet());
        return postActionWithSignature(action, signature, time);
    }

    /**
     * 从桥合约提现（withdraw3，用户签名）。
     *
     * @param amount      金额（字符串）
     * @param destination 目标地址（0x 前缀）
     * @return JSON 响应
     */
    public JsonNode withdrawFromBridge(double amount, String destination) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "withdraw3");
        action.put("destination", destination);
        action.put("amount", String.valueOf(amount));
        action.put("time", time);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "destination", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "time", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:Withdraw",
                isMainnet());
        return postActionWithSignature(action, signature, time);
    }

    /**
     * USD 类目转移（Spot ⇄ Perp）。
     *
     * @param toPerp true 表示从 Spot 转至 Perp；false 表示从 Perp 转至 Spot
     * @param amount 金额（USD，内部按微单位转换）
     * @return JSON 响应
     */
    public JsonNode usdClassTransfer(boolean toPerp, double amount) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "usdClassTransfer");
        String strAmount = String.valueOf(amount);
        if (this.vaultAddress != null && !this.vaultAddress.isEmpty()) {
            strAmount = strAmount + " subaccount:" + this.vaultAddress;
        }
        action.put("amount", strAmount);
        action.put("toPerp", toPerp);
        action.put("nonce", nonce);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "toPerp", "type", "bool"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:UsdClassTransfer",
                isMainnet());
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * 资产跨 DEX 发送（sendAsset，用户签名）。
     *
     * @param destination    目标地址（0x 前缀）
     * @param sourceDex      源 DEX 名称
     * @param destinationDex 目标 DEX 名称
     * @param token          Token 名称
     * @param amount         数量（字符串）
     * @param fromSubAccount 源子账户地址（可选）
     * @return JSON 响应
     */
    public JsonNode sendAsset(String destination, String sourceDex, String destinationDex, String token, String amount,
                              String fromSubAccount) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "sendAsset");
        action.put("destination", destination);
        action.put("sourceDex", sourceDex);
        action.put("destinationDex", destinationDex);
        action.put("token", token);
        action.put("amount", amount);
        String from = fromSubAccount != null ? fromSubAccount : (this.vaultAddress != null ? this.vaultAddress : "");
        action.put("fromSubAccount", from);
        action.put("nonce", nonce);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "destination", "type", "string"),
                Map.of("name", "sourceDex", "type", "string"),
                Map.of("name", "destinationDex", "type", "string"),
                Map.of("name", "token", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "fromSubAccount", "type", "string"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:SendAsset",
                isMainnet());
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * 授权 Builder 费用率（用户签名）。
     *
     * @param builder    Builder 地址（0x 前缀）
     * @param maxFeeRate 允许的最大费用率（字符串小数）
     * @return JSON 响应
     */
    public JsonNode approveBuilderFee(String builder, String maxFeeRate) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "approveBuilderFee");
        action.put("builder", builder == null ? null : builder.toLowerCase());
        action.put("maxFeeRate", maxFeeRate);
        action.put("nonce", nonce);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "maxFeeRate", "type", "string"),
                Map.of("name", "builder", "type", "address"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:ApproveBuilderFee",
                isMainnet());
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * 绑定推荐码（用户签名）。
     *
     * @param code 推荐码字符串
     * @return JSON 响应
     */
    public JsonNode setReferrer(String code) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "setReferrer");
        action.put("code", code);
        action.put("nonce", nonce);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "code", "type", "string"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:SetReferrer",
                isMainnet());
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * Token 委托/取消委托（用户签名）。
     *
     * @param validator    验证者地址（0x 前缀）
     * @param wei          委托数量（Wei 单位）
     * @param isUndelegate true 表示取消委托；false 表示委托
     * @return JSON 响应
     */
    public JsonNode tokenDelegate(String validator, long wei, boolean isUndelegate) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "tokenDelegate");
        action.put("validator", validator == null ? null : validator.toLowerCase());
        action.put("wei", wei);
        action.put("isUndelegate", isUndelegate);
        action.put("nonce", nonce);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "validator", "type", "address"),
                Map.of("name", "wei", "type", "uint64"),
                Map.of("name", "isUndelegate", "type", "bool"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:TokenDelegate",
                isMainnet());
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * 转换为多签用户（用户签名）。
     *
     * @param signersJson 签名者配置 JSON 字符串
     * @return JSON 响应
     */
    public JsonNode convertToMultiSigUser(String signersJson) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "convertToMultiSigUser");
        action.put("signers", signersJson);
        action.put("nonce", nonce);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "signers", "type", "string"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:ConvertToMultiSigUser",
                isMainnet());
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * Vault 资金转移（存入/取出）
     *
     * @param vaultAddress Vault 地址
     * @param isDeposit    是否存入
     * @param usd          金额（微 USDC 单位）
     * @return JSON 响应
     */
    public JsonNode vaultTransfer(String vaultAddress, boolean isDeposit, long usd) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "vaultTransfer");
        action.put("vaultAddress", vaultAddress == null ? null : vaultAddress.toLowerCase());
        action.put("isDeposit", isDeposit);
        action.put("usd", usd);
        return postAction(action);
    }

    /**
     * SpotDeploy: 注册 Token（registerToken2）
     */
    public JsonNode spotDeployRegisterToken(String tokenName, int szDecimals, int weiDecimals, int maxGas,
                                            String fullName) {
        Map<String, Object> action = new LinkedHashMap<>();
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("name", tokenName);
        spec.put("szDecimals", szDecimals);
        spec.put("weiDecimals", weiDecimals);
        Map<String, Object> registerToken2 = new LinkedHashMap<>();
        registerToken2.put("spec", spec);
        registerToken2.put("maxGas", maxGas);
        registerToken2.put("fullName", fullName);
        action.put("type", "spotDeploy");
        action.put("registerToken2", registerToken2);
        return postAction(action);
    }

    /**
     * SpotDeploy: 用户创世分配（userGenesis）。
     *
     * @param token               Token ID
     * @param userAndWei          用户与 Wei 金额列表，形如 [[user,addressLower],[wei,string]]
     * @param existingTokenAndWei 既有 Token 与 Wei 金额列表，形如
     *                            [[tokenId,int],[wei,string]]
     * @return JSON 响应
     */
    public JsonNode spotDeployUserGenesis(int token, List<String[]> userAndWei, List<Object[]> existingTokenAndWei) {
        List<List<Object>> userAndWeiWire = new ArrayList<>();
        if (userAndWei != null) {
            for (String[] pair : userAndWei) {
                String user = pair[0] == null ? null : pair[0].toLowerCase();
                String wei = pair[1];
                List<Object> entry = new ArrayList<>();
                entry.add(user);
                entry.add(wei);
                userAndWeiWire.add(entry);
            }
        }
        List<List<Object>> existingWire = new ArrayList<>();
        if (existingTokenAndWei != null) {
            for (Object[] pair : existingTokenAndWei) {
                Integer t = (Integer) pair[0];
                String wei = (String) pair[1];
                List<Object> entry = new ArrayList<>();
                entry.add(t);
                entry.add(wei);
                existingWire.add(entry);
            }
        }

        Map<String, Object> userGenesis = new LinkedHashMap<>();
        userGenesis.put("token", token);
        userGenesis.put("userAndWei", userAndWeiWire);
        userGenesis.put("existingTokenAndWei", existingWire);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotDeploy");
        action.put("userGenesis", userGenesis);
        return postAction(action);
    }

    /**
     * SpotDeploy: 启用冻结权限。
     *
     * @param token Token ID
     * @return JSON 响应
     */
    public JsonNode spotDeployEnableFreezePrivilege(int token) {
        return spotDeployTokenActionInner("enableFreezePrivilege", token);
    }

    /**
     * SpotDeploy: 撤销冻结权限。
     *
     * @param token Token ID
     * @return JSON 响应
     */
    public JsonNode spotDeployRevokeFreezePrivilege(int token) {
        return spotDeployTokenActionInner("revokeFreezePrivilege", token);
    }

    /**
     * SpotDeploy: 冻结/解冻用户。
     *
     * @param token  Token ID
     * @param user   用户地址（0x 前缀）
     * @param freeze true 为冻结；false 为解冻
     * @return JSON 响应
     */
    public JsonNode spotDeployFreezeUser(int token, String user, boolean freeze) {
        Map<String, Object> freezeUser = new LinkedHashMap<>();
        freezeUser.put("token", token);
        freezeUser.put("user", user == null ? null : user.toLowerCase());
        freezeUser.put("freeze", freeze);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotDeploy");
        action.put("freezeUser", freezeUser);
        return postAction(action);
    }

    /**
     * SpotDeploy: 启用报价 Token。
     *
     * @param token Token ID
     * @return JSON 响应
     */
    public JsonNode spotDeployEnableQuoteToken(int token) {
        return spotDeployTokenActionInner("enableQuoteToken", token);
    }

    /**
     * SpotDeploy: 通用 Token 操作内部封装
     */
    private JsonNode spotDeployTokenActionInner(String variant, int token) {
        Map<String, Object> action = new LinkedHashMap<>();
        Map<String, Object> variantObj = new LinkedHashMap<>();
        variantObj.put("token", token);
        action.put("type", "spotDeploy");
        action.put(variant, variantObj);
        return postAction(action);
    }

    /**
     * SpotDeploy: 创世（genesis）。
     *
     * @param token            Token ID
     * @param maxSupply        最大供应量（字符串）
     * @param noHyperliquidity 是否不启用 Hyperliquidity
     * @return JSON 响应
     */
    public JsonNode spotDeployGenesis(int token, String maxSupply, boolean noHyperliquidity) {
        Map<String, Object> genesis = new LinkedHashMap<>();
        genesis.put("token", token);
        genesis.put("maxSupply", maxSupply);
        if (noHyperliquidity) {
            genesis.put("noHyperliquidity", true);
        }
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotDeploy");
        action.put("genesis", genesis);
        return postAction(action);
    }

    /**
     * SpotDeploy: 注册现货交易对（registerSpot）。
     *
     * @param baseToken  基础 Token ID
     * @param quoteToken 报价 Token ID
     * @return JSON 响应
     */
    public JsonNode spotDeployRegisterSpot(int baseToken, int quoteToken) {
        Map<String, Object> register = new LinkedHashMap<>();
        List<Integer> tokens = new ArrayList<>();
        tokens.add(baseToken);
        tokens.add(quoteToken);
        register.put("tokens", tokens);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotDeploy");
        action.put("registerSpot", register);
        return postAction(action);
    }

    /**
     * SpotDeploy: 注册 Hyperliquidity 做市。
     *
     * @param spot          现货交易对 ID
     * @param startPx       起始价格
     * @param orderSz       每档订单数量
     * @param nOrders       订单档数
     * @param nSeededLevels 预置档位数（可选）
     * @return JSON 响应
     */
    public JsonNode spotDeployRegisterHyperliquidity(int spot, double startPx, double orderSz, int nOrders,
                                                     Integer nSeededLevels) {
        Map<String, Object> register = new LinkedHashMap<>();
        register.put("spot", spot);
        register.put("startPx", String.valueOf(startPx));
        register.put("orderSz", String.valueOf(orderSz));
        register.put("nOrders", nOrders);
        if (nSeededLevels != null) {
            register.put("nSeededLevels", nSeededLevels);
        }
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotDeploy");
        action.put("registerHyperliquidity", register);
        return postAction(action);
    }

    /**
     * SpotDeploy: 设置部署者交易费分成。
     *
     * @param token Token ID
     * @param share 分成比例（字符串小数）
     * @return JSON 响应
     */
    public JsonNode spotDeploySetDeployerTradingFeeShare(int token, String share) {
        Map<String, Object> setShare = new LinkedHashMap<>();
        setShare.put("token", token);
        setShare.put("share", share);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotDeploy");
        action.put("setDeployerTradingFeeShare", setShare);
        return postAction(action);
    }

    /**
     * 用户授权并创建新的 Agent（API Wallet）。
     * 与 Python Exchange.approve_agent 一致：
     * - 随机生成 32 字节私钥，得到 agentAddress；
     * - 构造 {type:"approveAgent", agentAddress, agentName?, nonce} 用户签名动作；
     * - 使用 signUserSignedAction(primaryType="HyperliquidTransaction:ApproveAgent")
     * 签名；
     * - 发送到 /exchange 并返回服务端响应与新私钥。
     * <p>
     * 注意：当 name 为 null 时，不在 action 中包含 agentName 字段（与 Python 对齐）。
     *
     * @param name 可选的 Agent 名称（显示用途），可为 null
     * @return 服务端响应与生成的 Agent 私钥/地址
     */
    public ApproveAgentResult approveAgent(String name) {
        // 生成 32 字节随机私钥（0x 前缀）
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        String agentPrivateKey = "0x" + org.web3j.utils.Numeric.toHexStringNoPrefix(bytes);
        org.web3j.crypto.Credentials agentCred = org.web3j.crypto.Credentials.create(agentPrivateKey);
        String agentAddress = agentCred.getAddress();

        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "approveAgent");
        action.put("agentAddress", agentAddress);
        action.put("nonce", nonce);
        if (name != null) {
            action.put("agentName", name);
        }

        // ApproveAgent payload types
        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "agentAddress", "type", "address"),
                Map.of("name", "agentName", "type", "string"),
                Map.of("name", "nonce", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                wallet,
                action,
                payloadTypes,
                "HyperliquidTransaction:ApproveAgent",
                isMainnet());

        com.fasterxml.jackson.databind.JsonNode resp = postActionWithSignature(action, signature, nonce);
        return new ApproveAgentResult(resp, agentPrivateKey, agentAddress);
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
        if (effectiveVault != null) {
            effectiveVault = effectiveVault.toLowerCase();
            String signerAddr = wallet.getAddress().toLowerCase();
            if (effectiveVault.equals(signerAddr)) {
                effectiveVault = null;
            }
        }

        Long ea = expiresAfter;
        if (ea != null && ea < 1_000_000_000_000L) {
            ea = nonce + ea;
        }

        Map<String, Object> signature = Signing.signL1Action(
                wallet,
                action,
                effectiveVault,
                nonce,
                ea,
                isMainnet());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("nonce", nonce);
        payload.put("signature", signature);
        payload.put("vaultAddress", effectiveVault);
        payload.put("expiresAfter", ea);

        return hypeHttpClient.post("/exchange", payload);
    }

    /**
     * 统一封装：使用现成签名（用户签名或其他签名）发送到 /exchange。
     * 与 postAction 的区别在于：不在此方法内生成签名，而是接受外部传入的签名。
     *
     * @param action    动作 Map
     * @param signature 现成的 r/s/v 签名（与 EIP-712 TypedData 对应）
     * @param nonce     时间戳/随机数
     * @return JSON 响应
     */
    private JsonNode postActionWithSignature(Map<String, Object> action, Map<String, Object> signature, long nonce) {
        String type = String.valueOf(action.getOrDefault("type", ""));
        boolean userSigned = "approveAgent".equals(type)
                || "userDexAbstraction".equals(type)
                || "usdSend".equals(type)
                || "withdraw3".equals(type)
                || "spotSend".equals(type)
                || "usdClassTransfer".equals(type)
                || "sendAsset".equals(type)
                || "approveBuilderFee".equals(type)
                || "setReferrer".equals(type)
                || "tokenDelegate".equals(type)
                || "convertToMultiSigUser".equals(type);
        String effectiveVault = ("usdClassTransfer".equals(type) || "sendAsset".equals(type)) ? null : vaultAddress;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("nonce", nonce);
        payload.put("signature", signature);
        if (!userSigned) {
            payload.put("vaultAddress", effectiveVault);
            payload.put("expiresAfter", expiresAfter);
        } else {
            payload.put("vaultAddress", effectiveVault);
        }
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

    /**
     * 确保当前地址已启用 Dex Abstraction。
     *
     * <p>
     * 顺序尝试：
     * 1) 查询状态；
     * 2) userDexAbstraction(user, true) 用户签名开启；
     * 3) agentEnableDexAbstraction L1 开启。
     * 成功后写入本地缓存。
     * </p>
     */
    private void ensureDexAbstractionEnabled() {
        String address = wallet.getAddress().toLowerCase();
        Boolean cached = dexEnabledCache.getIfPresent(address);
        if (cached != null && cached)
            return;

        boolean enabled = false;
        try {
            JsonNode state = info.queryUserDexAbstractionState(address);
            enabled = isDexEnabled(state);
        } catch (Exception ignored) {
        }

        if (!enabled) {
            try {
                JsonNode userToggle = userDexAbstraction(address, true);
                enabled = isOk(userToggle) || isAlreadySet(userToggle);
            } catch (Exception ignored) {
            }
        }

        if (!enabled) {
            try {
                JsonNode resp = agentEnableDexAbstraction();
                enabled = isOk(resp) || isAlreadySet(resp);
            } catch (Exception ignored) {
            }
        }

        if (enabled) {
            dexEnabledCache.put(address, Boolean.TRUE);
        }
    }

    /**
     * 解析 Dex Abstraction 启用状态。
     *
     * @param node 状态 JSON
     * @return 已启用返回 true，否则 false
     */
    private boolean isDexEnabled(JsonNode node) {
        if (node == null)
            return false;
        if (node.has("enabled"))
            return node.get("enabled").asBoolean(false);
        if (node.has("data") && node.get("data").has("enabled"))
            return node.get("data").get("enabled").asBoolean(false);
        String s = node.toString().toLowerCase();
        return s.contains("\"enabled\":true");
    }

    /**
     * 判断响应是否 status=ok。
     *
     * @param node 响应 JSON
     * @return 是则 true，否则 false
     */
    private boolean isOk(JsonNode node) {
        return node != null && node.has("status") && "ok".equalsIgnoreCase(node.get("status").asText());
    }

    /**
     * 判断响应是否为“已设置”类错误（already set）。
     *
     * @param node 响应 JSON
     * @return 是则 true，否则 false
     */
    private boolean isAlreadySet(JsonNode node) {
        return node != null && node.has("status") && "err".equalsIgnoreCase(node.get("status").asText())
                && node.has("response") && node.get("response").isTextual()
                && node.get("response").asText().toLowerCase().contains("already set");
    }

    /**
     * 市价开仓占位转换：为 IOC 市价单计算占位限价。
     *
     * <p>
     * 当 limitPx 为空且 TIF=IOC 时：
     * - 取 {@code req.slippage} 或默认滑点配置；
     * - 使用 {@link #computeSlippagePrice(String, boolean, double)} 计算占位价并写回。
     * </p>
     *
     * @param req 下单请求
     */
    private void marketOpenTransition(OrderRequest req) {
        if (req == null)
            return;
        if (req.getLimitPx() == null && req.getOrderType() != null && req.getOrderType().getLimit() != null &&
                req.getOrderType().getLimit().getTif() == Tif.IOC) {
            double slip = req.getSlippage() != null ? req.getSlippage()
                    : defaultSlippageByCoin.getOrDefault(req.getCoin(), defaultSlippage);
            double slipPx = computeSlippagePrice(req.getCoin(), Boolean.TRUE.equals(req.getIsBuy()), slip);
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
            throw new HypeError("Failed to get mid price for coin " + coin
                    + " (allMids returned empty or does not contain the coin)");
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

        Integer szDecimals = szDecimalsCache.getIfPresent(coin);
        if (szDecimals == null) {
            Meta.Universe metaUniverse = info.getMetaUniverse(coin);
            szDecimals = metaUniverse.getSzDecimals();
            if (szDecimals == null) {
                throw new HypeError("Failed to get szDecimals from Meta.Universe, coin: " + coin);
            }
            szDecimalsCache.put(coin, szDecimals);
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
     * 规范化永续（PERP）触发价精度（triggerPx）。
     *
     * <p>
     * 规则与限价一致：先按 5 位有效数字，再按 (6 - szDecimals) 小数位。
     * </p>
     *
     * @param req 下单请求（仅当 instrumentType=PERP 且为触发单且 triggerPx 非空时生效）
     */
    private void normalizePerpTriggerPx(OrderRequest req) {
        if (req == null)
            return;
        if (req.getInstrumentType() != InstrumentType.PERP)
            return;
        if (req.getOrderType() == null || req.getOrderType().getTrigger() == null)
            return;
        Double px = req.getOrderType().getTrigger().getTriggerPx();
        if (px == null)
            return;
        String coin = req.getCoin();
        Integer szDecimals = szDecimalsCache.getIfPresent(coin);
        if (szDecimals == null) {
            Meta.Universe mu = info.getMetaUniverse(coin);
            szDecimals = mu.getSzDecimals();
            if (szDecimals != null)
                szDecimalsCache.put(coin, szDecimals);
        }
        if (szDecimals == null)
            return;
        int decimals = 6 - szDecimals;
        if (decimals < 0)
            decimals = 0;
        BigDecimal bd = BigDecimal.valueOf(px).round(new MathContext(5, RoundingMode.HALF_UP)).setScale(decimals,
                RoundingMode.HALF_UP);
        double newPx = bd.doubleValue();
        TriggerOrderType oldTrig = req.getOrderType().getTrigger();
        TriggerOrderType newTrig = new TriggerOrderType(newPx, oldTrig.isMarket(), oldTrig.getTpslEnum());
        LimitOrderType oldLimit = req.getOrderType().getLimit();
        req.setOrderType(new OrderType(oldLimit, newTrig));
    }

    /**
     * 设置全局默认滑点比例。
     *
     * @param slippage 滑点比例（例如 0.05 表示 5%）
     */
    public void setDefaultSlippage(double slippage) {
        this.defaultSlippage = slippage;
    }

    /**
     * 为指定币种设置默认滑点比例（覆盖全局）。
     *
     * @param coin     币种名称
     * @param slippage 滑点比例
     */
    public void setDefaultSlippage(String coin, double slippage) {
        if (coin != null)
            this.defaultSlippageByCoin.put(coin, slippage);
    }

    /**
     * 一键市价全量平仓（根据账户当前仓位自动推断方向与数量）。
     *
     * @param coin 币种名称
     * @return 服务端订单响应
     * @throws HypeError 当无仓位可平时抛出
     */
    public Order closePositionAtMarketAll(String coin) {
        return order(OrderRequest.Close.positionAtMarketAll(coin));
    }

    /**
     * 一键限价全量平仓（根据账户当前仓位自动推断方向与数量）。
     *
     * @param tif     TIF 策略
     * @param coin    币种名称
     * @param limitPx 限价价格
     * @param cloid   客户端订单 ID（可为 null）
     * @return 服务端订单响应
     * @throws HypeError 当无仓位可平时抛出
     */
    public Order closePositionLimitAll(Tif tif, String coin, double limitPx, Cloid cloid) {
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError("No position to close for coin " + coin);
        }
        boolean isBuy = szi < 0.0;
        double absSz = Math.abs(szi);
        OrderRequest req = OrderRequest.Close.limit(tif, coin, szi, limitPx, cloid);
        req.setSz(absSz);
        req.setIsBuy(isBuy);
        return order(req);
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
