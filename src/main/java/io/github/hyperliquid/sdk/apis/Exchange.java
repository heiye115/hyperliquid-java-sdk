package io.github.hyperliquid.sdk.apis;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.model.approve.ApproveAgentResult;
import io.github.hyperliquid.sdk.model.info.ClearinghouseState;
import io.github.hyperliquid.sdk.model.info.UpdateLeverage;
import io.github.hyperliquid.sdk.model.order.*;
import io.github.hyperliquid.sdk.model.wallet.ApiWallet;
import io.github.hyperliquid.sdk.utils.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExchangeClient 客户端，负责下单、撤单、转账等 L1/L2 操作。
 * 当前版本实现核心下单与批量下单，其他 L1 操作将在后续补充。
 */
public class Exchange {

    /**
     * 用户API钱包
     */
    private final ApiWallet apiWallet;

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
    private String vaultAddress;

    /**
     * 获取 vault 地址
     *
     * @return vault 地址
     */
    public String getVaultAddress() {
        return vaultAddress;
    }

    /**
     * 设置 vault 地址
     *
     * @param vaultAddress vault 地址
     */
    public void setVaultAddress(String vaultAddress) {
        this.vaultAddress = vaultAddress;
    }

    /**
     * 默认滑点，用于计算滑点价格（字符串）
     */
    private final Map<String, String> defaultSlippageByCoin = new ConcurrentHashMap<>();

    /**
     * 默认滑点，用于计算滑点价格（字符串，例如 "0.05" 表示 5%）
     */
    private String defaultSlippage = "0.05";

    /**
     * 构造 Exchange 客户端。
     *
     * @param hypeHttpClient HTTP 客户端实例
     * @param wallet         用户钱包凭证
     * @param info           Info 客户端实例
     */
    public Exchange(HypeHttpClient hypeHttpClient, ApiWallet wallet, Info info) {
        this.hypeHttpClient = hypeHttpClient;
        this.apiWallet = wallet;
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
     * @return UpdateLeverage
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
        OrderRequest effective = prepareRequest(req);
        // 格式化订单数量精度
        formatOrderSize(effective);
        marketOpenTransition(effective);
        // 格式化订单价格精度
        formatOrderPrice(effective);
        int assetId = ensureAssetId(effective.getCoin());
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, effective);
        Map<String, Object> action = buildOrderAction(List.of(wire), builder);
        // 获取订单的 expiresAfter，默认 120 秒
        Long expiresAfter = effective.getExpiresAfter() != null ? effective.getExpiresAfter() : 120_000L;
        JsonNode node = postAction(action, expiresAfter);
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
     * 根据资产精度格式化订单数量
     */
    private void formatOrderSize(OrderRequest req) {
        if (req == null || req.getSz() == null || req.getSz().isEmpty()) return;
        // 优化：直接从缓存获取 szDecimals，避免每次都获取完整 Universe 对象
        Integer szDecimals = info.getSzDecimals(req.getCoin());
        if (szDecimals == null) return;
        try {
            // 使用 BigDecimal 按精度四舍五入，向下取整更安全
            BigDecimal bd = new BigDecimal(req.getSz()).setScale(szDecimals, RoundingMode.DOWN);
            req.setSz(bd.toPlainString());
        } catch (NumberFormatException e) {
            throw new HypeError("Invalid order size format: " + req.getSz() + ". Must be a valid number.");
        }
    }

    /**
     * 根据资产精度格式化订单价格（限价与触发价）
     * <p>
     * 规则与 Python SDK 对齐：
     * 1. 先按 5 位有效数字四舍五入
     * 2. 再按小数位四舍五入（永续：6-szDecimals；现货：8-szDecimals）
     * </p>
     */
    private void formatOrderPrice(OrderRequest req) {
        if (req == null) return;
        // 优化：直接从缓存获取 szDecimals，避免每次都获取完整 Universe 对象
        Integer szDecimals = info.getSzDecimals(req.getCoin());
        if (szDecimals == null) return;
        boolean isSpot = req.getInstrumentType() == InstrumentType.SPOT;

        // 计算小数位：现货 8-szDecimals，永续 6-szDecimals
        int decimals = (isSpot ? 8 : 6) - szDecimals;
        if (decimals < 0) {
            decimals = 0;
        }

        // 1. 格式化限价（limitPx）
        if (req.getLimitPx() != null && !req.getLimitPx().isEmpty()) {
            try {
                BigDecimal bd = new BigDecimal(req.getLimitPx()).round(new MathContext(5, RoundingMode.HALF_UP)).setScale(decimals, RoundingMode.HALF_UP);
                req.setLimitPx(bd.stripTrailingZeros().toPlainString());
            } catch (NumberFormatException e) {
                throw new HypeError("Invalid limit price format: " + req.getLimitPx() + ". Must be a valid number.");
            }
        }

        // 2. 格式化触发价（triggerPx）
        if (req.getOrderType() != null && req.getOrderType().getTrigger() != null) {
            String triggerPx = req.getOrderType().getTrigger().getTriggerPx();
            if (triggerPx != null && !triggerPx.isEmpty()) {
                try {
                    BigDecimal bd = new BigDecimal(triggerPx).round(new MathContext(5, RoundingMode.HALF_UP)).setScale(decimals, RoundingMode.HALF_UP);
                    String newPx = bd.stripTrailingZeros().toPlainString();
                    TriggerOrderType oldTrig = req.getOrderType().getTrigger();
                    TriggerOrderType newTrig = new TriggerOrderType(newPx, oldTrig.isMarket(), oldTrig.getTpslEnum());
                    LimitOrderType oldLimit = req.getOrderType().getLimit();
                    req.setOrderType(new OrderType(oldLimit, newTrig));
                } catch (NumberFormatException e) {
                    throw new HypeError("Invalid trigger price format: " + triggerPx + ". Must be a valid number.");
                }
            }
        }
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
            String sz = (req.getSz() != null && !req.getSz().isEmpty()) ? req.getSz() : String.valueOf(Math.abs(szi));
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
     * 推断当前账户在指定币种的"签名仓位尺寸"。
     *
     * <p>
     * 正数表示多仓，负数表示空仓；当无仓位或解析失败时返回 0.0。
     * </p>
     *
     * @param coin 币种名称
     * @return 签名尺寸（double）
     */
    private double inferSignedPosition(String coin) {
        ClearinghouseState state = info.userState(apiWallet.getPrimaryWalletAddress().toLowerCase());
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
     * 为 positionTpsl 订单组自动推断并填充仓位方向和数量。
     * <p>
     * 当订单中的 isBuy 或 sz 为 null 时：
     * - 自动查询账户持仓
     * - 根据 szi（签名仓位尺寸）推断方向和数量
     * - 填充所有订单的方向和数量
     * </p>
     *
     * @param orders positionTpsl 订单列表（同一币种）
     * @throws HypeError 当无持仓时抛出
     */
    private void inferAndFillPositionTpslOrders(List<OrderRequest> orders) {
        // 获取第一个订单的币种（positionTpsl 所有订单应该是同一个币种）
        OrderRequest firstOrder = orders.getFirst();
        String coin = firstOrder.getCoin();

        // 检查是否需要自动推断（isBuy 或 sz 为 null）
        boolean needsInference = firstOrder.getIsBuy() == null || firstOrder.getSz() == null;

        if (!needsInference) {
            return;
        }

        // 自动查询仓位并推断
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError("No position found for " + coin + ". Cannot auto-infer direction and size for positionTpsl.");
        }

        // 推断方向和数量
        boolean isBuy = szi > 0; // 多仓需要卖出平仓，所以 isBuy=true 表示多仓
        String sz = String.valueOf(Math.abs(szi));

        // 填充所有订单的方向和数量
        for (OrderRequest order : orders) {
            if (order.getIsBuy() == null) {
                // 对于止盈止损订单，需要反向
                if (order.getReduceOnly() != null && order.getReduceOnly()) {
                    order.setIsBuy(!isBuy);  // 反向平仓
                } else {
                    order.setIsBuy(isBuy);
                }
            }
            if (order.getSz() == null) {
                order.setSz(sz);
            }
        }
    }


    /**
     * 更新隔离保证金
     *
     * @param amount   金额（USD，字符串，内部按微单位转换）
     * @param coinName 币种名
     * @return JSON 响应
     */
    public JsonNode updateIsolatedMargin(String amount, String coinName) {
        int assetId = ensureAssetId(coinName);
        try {
            long ntli = Signing.floatToUsdInt(Double.parseDouble(amount));
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "updateIsolatedMargin");
            action.put("asset", assetId);
            action.put("isBuy", true);
            action.put("ntli", ntli);
            return postAction(action);
        } catch (NumberFormatException e) {
            throw new HypeError("Invalid amount format: " + amount + ". Must be a valid number.");
        }
    }


    /**
     * 批量下单（支持 grouping 分组）。
     *
     * @param requests 下单请求列表
     * @param builder  可选 builder
     * @param grouping 分组类型："na" | "normalTpsl" | "positionTpsl"
     *                 1. "na" - 普通订单（默认值）
     *                 使用场景：
     *                 ✅ 单笔普通订单（开仓、平仓、限价、市价等）
     *                 ✅ 批量下单但订单之间无关联
     *                 ✅ 不需要 TP/SL 的任何订单
     *                 2. "normalTpsl" - 普通止盈止损组
     *                 使用场景：
     *                 ✅ 同时开仓并设置 TP/SL
     *                 ✅ 批量下单：1个开仓订单 + 1个或2个止盈止损订单
     *                 3. "positionTpsl" - 仓位止盈止损组
     *                 使用场景：
     *                 ✅ 针对已有仓位设置或修改 TP/SL
     *                 ✅ 不开新仓，只设置现有仓位的保护
     * @return 响应 JSON
     */
    public JsonNode bulkOrders(List<OrderRequest> requests, Map<String, Object> builder, String grouping) {
        // 格式化订单数量精度
        requests.forEach(this::formatOrderSize);
        //处理市价单
        requests.forEach(this::marketOpenTransition);
        // 格式化订单价格精度
        requests.forEach(this::formatOrderPrice);
        List<OrderWire> wires = new ArrayList<>();
        for (OrderRequest r : requests) {
            int assetId = ensureAssetId(r.getCoin());
            wires.add(Signing.orderRequestToOrderWire(assetId, r));
        }
        Map<String, Object> action = buildOrderAction(wires, builder);
        if (grouping != null && !grouping.isEmpty()) {
            action.put("grouping", grouping);
        }
        return postAction(action);
    }


    /**
     * 批量下单（支持 OrderGroup 自动推断 grouping）。
     * <p>
     * 通过 OrderGroup 自动识别分组类型，无需手动指定 grouping 参数。
     * <p>
     * 使用示例：
     * <pre>
     * // 自动推断 grouping="normalTpsl"
     * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
     *     .perp("ETH")
     *     .buy(0.1)
     *     .entryPrice(3500.0)
     *     .takeProfit(3600.0)
     *     .stopLoss(3400.0)
     *     .buildNormalTpsl();
     * JsonNode result = exchange.bulkOrders(orderGroup);
     *
     * // 自动推断 grouping="positionTpsl"
     * OrderGroup orderGroup2 = OrderRequest.entryWithTpSl()
     *     .perp("ETH")
     *     .closePosition(0.5, true)
     *     .takeProfit(3600.0)
     *     .buildPositionTpsl();
     * JsonNode result2 = exchange.bulkOrders(orderGroup2);
     * </pre>
     *
     * @param orderGroup 订单组（包含订单列表和分组类型）
     * @return 响应 JSON
     */
    public JsonNode bulkOrders(OrderGroup orderGroup) {
        return bulkOrders(orderGroup, null);
    }

    /**
     * 批量下单（支持 OrderGroup 和 builder）。
     * <p>
     * 对于 positionTpsl 类型的订单组，如果订单中的 isBuy 或 sz 为 null，
     * 会自动查询账户持仓并填充方向和数量。
     *
     * @param orderGroup 订单组（包含订单列表和分组类型）
     * @param builder    可选 builder
     * @return 响应 JSON
     */
    public JsonNode bulkOrders(OrderGroup orderGroup, Map<String, Object> builder) {
        List<OrderRequest> orders = orderGroup.getOrders();
        if (orders == null || orders.isEmpty()) {
            throw new HypeError("No orders found in OrderGroup.");
        }
        // 对于 positionTpsl，检查是否需要自动推断仓位
        if (GroupingType.POSITION_TPSL == orderGroup.getGroupingType()) {
            inferAndFillPositionTpslOrders(orders);
        }
        return bulkOrders(orders, builder, orderGroup.getGroupingType().getValue());
    }

    /**
     * 批量下单（普通订单，默认 grouping="na"）。
     * <p>
     * 用于批量提交多个普通订单，订单之间无关联关系。
     * <p>
     * 使用示例：
     * <pre>
     * // 批量下多个币种的订单
     * List<OrderRequest> orders = Arrays.asList(
     *     OrderRequest.builder().perp("BTC").buy(0.01).limitPrice(95000.0).build(),
     *     OrderRequest.builder().perp("ETH").buy(0.1).limitPrice(3500.0).build()
     * );
     * JsonNode result = exchange.bulkOrders(orders);
     * </pre>
     *
     * @param requests 订单列表
     * @return 响应 JSON
     */
    public JsonNode bulkOrders(List<OrderRequest> requests) {
        return bulkOrders(requests, null, null);
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
     * 批量修改订单（与 Python bulk_modify_orders_new 对齐）。
     * <p>
     * 使用示例：
     * <pre>
     * // 修改多个订单
     * List<ModifyRequest> modifies = Arrays.asList(
     *     ModifyRequest.byOid("ETH", 123456L, newReq1),
     *     ModifyRequest.byCloid("BTC", cloid, newReq2)
     * );
     * JsonNode result = exchange.bulkModifyOrders(modifies);
     * </pre>
     *
     * @param modifyRequests 批量修改请求列表
     * @return 响应 JSON
     */
    public JsonNode bulkModifyOrders(List<ModifyRequest> modifyRequests) {
        if (modifyRequests == null || modifyRequests.isEmpty()) {
            throw new HypeError("Modify requests cannot be empty");
        }

        List<Map<String, Object>> modifies = new ArrayList<>();
        for (ModifyRequest mr : modifyRequests) {
            int assetId = ensureAssetId(mr.getCoinName());
            OrderWire wire = Signing.orderRequestToOrderWire(assetId, mr.getNewOrder());
            Map<String, Object> modify = new LinkedHashMap<>();

            // 支持 OID 或 Cloid
            if (mr.getOid() != null) {
                modify.put("oid", mr.getOid());
            } else if (mr.getCloid() != null) {
                modify.put("oid", mr.getCloid().getRaw());
            } else {
                throw new HypeError("Either oid or cloid must be provided for modify request");
            }

            modify.put("order", wire);
            modifies.add(modify);
        }

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "batchModify");
        action.put("modifies", modifies);
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
            Map<String, Object> filtered = validateAndFilterBuilder(builder);
            if (!filtered.isEmpty()) {
                action.put("builder", filtered);
            }
        }
        return action;
    }

    /**
     * 验证并过滤 builder 参数。
     * <p>
     * 仅保留官方文档允许的字段：
     * - b（地址）：Builder 地址
     * - f（费用）：Builder 费用（非负整数）
     * 其余键将被忽略，避免 422 反序列化失败。
     * </p>
     *
     * @param builder 原始 builder 参数
     * @return 过滤后的 builder 参数
     * @throws HypeError 当参数验证失败时抛出
     */
    private Map<String, Object> validateAndFilterBuilder(Map<String, Object> builder) {
        Map<String, Object> filtered = new LinkedHashMap<>();

        // 验证并过滤地址字段 b
        if (builder.containsKey("b")) {
            Object bVal = builder.get("b");
            if (bVal instanceof String s) {
                filtered.put("b", s.toLowerCase());
            }
        }

        // 验证并过滤费用字段 f
        if (builder.containsKey("f")) {
            Object fVal = builder.get("f");
            if (!(fVal instanceof Number)) {
                throw new HypeError("builder.f must be a non-negative integer (numeric type)");
            }
            long f = ((Number) fVal).longValue();
            if (f < 0) {
                throw new HypeError("builder.f cannot be negative");
            }
            // 限制一个合理上限，避免误传超大数导致后端拒绝（可根据业务调整）
            if (f > 1_000_000L) {
                throw new HypeError("builder.f is too large, please verify the unit and value range");
            }
            filtered.put("f", f);
        }

        return filtered;
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
                apiWallet.getCredentials(),
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
     * @param amount      金额（字符串）
     * @param destination 目标地址（0x 前缀）
     * @return JSON 响应
     */
    public JsonNode usdTransfer(String amount, String destination) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "usdSend");
        action.put("destination", destination);
        action.put("amount", amount);  // 直接使用字符串
        action.put("time", time);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "destination", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "time", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                apiWallet.getCredentials(),
                action,
                payloadTypes,
                "HyperliquidTransaction:UsdSend",
                isMainnet());
        return postActionWithSignature(action, signature, time);
    }

    /**
     * 现货 Token 转账（用户签名）。
     *
     * @param amount      转账数量（字符串）
     * @param destination 目标地址（0x 前缀）
     * @param token       Token 名称（如 "HL"）
     * @return JSON 响应
     */
    public JsonNode spotTransfer(String amount, String destination, String token) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotSend");
        action.put("destination", destination);
        action.put("token", token);
        action.put("amount", amount);  // 直接使用字符串
        action.put("time", time);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "destination", "type", "string"),
                Map.of("name", "token", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "time", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                apiWallet.getCredentials(),
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
    public JsonNode withdrawFromBridge(String amount, String destination) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "withdraw3");
        action.put("destination", destination);
        action.put("amount", amount);  // 直接使用字符串
        action.put("time", time);

        List<Map<String, Object>> payloadTypes = List.of(
                Map.of("name", "hyperliquidChain", "type", "string"),
                Map.of("name", "destination", "type", "string"),
                Map.of("name", "amount", "type", "string"),
                Map.of("name", "time", "type", "uint64"));

        Map<String, Object> signature = Signing.signUserSignedAction(
                apiWallet.getCredentials(),
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
     * @param amount 金额（字符串）
     * @return JSON 响应
     */
    public JsonNode usdClassTransfer(boolean toPerp, String amount) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "usdClassTransfer");
        String strAmount = amount;  // 已经是字符串
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
                apiWallet.getCredentials(),
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
                apiWallet.getCredentials(),
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
                apiWallet.getCredentials(),
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
                apiWallet.getCredentials(),
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
                apiWallet.getCredentials(),
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
                apiWallet.getCredentials(),
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
                apiWallet.getCredentials(),
                action,
                payloadTypes,
                "HyperliquidTransaction:ApproveAgent",
                isMainnet());

        JsonNode resp = postActionWithSignature(action, signature, nonce);
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
        return postAction(action, null);
    }

    /**
     * 发送 L1 动作并签名（支持自定义过期时间）。
     * <p>
     * 规则：
     * - nonce 使用毫秒时间戳（与 Python get_timestamp_ms 一致）；
     * - usdClassTransfer/sendAsset 类型动作不附带 vaultAddress（保持 Python 行为一致）；
     * - 其它动作使用已设置的 vaultAddress；
     * - 使用 Signing.signL1Action 完成 TypedData 构造与签名。
     *
     * @param action       L1 动作（Map）
     * @param expiresAfter 订单过期时间（毫秒），为 null 时使用默认值 120000ms
     * @return JSON 响应
     */
    public JsonNode postAction(Map<String, Object> action, Long expiresAfter) {
        long nonce = Signing.getTimestampMs();
        String type = String.valueOf(action.getOrDefault("type", ""));
        String effectiveVault = calculateEffectiveVaultAddress(type);

        // 默认 120 秒
        if (expiresAfter == null) {
            expiresAfter = 120_000L;
        }
        long ea = expiresAfter;
        if (ea < 1_000_000_000_000L) {
            ea = nonce + ea;
        }

        Map<String, Object> signature = Signing.signL1Action(
                apiWallet.getCredentials(),
                action,
                effectiveVault,
                nonce,
                ea,
                isMainnet());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("nonce", nonce);
        payload.put("signature", signature);
        if (effectiveVault != null) {
            payload.put("vaultAddress", effectiveVault);
        }
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
        boolean userSigned = isUserSignedAction(type);
        String effectiveVault = calculateEffectiveVaultAddress(type);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("nonce", nonce);
        payload.put("signature", signature);
        if (!userSigned) {
            payload.put("vaultAddress", effectiveVault);
            // userSigned 动作不需要 expiresAfter
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
     * 计算有效的 vault 地址。
     * <p>
     * 规则：
     * - usdClassTransfer 和 sendAsset 类型不使用 vaultAddress
     * - 如果 vaultAddress 与签名者地址相同，返回 null
     * - 否则返回小写化的 vaultAddress
     * </p>
     *
     * @param actionType 动作类型
     * @return 有效的 vault 地址，或 null
     */
    private String calculateEffectiveVaultAddress(String actionType) {
        // usdClassTransfer 和 sendAsset 不使用 vaultAddress
        if ("usdClassTransfer".equals(actionType) || "sendAsset".equals(actionType)) {
            return null;
        }
        if (vaultAddress == null) {
            return null;
        }
        String effectiveVault = vaultAddress.toLowerCase();
        String signerAddr = apiWallet.getPrimaryWalletAddress().toLowerCase();

        // 如果 vault 地址与签名者地址相同，返回 null
        if (effectiveVault.equals(signerAddr)) {
            return null;
        }
        return effectiveVault;
    }

    /**
     * 判断动作是否为用户签名类型。
     * <p>
     * 用户签名动作使用 EIP-712 TypedData 签名，区别于 L1 动作签名。
     * </p>
     *
     * @param actionType 动作类型
     * @return 是用户签名动作返回 true，否则返回 false
     */
    private boolean isUserSignedAction(String actionType) {
        return "approveAgent".equals(actionType)
                || "userDexAbstraction".equals(actionType)
                || "usdSend".equals(actionType)
                || "withdraw3".equals(actionType)
                || "spotSend".equals(actionType)
                || "usdClassTransfer".equals(actionType)
                || "sendAsset".equals(actionType)
                || "approveBuilderFee".equals(actionType)
                || "setReferrer".equals(actionType)
                || "tokenDelegate".equals(actionType)
                || "convertToMultiSigUser".equals(actionType);
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
        if (req == null) return;
        if (req.getLimitPx() == null &&
                req.getOrderType() != null &&
                req.getOrderType().getLimit() != null &&
                req.getOrderType().getLimit().getTif() == Tif.IOC) {
            String slip = req.getSlippage() != null ? req.getSlippage() : defaultSlippageByCoin.getOrDefault(req.getCoin(), defaultSlippage);
            String slipPx = computeSlippagePrice(req.getCoin(), Boolean.TRUE.equals(req.getIsBuy()), slip);
            req.setLimitPx(slipPx);
        }
    }

    /**
     * 计算带滑点的价格（字符串版本）
     */
    public String computeSlippagePrice(String coin, boolean isBuy, String slippage) {
        Map<String, String> mids = info.allMids();
        String midStr = mids.get(coin);
        if (midStr == null) {
            throw new HypeError("Failed to get mid price for coin " + coin + " (allMids returned empty or does not contain the coin)");
        }
        try {
            double basePx = Double.parseDouble(midStr);
            double slippageVal = Double.parseDouble(slippage);
            double resultPx = basePx * (isBuy ? (1.0 + slippageVal) : (1.0 - slippageVal));
            return String.valueOf(resultPx);
        } catch (NumberFormatException e) {
            throw new HypeError("Invalid number format. midPrice: " + midStr + ", slippage: " + slippage);
        }
    }

    /**
     * 设置全局默认滑点比例。
     *
     * @param slippage 滑点比例（字符串，例如 "0.05" 表示 5%）
     */
    public void setDefaultSlippage(String slippage) {
        this.defaultSlippage = slippage;
    }

    /**
     * 为指定币种设置默认滑点比例（覆盖全局）。
     *
     * @param coin     币种名称
     * @param slippage 滑点比例（字符串）
     */
    public void setDefaultSlippage(String coin, String slippage) {
        if (coin != null)
            this.defaultSlippageByCoin.put(coin, slippage);
    }

    /**
     * 市价全量平仓指定币种（根据账户当前仓位自动推断方向与数量）。
     *
     * @param coin 币种名称
     * @return 服务端订单响应
     * @throws HypeError 当无仓位可平时抛出
     */
    public Order closePositionMarket(String coin) {
        return order(OrderRequest.Close.marketAll(coin));
    }

    /**
     * 市价平仓指定币种（支持部分平仓与自定义滑点）。
     * <p>
     * 自动查询账户仓位，推断平仓方向（多仓卖出/空仓买入），并按市价平仓。
     * <p>
     * 使用示例：
     * <pre>
     * // 完全平仓
     * Order result = exchange.closePositionMarket("ETH", null, null, null);
     *
     * // 部分平仓
     * Order result = exchange.closePositionMarket("ETH", 0.5, null, null);
     *
     * // 自定义滑点
     * Order result = exchange.closePositionMarket("ETH", null, 0.1, null);
     * </pre>
     *
     * @param coin     币种名称
     * @param sz       平仓数量（可为 null，默认全平）
     * @param slippage 滑点比例（可为 null，默认 0.05）
     * @param cloid    客户端订单 ID（可为 null）
     * @return 订单响应
     * @throws HypeError 当无仓位可平时抛出
     */
    public Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid) {
        // 查询当前仓位
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError("No position to close for coin " + coin);
        }

        // 推断平仓方向：持有多仓则卖出，持有空仓则买入
        boolean isBuy = szi < 0;

        // 确定平仓数量
        String closeSz = (sz != null && !sz.isEmpty()) ? sz : String.valueOf(Math.abs(szi));

        // 构建市价平仓请求
        OrderRequest req = OrderRequest.Close.market(coin, isBuy, closeSz, cloid);

        // 设置滑点（如果提供）
        if (slippage != null && !slippage.isEmpty()) {
            req.setSlippage(slippage);
        }

        return order(req);
    }

    /**
     * 市价平仓指定币种（带 builder 支持）。
     *
     * @param coin     币种名称
     * @param sz       平仓数量（可为 null）
     * @param slippage 滑点比例（可为 null）
     * @param cloid    客户端订单 ID（可为 null）
     * @param builder  builder 信息（可为 null）
     * @return 订单响应
     */
    public Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid, Map<String, Object> builder) {
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError("No position to close for coin " + coin);
        }

        boolean isBuy = szi < 0;
        String closeSz = (sz != null && !sz.isEmpty()) ? sz : String.valueOf(Math.abs(szi));
        OrderRequest req = OrderRequest.Close.market(coin, isBuy, closeSz, cloid);

        if (slippage != null && !slippage.isEmpty()) {
            req.setSlippage(slippage);
        }

        return order(req, builder);
    }

    /**
     * 限价全量平仓指定币种（根据账户当前仓位自动推断方向与数量）。
     *
     * @param tif     TIF 策略
     * @param coin    币种名称
     * @param limitPx 限价价格
     * @param cloid   客户端订单 ID（可为 null）
     * @return 服务端订单响应
     * @throws HypeError 当无仓位可平时抛出
     */
    public Order closePositionLimit(Tif tif, String coin, String limitPx, Cloid cloid) {
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError("No position to close for coin " + coin);
        }
        OrderRequest req = OrderRequest.Close.limit(tif, coin, String.valueOf(Math.abs(szi)), limitPx, cloid);
        return order(req);
    }


    /**
     * 市价平掉所有币种的全部持仓（自动推断多空方向）。
     * <p>
     * 查询账户所有持仓，自动推断每个币种的平仓方向和数量，批量下单一次性平仓。
     * 支持同时平掉多个币种的多空仓位。
     * </p>
     * <p>
     * 使用示例：
     * <pre>
     * // 一键平掉所有持仓
     * JsonNode result = exchange.closeAllPositions();
     * System.out.println("平仓结果: " + result);
     * </pre>
     *
     * @return 批量订单响应 JSON
     * @throws HypeError 当没有任何持仓时抛出
     */
    public JsonNode closeAllPositions() {
        // 查询当前账户所有仓位
        ClearinghouseState state = info.userState(apiWallet.getPrimaryWalletAddress().toLowerCase());
        if (state == null || state.getAssetPositions() == null || state.getAssetPositions().isEmpty()) {
            throw new HypeError("No positions to close");
        }

        // 构建所有平仓订单
        List<OrderRequest> closeOrders = new ArrayList<>();
        for (ClearinghouseState.AssetPositions ap : state.getAssetPositions()) {
            ClearinghouseState.Position pos = ap.getPosition();
            if (pos == null || pos.getCoin() == null || pos.getSzi() == null) {
                continue;
            }

            double szi;
            try {
                szi = Double.parseDouble(pos.getSzi());
            } catch (Exception e) {
                continue; // 解析失败跳过
            }

            // 跳过没有仓位的币种
            if (szi == 0.0) {
                continue;
            }

            // 推断平仓方向：持有多仓则卖出，持有空仓则买入
            boolean isBuy = szi < 0;
            double closeSz = Math.abs(szi);

            // 构建市价平仓请求
            OrderRequest req = OrderRequest.Close.market(pos.getCoin(), isBuy, String.valueOf(closeSz), null);
            closeOrders.add(req);
        }

        // 检查是否有需要平仓的订单
        if (closeOrders.isEmpty()) {
            throw new HypeError("No positions to close (all positions are zero)");
        }

        // 批量下单平仓
        return bulkOrders(closeOrders);
    }

    /**
     * 判断当前是否主网。
     *
     * @return 主网返回 true，否则 false
     */
    private boolean isMainnet() {
        return Constants.MAINNET_API_URL.equals(hypeHttpClient.getBaseUrl());
    }

    // ==================== Spot 子账户转账（Sub Account Spot Transfer） ====================

    /**
     * Spot 子账户转账（与 Python SDK 的 sub_account_spot_transfer 对齐）。
     * <p>
     * 用于在主账户与 Spot 子账户之间转移现货代币。
     * </p>
     *
     * @param subAccountUser 子账户用户地址（42 位十六进制格式）
     * @param isDeposit      true 表示从主账户转入子账户，false 表示从子账户转回主账户
     * @param token          代币名称（例如 "USDC"、"ETH" 等）
     * @param amount         转账数量（字符串格式）
     * @return JSON 响应
     */
    public JsonNode subAccountSpotTransfer(String subAccountUser, boolean isDeposit, String token, String amount) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "subAccountSpotTransfer");
        action.put("subAccountUser", subAccountUser == null ? null : subAccountUser.toLowerCase());
        action.put("isDeposit", isDeposit);
        action.put("token", token);
        action.put("amount", amount);  // 直接使用字符串

        return postAction(action);
    }

    // ==================== 多签操作（Multi-Sig） ====================

    /**
     * 多签操作（与 Python SDK 的 multi_sig 对齐）。
     * <p>
     * 用于多签账户执行操作，需要多个签名者的签名。
     * </p>
     *
     * @param multiSigUser 多签账户地址（42 位十六进制格式）
     * @param innerAction  内部动作（实际要执行的操作）
     * @param signatures   所有签名者的签名列表（按地址排序）
     * @param nonce        随机数/时间戳
     * @param vaultAddress vault 地址（可为 null）
     * @return JSON 响应
     */
    public JsonNode multiSig(
            String multiSigUser,
            Map<String, Object> innerAction,
            List<Map<String, Object>> signatures,
            long nonce,
            String vaultAddress
    ) {
        // 构造 multiSig action
        Map<String, Object> multiSigAction = new LinkedHashMap<>();
        multiSigAction.put("type", "multiSig");
        multiSigAction.put("signatureChainId", "0x66eee");
        multiSigAction.put("signatures", signatures);

        // 构造 payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("multiSigUser", multiSigUser.toLowerCase());
        payload.put("outerSigner", apiWallet.getPrimaryWalletAddress().toLowerCase());
        payload.put("action", innerAction);
        multiSigAction.put("payload", payload);

        // 签名
        Map<String, Object> signature = Signing.signMultiSigAction(
                apiWallet.getCredentials(),
                multiSigAction,
                isMainnet(),
                vaultAddress,
                nonce,
                null // expiresAfter
        );

        // 发送请求
        return postActionWithSignature(multiSigAction, signature, nonce);
    }

    /**
     * 多签操作（简化版，使用当前 vaultAddress）。
     *
     * @param multiSigUser 多签账户地址
     * @param innerAction  内部动作
     * @param signatures   签名列表
     * @param nonce        随机数/时间戳
     * @return JSON 响应
     */
    public JsonNode multiSig(
            String multiSigUser,
            Map<String, Object> innerAction,
            List<Map<String, Object>> signatures,
            long nonce
    ) {
        return multiSig(multiSigUser, innerAction, signatures, nonce, this.vaultAddress);
    }

    // ==================== PerpDeploy Oracle 设置 ====================

    /**
     * PerpDeploy Oracle 设置（与 Python SDK 的 perp_deploy_set_oracle 对齐）。
     * <p>
     * 用于 Builder-deployed perp dex 的 Oracle 价格更新。
     * </p>
     *
     * @param dex             perp dex 名称
     * @param oraclePxs       Oracle 价格 Map（币种名 -> 价格字符串）
     * @param allMarkPxs      标记价格列表（每个元素是 Map<币种, 价格>）
     * @param externalPerpPxs 外部永续价格 Map（币种名 -> 价格字符串）
     * @return JSON 响应
     */
    public JsonNode perpDeploySetOracle(
            String dex,
            Map<String, String> oraclePxs,
            List<Map<String, String>> allMarkPxs,
            Map<String, String> externalPerpPxs
    ) {
        // 1. 排序 oraclePxs
        List<List<String>> oraclePxsWire = new ArrayList<>();
        if (oraclePxs != null) {
            List<Map.Entry<String, String>> sorted = new ArrayList<>(oraclePxs.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, String> entry : sorted) {
                oraclePxsWire.add(Arrays.asList(entry.getKey(), entry.getValue()));
            }
        }

        // 2. 排序 markPxs
        List<List<List<String>>> markPxsWire = new ArrayList<>();
        if (allMarkPxs != null) {
            for (Map<String, String> markPxs : allMarkPxs) {
                List<List<String>> markWire = new ArrayList<>();
                List<Map.Entry<String, String>> sorted = new ArrayList<>(markPxs.entrySet());
                sorted.sort(Map.Entry.comparingByKey());
                for (Map.Entry<String, String> entry : sorted) {
                    markWire.add(Arrays.asList(entry.getKey(), entry.getValue()));
                }
                markPxsWire.add(markWire);
            }
        }

        // 3. 排序 externalPerpPxs
        List<List<String>> externalPerpPxsWire = new ArrayList<>();
        if (externalPerpPxs != null) {
            List<Map.Entry<String, String>> sorted = new ArrayList<>(externalPerpPxs.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, String> entry : sorted) {
                externalPerpPxsWire.add(Arrays.asList(entry.getKey(), entry.getValue()));
            }
        }

        // 4. 构造 action
        Map<String, Object> setOracle = new LinkedHashMap<>();
        setOracle.put("dex", dex);
        setOracle.put("oraclePxs", oraclePxsWire);
        setOracle.put("markPxs", markPxsWire);
        setOracle.put("externalPerpPxs", externalPerpPxsWire);

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "perpDeploy");
        action.put("setOracle", setOracle);

        return postAction(action);
    }

    // ==================== EVM BigBlocks 开关 ====================

    /**
     * EVM BigBlocks 开关（与 Python SDK 的 use_big_blocks 对齐）。
     * <p>
     * 用于启用/禁用 EVM Big Blocks 功能。
     * </p>
     *
     * @param enable true 表示启用，false 表示禁用
     * @return JSON 响应
     */
    public JsonNode useBigBlocks(boolean enable) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "evmUserModify");
        action.put("usingBigBlocks", enable);

        return postAction(action);
    }

    // ==================== C Validator 操作（专业功能） ====================

    /**
     * C Validator 注册（与 Python SDK 的 c_validator_register 对齐）。
     * <p>
     * 用于注册新的验证者节点。
     * </p>
     *
     * @param nodeIp              节点 IP 地址
     * @param name                验证者名称
     * @param description         验证者描述
     * @param delegationsDisabled 是否禁用委托
     * @param commissionBps       佣金比例（基点，1 bps = 0.01%）
     * @param signer              签名者地址
     * @param unjailed            是否解除监禱
     * @param initialWei          初始质押数量（wei）
     * @return JSON 响应
     */
    public JsonNode cValidatorRegister(
            String nodeIp,
            String name,
            String description,
            boolean delegationsDisabled,
            int commissionBps,
            String signer,
            boolean unjailed,
            long initialWei
    ) {
        // 构造 profile
        Map<String, Object> nodeIpMap = new LinkedHashMap<>();
        nodeIpMap.put("Ip", nodeIp);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("node_ip", nodeIpMap);
        profile.put("name", name);
        profile.put("description", description);
        profile.put("delegations_disabled", delegationsDisabled);
        profile.put("commission_bps", commissionBps);
        profile.put("signer", signer);

        // 构造 register
        Map<String, Object> register = new LinkedHashMap<>();
        register.put("profile", profile);
        register.put("unjailed", unjailed);
        register.put("initial_wei", initialWei);

        // 构造 action
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CValidatorAction");
        action.put("register", register);

        return postAction(action);
    }

    /**
     * C Validator 更改配置（与 Python SDK 的 c_validator_change_profile 对齐）。
     * <p>
     * 用于修改验证者节点的配置信息。所有参数可为 null，仅更新非 null 参数。
     * </p>
     *
     * @param nodeIp             节点 IP 地址（可为 null）
     * @param name               验证者名称（可为 null）
     * @param description        验证者描述（可为 null）
     * @param unjailed           是否解除监禱
     * @param disableDelegations 是否禁用委托（可为 null）
     * @param commissionBps      佣金比例（可为 null）
     * @param signer             签名者地址（可为 null）
     * @return JSON 响应
     */
    public JsonNode cValidatorChangeProfile(
            String nodeIp,
            String name,
            String description,
            boolean unjailed,
            Boolean disableDelegations,
            Integer commissionBps,
            String signer
    ) {
        // 构造 changeProfile
        Map<String, Object> changeProfile = new LinkedHashMap<>();

        if (nodeIp != null) {
            Map<String, Object> nodeIpMap = new LinkedHashMap<>();
            nodeIpMap.put("Ip", nodeIp);
            changeProfile.put("node_ip", nodeIpMap);
        } else {
            changeProfile.put("node_ip", null);
        }

        changeProfile.put("name", name);
        changeProfile.put("description", description);
        changeProfile.put("unjailed", unjailed);
        changeProfile.put("disable_delegations", disableDelegations);
        changeProfile.put("commission_bps", commissionBps);
        changeProfile.put("signer", signer);

        // 构造 action
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CValidatorAction");
        action.put("changeProfile", changeProfile);

        return postAction(action);
    }

    /**
     * C Validator 注销（与 Python SDK 的 c_validator_unregister 对齐）。
     * <p>
     * 用于注销验证者节点。
     * </p>
     *
     * @return JSON 响应
     */
    public JsonNode cValidatorUnregister() {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CValidatorAction");
        action.put("unregister", null);

        return postAction(action);
    }

    // ==================== C Signer 监禱操作（专业功能） ====================

    /**
     * C Signer 监禱自己（与 Python SDK 的 c_signer_jail_self 对齐）。
     * <p>
     * 用于验证者主动监禱自己的签名者。
     * </p>
     *
     * @return JSON 响应
     */
    public JsonNode cSignerJailSelf() {
        return cSignerInner("jailSelf");
    }

    /**
     * C Signer 解除监禱（与 Python SDK 的 c_signer_unjail_self 对齐）。
     * <p>
     * 用于验证者解除签名者的监禱状态。
     * </p>
     *
     * @return JSON 响应
     */
    public JsonNode cSignerUnjailSelf() {
        return cSignerInner("unjailSelf");
    }

    /**
     * C Signer 操作的内部实现。
     *
     * @param variant 操作类型（jailSelf 或 unjailSelf）
     * @return JSON 响应
     */
    private JsonNode cSignerInner(String variant) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CSignerAction");
        action.put(variant, null);

        return postAction(action);
    }

    // ==================== Noop 测试操作 ====================

    /**
     * Noop 测试操作（与 Python SDK 的 noop 对齐）。
     * <p>
     * 用于测试签名和网络连通性，不执行任何实际操作。
     * </p>
     *
     * @param nonce 随机数/时间戳
     * @return JSON 响应
     */
    public JsonNode noop(long nonce) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "noop");

        // 使用自定义 nonce 进行签名
        String effectiveVault = vaultAddress;
        if (effectiveVault != null) {
            effectiveVault = effectiveVault.toLowerCase();
            String signerAddr = apiWallet.getPrimaryWalletAddress().toLowerCase();
            if (effectiveVault.equals(signerAddr)) {
                effectiveVault = null;
            }
        }

        Map<String, Object> signature = Signing.signL1Action(
                apiWallet.getCredentials(),
                action,
                effectiveVault,
                nonce,
                null, // expiresAfter
                isMainnet());

        return postActionWithSignature(action, signature, nonce);
    }
}
