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
 * Exchange client for Hyperliquid SDK, responsible for order placement,
 * cancellation,
 * transfers, and other L1/L2 operations.
 *
 * <p>
 * This class provides comprehensive functionality for interacting with the
 * Hyperliquid exchange,
 * including:
 * </p>
 * <ul>
 * <li>Order management (placement, cancellation, modification)</li>
 * <li>Batch order operations with grouping support</li>
 * <li>Position management (opening, closing, TP/SL)</li>
 * <li>Asset transfers (USD, spot tokens, vault operations)</li>
 * <li>Leverage and margin adjustments</li>
 * <li>Advanced features (builders, agents, multi-signature)</li>
 * <li>Validator operations and protocol-level interactions</li>
 * </ul>
 */
public class Exchange {

    /**
     * User API wallet
     */
    private final ApiWallet apiWallet;

    /**
     * HTTP client
     */
    private final HypeHttpClient hypeHttpClient;

    /**
     * Info client instance
     */
    private final Info info;

    /**
     * Ethereum address (0x prefix)
     */
    private volatile String vaultAddress;

    /**
     * Get vault address
     *
     * @return vault address
     */
    public String getVaultAddress() {
        return vaultAddress;
    }

    /**
     * Set vault address
     *
     * @param vaultAddress vault address
     */
    public void setVaultAddress(String vaultAddress) {
        this.vaultAddress = vaultAddress;
    }

    /**
     * Default slippage, used to calculate slippage price (string)
     */
    private final Map<String, String> defaultSlippageByCoin = new ConcurrentHashMap<>();

    /**
     * Default slippage, used to calculate slippage price (string, e.g., "0.05" for
     * 5%)
     */
    private volatile String defaultSlippage = "0.05";

    /**
     * Construct Exchange client.
     *
     * @param hypeHttpClient HTTP client instance
     * @param wallet         User wallet credentials
     * @param info           Info client instance
     */
    public Exchange(HypeHttpClient hypeHttpClient, ApiWallet wallet, Info info) {
        this.hypeHttpClient = hypeHttpClient;
        this.apiWallet = wallet;
        this.info = info;

    }

    /**
     * Schedule cancellation (scheduleCancel).
     *
     * @param timeMs Millisecond timestamp for cancellation execution; null means
     *               immediate execution
     * @return JSON response
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
     * Change leverage
     *
     * @param coinName Coin name
     * @param crossed  Whether cross margin
     * @param leverage Leverage multiple
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
     * Single order placement (normal order scenario)
     *
     * @param req Order request
     * @return Trading interface response JSON
     */
    public Order order(OrderRequest req) {
        return order(req, null);
    }

    /**
     * Place order (single).
     *
     * @param req     Order request
     * @param builder Optional builder parameters (can be null)
     *                - "b": Builder address (0x prefix string)
     *                - "f": Builder fee (non-negative integer)
     *                For example: When the user wants to utilize a specific
     *                Builder's customized liquidity, specific trading strategies,
     *                or pay Builder fees, then the builder parameter needs to be
     *                set.
     */
    public Order order(OrderRequest req, Map<String, Object> builder) {
        OrderRequest effective = preprocessOrder(req);
        int assetId = ensureAssetId(effective.getCoin());
        OrderWire wire = Signing.orderRequestToOrderWire(assetId, effective);
        Map<String, Object> action = buildOrderAction(List.of(wire), builder);
        // Get order expiresAfter, default 120 seconds
        Long expiresAfter = effective.getExpiresAfter() != null ? effective.getExpiresAfter() : 120_000L;
        JsonNode node = postAction(action, expiresAfter);
        return JSONUtil.convertValue(node, Order.class);
    }

    /**
     * Format order quantity based on asset precision
     */
    private void formatOrderSize(OrderRequest req) {
        if (req == null || req.getSz() == null || req.getSz().isEmpty())
            return;
        // Optimization: fetch szDecimals directly from cache to avoid retrieving the
        // full Universe every time
        Integer szDecimals = info.getSzDecimals(req.getCoin());
        if (szDecimals == null)
            return;
        try {
            // Use BigDecimal to round according to precision; flooring is safer
            BigDecimal bd = new BigDecimal(req.getSz()).setScale(szDecimals, RoundingMode.DOWN);
            req.setSz(bd.toPlainString());
        } catch (NumberFormatException e) {
            throw new HypeError("Invalid order size format: " + req.getSz() + ". Must be a valid number.");
        }
    }

    /**
     * Format order price (limit and trigger price) based on asset precision
     * <p>
     * Rules aligned with Python SDK:
     * 1. First round to 5 significant digits
     * 2. Then round to decimal places (perpetual: 6-szDecimals; spot: 8-szDecimals)
     * </p>
     */
    private void formatOrderPrice(OrderRequest req) {
        if (req == null)
            return;
        // Optimization: fetch szDecimals directly from cache to avoid retrieving the
        // full Universe every time
        Integer szDecimals = info.getSzDecimals(req.getCoin());
        if (szDecimals == null)
            return;
        boolean isSpot = req.getInstrumentType() == InstrumentType.SPOT;

        // Compute decimal places: spot = 8 - szDecimals; perp = 6 - szDecimals
        int decimals = (isSpot ? 8 : 6) - szDecimals;
        if (decimals < 0) {
            decimals = 0;
        }

        // 1. Format limit price (limitPx)
        if (req.getLimitPx() != null && !req.getLimitPx().isEmpty()) {
            try {
                BigDecimal bd = new BigDecimal(req.getLimitPx()).round(new MathContext(5, RoundingMode.HALF_UP))
                        .setScale(decimals, RoundingMode.HALF_UP);
                req.setLimitPx(bd.stripTrailingZeros().toPlainString());
            } catch (NumberFormatException e) {
                throw new HypeError("Invalid limit price format: " + req.getLimitPx() + ". Must be a valid number.");
            }
        }

        // 2. Format trigger price (triggerPx)
        if (req.getOrderType() != null && req.getOrderType().getTrigger() != null) {
            String triggerPx = req.getOrderType().getTrigger().getTriggerPx();
            if (triggerPx != null && !triggerPx.isEmpty()) {
                try {
                    BigDecimal bd = new BigDecimal(triggerPx).round(new MathContext(5, RoundingMode.HALF_UP))
                            .setScale(decimals, RoundingMode.HALF_UP);
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
     * Preprocess a single order request before signing and submission.
     * <p>
     * This helper centralizes the common preparation pipeline shared by
     * {@link #order(OrderRequest, java.util.Map)} and
     * {@link #bulkOrders(java.util.List, java.util.Map, String)}:
     * </p>
     * <ol>
     * <li>Invoke {@link #prepareRequest(OrderRequest)} to infer direction,
     * size, market placeholder prices and trigger defaults when necessary.</li>
     * <li>Normalize order size precision via
     * {@link #formatOrderSize(OrderRequest)}.</li>
     * <li>Normalize limit and trigger price precision via
     * {@link #formatOrderPrice(OrderRequest)}.</li>
     * </ol>
     *
     * @param req Original order request (must not be null)
     * @return Prepared order request used for signing and posting
     */
    private OrderRequest preprocessOrder(OrderRequest req) {
        OrderRequest effective = prepareRequest(req);
        formatOrderSize(effective);
        formatOrderPrice(effective);
        return effective;
    }

    /**
     * Prepare order request before serialization and submission.
     * <p>
     * Centralizes pre-processing logic for the single {@link #order(OrderRequest)}
     * flow, including:
     * <ol>
     * <li>Market-open placeholder handling: for non-reduce-only IOC orders,
     * calculate a slippage-based limit price via
     * {@link #applyMarketOpenSlippage(OrderRequest)}.</li>
     * <li>Market close-position inference: for close-position "market
     * placeholders" (reduce-only IOC without price), automatically infer
     * closing direction and size via
     * {@link #prepareMarketCloseRequest(OrderRequest)}.</li>
     * <li>Limit close-position inference: for close-position "limit
     * placeholders" (reduce-only GTC with price but without direction),
     * infer closing direction via
     * {@link #prepareLimitCloseRequest(OrderRequest)}.</li>
     * <li>Trigger order normalization: for trigger orders without explicit
     * limit price, fetch current mid price and use it as default via
     * {@link #prepareTriggerOrderRequest(OrderRequest)}.</li>
     * </ol>
     * This method does not perform size/price precision formatting; those are
     * handled separately by {@link #formatOrderSize(OrderRequest)} and
     * {@link #formatOrderPrice(OrderRequest)} after this step.
     * </p>
     *
     * @param req Original order request (must not be null)
     * @return Prepared order request with inferred fields filled when needed
     * @throws HypeError If the request is null or required account information
     *                   (e.g. position or mid price) is missing
     */
    private OrderRequest prepareRequest(OrderRequest req) {
        if (req == null) {
            throw new HypeError("OrderRequest cannot be null");
        }
        // Infer market order price with slippage
        if (applyMarketOpenSlippage(req)) {
            return req;
        }
        // Market close position inference
        if (isClosePositionMarket(req)) {
            return prepareMarketCloseRequest(req);
        }
        // Limit close position inference
        if (isClosePositionLimit(req)) {
            return prepareLimitCloseRequest(req);
        }
        // Conditional order inference
        if (isTriggerOrder(req)) {
            return prepareTriggerOrderRequest(req);
        }
        return req;
    }

    /**
     * Apply market-open slippage placeholder price for IOC market orders.
     * <p>
     * This method is used by both the single order() flow and the bulkOrders
     * flow to convert "market" semantics into an IOC limit order with a
     * slippage-derived price.
     * </p>
     *
     * @param req Order request
     * @return true if slippage price was applied, false otherwise
     */
    private boolean applyMarketOpenSlippage(OrderRequest req) {
        if (req == null) {
            return false;
        }
        if (req.getLimitPx() == null &&
                req.getOrderType() != null &&
                req.getOrderType().getLimit() != null &&
                req.getOrderType().getLimit().getTif() == Tif.IOC &&
                Boolean.FALSE.equals(req.getReduceOnly())) {
            String slip = req.getSlippage() != null ? req.getSlippage()
                    : defaultSlippageByCoin.getOrDefault(req.getCoin(), defaultSlippage);
            String slipPx = computeSlippagePrice(req.getCoin(), Boolean.TRUE.equals(req.getIsBuy()), slip);
            req.setLimitPx(slipPx);
            return true;
        }
        return false;
    }

    /**
     * Prepare market close position request by inferring position direction and
     * size.
     * <p>
     * If isBuy or sz is not specified, automatically infer:
     * 1. Query user's current position for the coin
     * 2. Determine closing direction (opposite to current position)
     * 3. Calculate closing size (absolute value of position size)
     * </p>
     * <p>
     * Note: This automatic inference is applied in the single
     * {@link #order(OrderRequest)}
     * flow only. When using
     * {@link #bulkOrders(java.util.List, java.util.Map, String)}
     * or other batch submission APIs, callers are expected to fully specify
     * direction and size in each close-position order.
     * </p>
     *
     * @param req Original order request
     * @return Processed order request with inferred parameters
     * @throws HypeError If no position exists for the specified coin
     */
    private OrderRequest prepareMarketCloseRequest(OrderRequest req) {
        if (req.getIsBuy() != null && req.getSz() != null && req.getLimitPx() != null) {
            return req;
        }
        double szi = inferSignedPosition(req.getCoin());
        if (szi == 0.0) {
            throw new HypeError("No position to close for coin " + req.getCoin());
        }
        boolean isBuy = szi < 0.0;
        if (req.getIsBuy() == null) {
            req.setIsBuy(isBuy);
        }
        String sz = (req.getSz() != null && !req.getSz().isEmpty()) ? req.getSz() : String.valueOf(Math.abs(szi));
        if (req.getSz() == null) {
            req.setSz(sz);
        }
        if (req.getLimitPx() == null) {
            String slip = req.getSlippage() != null ? req.getSlippage()
                    : defaultSlippageByCoin.getOrDefault(req.getCoin(), defaultSlippage);
            String slipPx = computeSlippagePrice(req.getCoin(), Boolean.TRUE.equals(req.getIsBuy()), slip);
            req.setLimitPx(slipPx);
        }
        return req;
    }

    /**
     * Prepare limit close position request by inferring position direction.
     * <p>
     * Automatically determines the closing direction based on current position:
     * - If holding a short position (negative size), set isBuy=true to close
     * - If holding a long position (positive size), set isBuy=false to close
     * </p>
     * <p>
     * Note: This inference is only used in the single {@link #order(OrderRequest)}
     * flow. Batch APIs such as
     * {@link #bulkOrders(java.util.List, java.util.Map, String)}
     * require callers to explicitly provide the closing direction.
     * </p>
     *
     * @param req Original order request
     * @return Processed order request with inferred direction
     * @throws HypeError If no position exists for the specified coin
     */
    private OrderRequest prepareLimitCloseRequest(OrderRequest req) {
        double signedPosition = inferSignedPosition(req.getCoin());
        if (signedPosition == 0.0) {
            throw new HypeError("No position to close for coin " + req.getCoin());
        }
        boolean isBuy = signedPosition < 0.0;
        req.setIsBuy(isBuy);
        return req;
    }

    /**
     * Prepare trigger order request by setting limit price to current market mid
     * price if not specified.
     * <p>
     * For trigger orders, if limit price is not provided:
     * 1. Fetch current market mid price for the coin
     * 2. Set the limit price to the mid price as default
     * </p>
     *
     * @param req Original order request
     * @return Processed order request with limit price set if needed
     * @throws HypeError If no market mid price is available for the specified coin
     */
    private OrderRequest prepareTriggerOrderRequest(OrderRequest req) {
        if (req.getLimitPx() == null) {
            Map<String, String> mids = info.allMids();
            String midStr = mids.get(req.getCoin());
            if (midStr == null) {
                throw new HypeError("No mid for coin " + req.getCoin());
            }
            req.setLimitPx(midStr);
        }
        return req;
    }

    /**
     * Determine if it's a "market close position placeholder" request.
     *
     * @param req Order request
     * @return Returns true if yes, false otherwise
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
     * Determine if it's a "limit close position placeholder" request.
     *
     * @param req Order request
     * @return Returns true if yes, false otherwise
     */
    private boolean isClosePositionLimit(OrderRequest req) {
        return req != null
                && req.getInstrumentType() == InstrumentType.PERP
                && req.getOrderType() != null
                && req.getOrderType().getLimit() != null
                && req.getOrderType().getLimit().getTif() == Tif.GTC
                && Boolean.TRUE.equals(req.getReduceOnly())
                && req.getLimitPx() != null
                && req.getIsBuy() == null;
    }

    /**
     * Determine if it's a "conditional order" request.
     * <p>
     * Currently only supports perpetual (PERP) trigger orders. If spot trigger
     * orders are introduced in the future, this method should be updated to
     * relax the instrument type constraint accordingly.
     * </p>
     *
     * @param req Order request
     * @return Returns true if yes, false otherwise
     */
    private boolean isTriggerOrder(OrderRequest req) {
        return req != null
                && req.getInstrumentType() == InstrumentType.PERP
                && req.getOrderType() != null
                && req.getOrderType().getTrigger() != null;
    }

    /**
     * Infer the current account's "signed position size" for the specified coin.
     *
     * <p>
     * Positive numbers indicate long positions, negative numbers indicate short
     * positions; returns 0.0 when there is no position. Parsing failures are
     * treated as fatal and will throw HypeError.
     * </p>
     *
     * @param coin Coin name
     * @return Signed size (double)
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
                } catch (Exception e) {
                    throw new HypeError("Failed to parse position size: " + pos.getSzi(), e);
                }
            }
        }
        return 0.0;
    }

    /**
     * Automatically infer and fill position direction and quantity for positionTpsl
     * order groups.
     * <p>
     * When isBuy or sz in the order is null:
     * - Automatically query account positions
     * - Infer direction and quantity based on szi (signed position size)
     * - Fill direction and quantity for all orders
     * </p>
     *
     * @param orders positionTpsl order list (same coin)
     * @throws HypeError Thrown when there is no position
     */
    private void inferAndFillPositionTpslOrders(List<OrderRequest> orders) {
        // Get the coin of the first order (positionTpsl all orders should be the same
        // coin)
        OrderRequest firstOrder = orders.getFirst();
        String coin = firstOrder.getCoin();

        // Check if auto-inference is needed (isBuy or sz is null)
        boolean needsInference = firstOrder.getIsBuy() == null || firstOrder.getSz() == null;

        if (!needsInference) {
            return;
        }

        // Automatically query position and infer
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError(
                    "No position found for " + coin + ". Cannot auto-infer direction and size for positionTpsl.");
        }

        // Infer direction and quantity
        boolean isBuy = szi > 0; // Long position needs to sell to close, so isBuy=true means long position
        String sz = String.valueOf(Math.abs(szi));

        // Fill direction and quantity for all orders
        for (OrderRequest order : orders) {
            if (order.getIsBuy() == null) {
                // For take-profit/stop-loss orders, need to reverse direction
                if (order.getReduceOnly() != null && order.getReduceOnly()) {
                    order.setIsBuy(!isBuy); // Reverse direction to close position
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
     * Update isolated margin
     *
     * @param amount   Amount (USD, string, internally converted to micro units)
     * @param coinName Coin name
     * @return JSON response
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
     * Batch order placement (with grouping support).
     *
     * @param requests Order request list
     * @param builder  Optional builder
     * @param grouping Grouping type: "na" | "normalTpsl" | "positionTpsl"
     *                 1. "na" - Normal orders (default)
     *                 Usage scenarios:
     *                 ✅ Single normal orders (open, close, limit, market, etc.)
     *                 ✅ Batch orders with no correlation between orders
     *                 ✅ Any orders that don't need TP/SL
     *                 2. "normalTpsl" - Normal take-profit/stop-loss group
     *                 Usage scenarios:
     *                 ✅ Open position and set TP/SL simultaneously
     *                 ✅ Batch orders: 1 opening order + 1 or 2 TP/SL orders
     *                 3. "positionTpsl" - Position take-profit/stop-loss group
     *                 Usage scenarios:
     *                 ✅ Set or modify TP/SL for existing positions
     *                 ✅ Don't open new positions, only set protection for existing
     *                 positions
     * @return Response JSON
     */
    public JsonNode bulkOrders(List<OrderRequest> requests, Map<String, Object> builder, String grouping) {
        List<OrderRequest> effectiveRequests = new ArrayList<>(requests.size());
        for (OrderRequest r : requests) {
            effectiveRequests.add(preprocessOrder(r));
        }
        List<OrderWire> wires = new ArrayList<>();
        for (OrderRequest r : effectiveRequests) {
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
     * Batch order placement (with OrderGroup automatic grouping inference).
     * <p>
     * Automatically identifies grouping type through OrderGroup, no need to
     * manually specify grouping parameter.
     * <p>
     * Usage examples:
     *
     * <pre>
     * // Automatically infer grouping="normalTpsl"
     * OrderGroup orderGroup = OrderRequest.entryWithTpSl()
     *         .perp("ETH")
     *         .buy(0.1)
     *         .entryPrice(3500.0)
     *         .takeProfit(3600.0)
     *         .stopLoss(3400.0)
     *         .buildNormalTpsl();
     * JsonNode result = exchange.bulkOrders(orderGroup);
     *
     * // Automatically infer grouping="positionTpsl"
     * OrderGroup orderGroup2 = OrderRequest.entryWithTpSl()
     *         .perp("ETH")
     *         .closePosition(0.5, true)
     *         .takeProfit(3600.0)
     *         .buildPositionTpsl();
     * JsonNode result2 = exchange.bulkOrders(orderGroup2);
     * </pre>
     *
     * @param orderGroup Order group (contains order list and grouping type)
     * @return Response JSON
     */
    public JsonNode bulkOrders(OrderGroup orderGroup) {
        return bulkOrders(orderGroup, null);
    }

    /**
     * Batch order placement (with OrderGroup and builder support).
     * <p>
     * For positionTpsl type order groups, if isBuy or sz in the order is null,
     * it will automatically query account positions and fill in direction and
     * quantity.
     *
     * @param orderGroup Order group (contains order list and grouping type)
     * @param builder    Optional builder
     * @return Response JSON
     */
    public JsonNode bulkOrders(OrderGroup orderGroup, Map<String, Object> builder) {
        List<OrderRequest> orders = orderGroup.getOrders();
        if (orders == null || orders.isEmpty()) {
            throw new HypeError("No orders found in OrderGroup.");
        }
        // For positionTpsl, check if automatic position inference is needed
        if (GroupingType.POSITION_TPSL == orderGroup.getGroupingType()) {
            inferAndFillPositionTpslOrders(orders);
        }
        return bulkOrders(orders, builder, orderGroup.getGroupingType().getValue());
    }

    /**
     * Batch order placement (normal orders, default grouping="na").
     * <p>
     * Used to submit multiple normal orders in batch, with no correlation between
     * orders.
     * <p>
     * Usage example:
     *
     * <pre>
     * // Batch orders for multiple coins
     * List<OrderRequest> orders = Arrays.asList(
     *         OrderRequest.builder().perp("BTC").buy(0.01).limitPrice(95000.0).build(),
     *         OrderRequest.builder().perp("ETH").buy(0.1).limitPrice(3500.0).build());
     * JsonNode result = exchange.bulkOrders(orders);
     * </pre>
     *
     * @param requests Order list
     * @return Response JSON
     */
    public JsonNode bulkOrders(List<OrderRequest> requests) {
        return bulkOrders(requests, null, null);
    }

    /**
     * Cancel order by OID (maintain consistency with Python cancel behavior).
     *
     * @param coinName Coin name
     * @param oid      Order OID
     * @return Response JSON
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
     * Cancel order by Cloid (maintain consistency with Python cancel_by_cloid
     * behavior).
     *
     * @param coinName Coin name
     * @param cloid    Client order ID
     * @return Response JSON
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
     * Modify order (by OID).
     *
     * @param coinName Coin name
     * @param oid      Original order OID
     * @param newReq   New order request (price/quantity/type, etc.)
     * @return Response JSON
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
     * Batch modify orders (aligned with Python bulk_modify_orders_new).
     * <p>
     * Usage example:
     *
     * <pre>
     * // Modify multiple orders
     * List<ModifyRequest> modifies = Arrays.asList(
     *         ModifyRequest.byOid("ETH", 123456L, newReq1),
     *         ModifyRequest.byCloid("BTC", cloid, newReq2));
     * JsonNode result = exchange.bulkModifyOrders(modifies);
     * </pre>
     *
     * @param modifyRequests Batch modify request list
     * @return Response JSON
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
            // Support OID or Cloid
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
     * Build order action (includes grouping:"na" and optional builder).
     *
     * @param wires   Order wire list
     * @param builder Optional builder
     * @return L1 action Map
     */
    private Map<String, Object> buildOrderAction(List<OrderWire> wires, Map<String, Object> builder) {
        Map<String, Object> action = Signing.orderWiresToOrderAction(wires);
        // Keep default group "na" in Signing.orderWiresToOrderAction; do not override
        if (builder != null && !builder.isEmpty()) {
            Map<String, Object> filtered = validateAndFilterBuilder(builder);
            if (!filtered.isEmpty()) {
                action.put("builder", filtered);
            }
        }
        return action;
    }

    /**
     * Validate and filter builder parameters.
     * <p>
     * Only retain fields allowed by official documentation:
     * - b (address): Builder address
     * - f (fee): Builder fee (non-negative integer)
     * Other keys will be ignored to avoid 422 deserialization failure.
     * </p>
     *
     * @param builder Original builder parameters
     * @return Filtered builder parameters
     * @throws HypeError Thrown when parameter validation fails
     */
    private Map<String, Object> validateAndFilterBuilder(Map<String, Object> builder) {
        Map<String, Object> filtered = new LinkedHashMap<>();

        // Validate and filter address field b
        if (builder.containsKey("b")) {
            Object bVal = builder.get("b");
            if (bVal instanceof String s) {
                filtered.put("b", s.toLowerCase());
            }
        }

        // Validate and filter fee field f
        if (builder.containsKey("f")) {
            Object fVal = builder.get("f");
            if (!(fVal instanceof Number)) {
                throw new HypeError("builder.f must be a non-negative integer (numeric type)");
            }
            long f = ((Number) fVal).longValue();
            if (f < 0) {
                throw new HypeError("builder.f cannot be negative");
            }
            // Limit a reasonable upper bound to avoid mistakenly passing oversized numbers
            // that cause backend rejection (can be adjusted according to business)
            if (f > 1_000_000L) {
                throw new HypeError("builder.f is too large, please verify the unit and value range");
            }
            filtered.put("f", f);
        }

        return filtered;
    }

    /**
     * Enable Agent-side Dex Abstraction (consistent with Python
     * exchange.agent_enable_dex_abstraction).
     * Description:
     * - The server will create/enable an API Wallet (Agent) based on this action
     * for L1 order placement and other operations.
     * - This is an L1 action, directly using signL1Action for signing and
     * submission.
     *
     * @return JSON response
     */
    public JsonNode agentEnableDexAbstraction() {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "agentEnableDexAbstraction");
        // Directly reuse L1 sending logic
        return postAction(action);
    }

    /**
     * User-side Dex Abstraction switch (consistent with Python
     * exchange.user_dex_abstraction).
     *
     * @param user    User address (0x prefix)
     * @param enabled Whether to enable
     * @return JSON response
     */
    public JsonNode userDexAbstraction(String user, boolean enabled) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "userDexAbstraction");
        action.put("user", user == null ? null : user.toLowerCase());
        action.put("enabled", enabled);
        action.put("nonce", nonce);

        // Construct payloadTypes exactly consistent with Python
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
        // Send in line with _post_action (without redoing L1 signing)
        return postActionWithSignature(action, signature, nonce);
    }

    /**
     * Create sub-account.
     *
     * @param name Sub-account name
     * @return JSON response
     */
    public JsonNode createSubAccount(String name) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "createSubAccount");
        action.put("name", name);
        return postAction(action);
    }

    /**
     * Sub-account fund transfer.
     *
     * @param subAccountUser Sub-account address (0x prefix)
     * @param isDeposit      true means deposit; false means withdraw
     * @param usd            Amount (micro USDC units)
     * @return JSON response
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
     * USD balance transfer (user signed).
     *
     * @param amount      Amount (string)
     * @param destination Destination address (0x prefix)
     * @return JSON response
     */
    public JsonNode usdTransfer(String amount, String destination) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "usdSend");
        action.put("destination", destination);
        action.put("amount", amount);
        // Use the string directly
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
     * Spot token transfer (user signed).
     *
     * @param amount      Transfer quantity (string)
     * @param destination Destination address (0x prefix)
     * @param token       Token name (e.g., "HL")
     * @return JSON response
     */
    public JsonNode spotTransfer(String amount, String destination, String token) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "spotSend");
        action.put("destination", destination);
        action.put("token", token);
        action.put("amount", amount);
        // Use the string directly
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
     * Withdraw from bridge contract (withdraw3, user signed).
     *
     * @param amount      Amount (string)
     * @param destination Destination address (0x prefix)
     * @return JSON response
     */
    public JsonNode withdrawFromBridge(String amount, String destination) {
        long time = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "withdraw3");
        action.put("destination", destination);
        action.put("amount", amount);
        // Use the string directly
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
     * USD category transfer (Spot ⇄ Perp).
     *
     * @param toPerp true means transfer from Spot to Perp; false means transfer
     *               from Perp to Spot
     * @param amount Amount (string)
     * @return JSON response
     */
    public JsonNode usdClassTransfer(boolean toPerp, String amount) {
        long nonce = Signing.getTimestampMs();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "usdClassTransfer");
        String strAmount = amount;
        // Already a string
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
     * Cross-DEX asset transfer (sendAsset, user signed).
     *
     * @param destination    Destination address (0x prefix)
     * @param sourceDex      Source DEX name
     * @param destinationDex Destination DEX name
     * @param token          Token name
     * @param amount         Quantity (string)
     * @param fromSubAccount Source sub-account address (optional)
     * @return JSON response
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
     * Authorize Builder fee rate (user signed).
     *
     * @param builder    Builder address (0x prefix)
     * @param maxFeeRate Allowed maximum fee rate (string decimal)
     * @return JSON response
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
     * Bind referral code (user signed).
     *
     * @param code Referral code string
     * @return JSON response
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
     * Token delegation/undelegation (user signed).
     *
     * @param validator    Validator address (0x prefix)
     * @param wei          Delegation amount (Wei units)
     * @param isUndelegate true means undelegate; false means delegate
     * @return JSON response
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
     * Convert to multi-signature user (user signed).
     *
     * @param signersJson Signer configuration JSON string
     * @return JSON response
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
     * Vault fund transfer (deposit/withdraw)
     *
     * @param vaultAddress Vault address
     * @param isDeposit    Whether to deposit
     * @param usd          Amount (micro USDC units)
     * @return JSON response
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
     * SpotDeploy: Register Token (registerToken2)
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
     * SpotDeploy: User genesis allocation (userGenesis).
     *
     * @param token               Token ID
     * @param userAndWei          User and Wei amount list, in the form
     *                            [[user,addressLower],[wei,string]]
     * @param existingTokenAndWei Existing token and Wei amount list, in the form
     *                            [[tokenId,int],[wei,string]]
     * @return JSON response
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
     * SpotDeploy: Enable freeze privilege.
     *
     * @param token Token ID
     * @return JSON response
     */
    public JsonNode spotDeployEnableFreezePrivilege(int token) {
        return spotDeployTokenActionInner("enableFreezePrivilege", token);
    }

    /**
     * SpotDeploy: Revoke freeze privilege.
     *
     * @param token Token ID
     * @return JSON response
     */
    public JsonNode spotDeployRevokeFreezePrivilege(int token) {
        return spotDeployTokenActionInner("revokeFreezePrivilege", token);
    }

    /**
     * SpotDeploy: Freeze/unfreeze user.
     *
     * @param token  Token ID
     * @param user   User address (0x prefix)
     * @param freeze true to freeze; false to unfreeze
     * @return JSON response
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
     * SpotDeploy: Enable quote token.
     *
     * @param token Token ID
     * @return JSON response
     */
    public JsonNode spotDeployEnableQuoteToken(int token) {
        return spotDeployTokenActionInner("enableQuoteToken", token);
    }

    /**
     * SpotDeploy: Generic token operation internal wrapper
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
     * SpotDeploy: Genesis.
     *
     * @param token            Token ID
     * @param maxSupply        Maximum supply (string)
     * @param noHyperliquidity Whether to disable Hyperliquidity
     * @return JSON response
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
     * SpotDeploy: Register spot trading pair (registerSpot).
     *
     * @param baseToken  Base token ID
     * @param quoteToken Quote token ID
     * @return JSON response
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
     * SpotDeploy: Register Hyperliquidity market making.
     *
     * @param spot          Spot trading pair ID
     * @param startPx       Starting price
     * @param orderSz       Order size per level
     * @param nOrders       Number of order levels
     * @param nSeededLevels Number of seeded levels (optional)
     * @return JSON response
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
     * SpotDeploy: Set deployer trading fee share.
     *
     * @param token Token ID
     * @param share Share ratio (string decimal)
     * @return JSON response
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
     * User authorization and creation of new Agent (API Wallet).
     * Consistent with Python Exchange.approve_agent:
     * - Randomly generate 32-byte private key to get agentAddress;
     * - Construct {type:"approveAgent", agentAddress, agentName?, nonce} user
     * signed action;
     * - Sign using
     * signUserSignedAction(primaryType="HyperliquidTransaction:ApproveAgent");
     * - Send to /exchange and return server response with new private key.
     * <p>
     * Note: When name is null, the agentName field is not included in the action
     * (aligned with Python).
     *
     * @param name Optional Agent name (for display purposes), can be null
     * @return Server response and generated Agent private key/address
     */
    public ApproveAgentResult approveAgent(String name) {
        // Generate a 32-byte random private key (0x prefix)
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
     * Unified L1 action sending wrapper (sign and POST to /exchange).
     *
     * <p>
     * Rules:
     * - nonce uses millisecond timestamp (consistent with Python get_timestamp_ms);
     * - usdClassTransfer/sendAsset type actions do not include vaultAddress
     * (maintain Python behavior consistency);
     * - Other actions use the set vaultAddress and expiresAfter;
     * - Use Signing.signL1Action to complete TypedData construction and signing.
     *
     * @param action L1 action (Map)
     * @return JSON response
     */
    public JsonNode postAction(Map<String, Object> action) {
        return postAction(action, null);
    }

    /**
     * Send L1 action and sign (support custom expiration time).
     * <p>
     * Rules:
     * - nonce uses millisecond timestamp (consistent with Python get_timestamp_ms);
     * - usdClassTransfer/sendAsset type actions do not include vaultAddress
     * (maintain Python behavior consistency);
     * - Other actions use the set vaultAddress;
     * - Use Signing.signL1Action to complete TypedData construction and signing.
     *
     * @param action       L1 action (Map)
     * @param expiresAfter Order expiration time (milliseconds), uses default value
     *                     120000ms when null
     * @return JSON response
     */
    public JsonNode postAction(Map<String, Object> action, Long expiresAfter) {
        long nonce = Signing.getTimestampMs();
        String type = String.valueOf(action.getOrDefault("type", ""));
        String effectiveVault = calculateEffectiveVaultAddress(type);

        // Default 120 seconds
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
     * Unified wrapper: Use existing signature (user signature or other signature)
     * to send to /exchange.
     * The difference from postAction is: the signature is not generated within this
     * method, but accepts an externally passed signature.
     *
     * @param action    Action Map
     * @param signature Existing r/s/v signature (corresponding to EIP-712
     *                  TypedData)
     * @param nonce     Timestamp/random number
     * @return JSON response
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
            // userSigned actions do not need expiresAfter
        } else {
            payload.put("vaultAddress", effectiveVault);
        }
        return hypeHttpClient.post("/exchange", payload);
    }

    /**
     * Validate and return asset ID.
     *
     * @param coinName Coin name
     * @return Asset ID
     * @throws HypeError Thrown when mapping fails
     */
    private int ensureAssetId(String coinName) {
        Integer assetId = info.nameToAsset(coinName);
        if (assetId == null) {
            throw new HypeError("Unknown coin name: " + coinName);
        }
        return assetId;
    }

    /**
     * Calculate effective vault address.
     * <p>
     * Rules:
     * - usdClassTransfer and sendAsset types do not use vaultAddress
     * - If vaultAddress is the same as the signer address, return null
     * - Otherwise return lowercase vaultAddress
     * </p>
     *
     * @param actionType Action type
     * @return Effective vault address, or null
     */
    private String calculateEffectiveVaultAddress(String actionType) {
        // usdClassTransfer and sendAsset do not use vaultAddress
        if ("usdClassTransfer".equals(actionType) || "sendAsset".equals(actionType)) {
            return null;
        }
        if (vaultAddress == null) {
            return null;
        }
        String effectiveVault = vaultAddress.toLowerCase();
        String signerAddr = apiWallet.getPrimaryWalletAddress().toLowerCase();

        // If vault address is the same as signer address, return null
        if (effectiveVault.equals(signerAddr)) {
            return null;
        }
        return effectiveVault;
    }

    /**
     * Determine if the action is a user signature type.
     * <p>
     * User signature actions use EIP-712 TypedData signature, different from L1
     * action signatures.
     * </p>
     *
     * @param actionType Action type
     * @return Returns true if it's a user signature action, false otherwise
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
     * Parse Dex Abstraction enabled status.
     *
     * @param node Status JSON
     * @return Returns true if enabled, false otherwise
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
     * Determine if response status is ok.
     *
     * @param node Response JSON
     * @return Returns true if yes, false otherwise
     */
    private boolean isOk(JsonNode node) {
        return node != null && node.has("status") && "ok".equalsIgnoreCase(node.get("status").asText());
    }

    /**
     * Determine if response is "already set" type error.
     *
     * @param node Response JSON
     * @return Returns true if yes, false otherwise
     */
    private boolean isAlreadySet(JsonNode node) {
        return node != null && node.has("status") && "err".equalsIgnoreCase(node.get("status").asText())
                && node.has("response") && node.get("response").isTextual()
                && node.get("response").asText().toLowerCase().contains("already set");
    }

    /**
     * Calculate price with slippage (string version)
     */
    public String computeSlippagePrice(String coin, boolean isBuy, String slippage) {
        Map<String, String> mids = info.getCachedAllMids();
        String midStr = mids.get(coin);
        if (midStr == null) {
            throw new HypeError("Failed to get mid price for coin " + coin
                    + " (allMids returned empty or does not contain the coin)");
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
     * Set global default slippage ratio.
     *
     * @param slippage Slippage ratio (string, e.g., "0.05" for 5%)
     */
    public void setDefaultSlippage(String slippage) {
        this.defaultSlippage = slippage;
    }

    /**
     * Set default slippage ratio for specified coin (overrides global).
     *
     * @param coin     Coin name
     * @param slippage Slippage ratio (string)
     */
    public void setDefaultSlippage(String coin, String slippage) {
        if (coin != null)
            this.defaultSlippageByCoin.put(coin, slippage);
    }

    /**
     * Market close all positions for specified coin (automatically infer direction
     * and quantity based on current account position).
     *
     * @param coin Coin name
     * @return Server order response
     * @throws HypeError Thrown when there is no position to close
     */
    public Order closePositionMarket(String coin) {
        return order(OrderRequest.Close.marketAll(coin));
    }

    /**
     * Market close position for specified coin (supports partial closing and custom
     * slippage).
     * <p>
     * Automatically queries account position, infers closing direction (sell
     * long/buy short), and closes at market price.
     * <p>
     * Usage examples:
     *
     * <pre>
     * // Complete closing
     * Order result = exchange.closePositionMarket("ETH", null, null, null);
     *
     * // Partial closing
     * Order result = exchange.closePositionMarket("ETH", 0.5, null, null);
     *
     * // Custom slippage
     * Order result = exchange.closePositionMarket("ETH", null, 0.1, null);
     * </pre>
     *
     * @param coin     Coin name
     * @param sz       Closing quantity (can be null, defaults to full closing)
     * @param slippage Slippage ratio (can be null, defaults to 0.05)
     * @param cloid    Client order ID (can be null)
     * @return Order response
     * @throws HypeError Thrown when there is no position to close
     */
    public Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid) {
        // Query current position
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError("No position to close for coin " + coin);
        }

        // Infer closing direction: sell if long; buy if short
        boolean isBuy = szi < 0;

        // Determine closing quantity
        String closeSz = (sz != null && !sz.isEmpty()) ? sz : String.valueOf(Math.abs(szi));

        // Build market close request
        OrderRequest req = OrderRequest.Close.market(coin, isBuy, closeSz, cloid);

        // Set slippage (if provided)
        if (slippage != null && !slippage.isEmpty()) {
            req.setSlippage(slippage);
        }

        return order(req);
    }

    /**
     * Market close position for specified coin (with builder support).
     *
     * @param coin     Coin name
     * @param sz       Closing quantity (can be null)
     * @param slippage Slippage ratio (can be null)
     * @param cloid    Client order ID (can be null)
     * @param builder  Builder information (can be null)
     * @return Order response
     */
    public Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid,
            Map<String, Object> builder) {
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
     * Limit close all positions for specified coin (automatically infer direction
     * and quantity based on current account position).
     *
     * @param tif     TIF strategy
     * @param coin    Coin name
     * @param limitPx Limit price
     * @param cloid   Client order ID (can be null)
     * @return Server order response
     * @throws HypeError Thrown when there is no position to close
     */
    public Order closePositionLimit(Tif tif, String coin, String limitPx, Cloid cloid) {
        double szi = inferSignedPosition(coin);
        if (szi == 0.0) {
            throw new HypeError("No position to close for coin " + coin);
        }
        boolean isBuy = szi < 0.0;
        OrderRequest req = OrderRequest.Close.limit(tif, coin, isBuy, String.valueOf(Math.abs(szi)), limitPx, cloid);
        return order(req);
    }

    /**
     * Market close all positions for all coins (automatically infer long/short
     * directions).
     * <p>
     * Query all account positions, automatically infer closing direction and
     * quantity for each coin, batch order to close all at once.
     * Supports closing multiple long and short positions across different coins
     * simultaneously.
     * </p>
     * <p>
     * Usage example:
     *
     * <pre>
     * // One-click close all positions
     * JsonNode result = exchange.closeAllPositions();
     * System.out.println("Closing result: " + result);
     * </pre>
     *
     * @return Batch order response JSON
     * @throws HypeError Thrown when there are no positions to close
     */
    public JsonNode closeAllPositions() {
        // Query all positions for the current account
        ClearinghouseState state = info.userState(apiWallet.getPrimaryWalletAddress().toLowerCase());
        if (state == null || state.getAssetPositions() == null || state.getAssetPositions().isEmpty()) {
            throw new HypeError("No positions to close");
        }

        // Build all closing orders
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
                continue;
                // Skip if parsing fails
            }

            // Skip coins without positions
            if (szi == 0.0) {
                continue;
            }

            // Infer closing direction: sell if long; buy if short
            boolean isBuy = szi < 0;
            double closeSz = Math.abs(szi);

            // Build market close request
            OrderRequest req = OrderRequest.Close.market(pos.getCoin(), isBuy, String.valueOf(closeSz), null);
            String slip = req.getSlippage() != null ? req.getSlippage()
                    : defaultSlippageByCoin.getOrDefault(req.getCoin(), defaultSlippage);
            String slipPx = computeSlippagePrice(req.getCoin(), Boolean.TRUE.equals(req.getIsBuy()), slip);
            req.setLimitPx(slipPx);
            closeOrders.add(req);
        }

        // Check whether there are orders that need closing
        if (closeOrders.isEmpty()) {
            throw new HypeError("No positions to close (all positions are zero)");
        }

        // Place batch close orders
        return bulkOrders(closeOrders);
    }

    /**
     * Determine if current network is mainnet.
     *
     * @return Returns true if mainnet, false otherwise
     */
    private boolean isMainnet() {
        return Constants.MAINNET_API_URL.equals(hypeHttpClient.getBaseUrl());
    }

    // ==================== Spot Sub Account Transfer ====================

    /**
     * Spot sub account transfer (aligned with Python SDK's
     * sub_account_spot_transfer).
     * <p>
     * Used to transfer spot tokens between main account and Spot sub account.
     * </p>
     *
     * @param subAccountUser Sub account user address (42-character hexadecimal
     *                       format)
     * @param isDeposit      true means transfer from main account to sub account,
     *                       false means transfer from sub account to main account
     * @param token          Token name (e.g., "USDC", "ETH", etc.)
     * @param amount         Transfer quantity (string format)
     * @return JSON response
     */

    public JsonNode subAccountSpotTransfer(String subAccountUser, boolean isDeposit, String token, String amount) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "subAccountSpotTransfer");
        action.put("subAccountUser", subAccountUser == null ? null : subAccountUser.toLowerCase());
        action.put("isDeposit", isDeposit);
        action.put("token", token);
        action.put("amount", amount);
        // Use the string directly

        return postAction(action);
    }

    // ==================== Multi-Signature Operations ====================

    /**
     * Multi-signature operation (aligned with Python SDK's multi_sig).
     * <p>
     * Used for multi-signature accounts to execute operations, requiring signatures
     * from multiple signers.
     * </p>
     *
     * @param multiSigUser Multi-signature account address (42-character hexadecimal
     *                     format)
     * @param innerAction  Inner action (actual operation to be executed)
     * @param signatures   List of all signers' signatures (sorted by address)
     * @param nonce        Random number/timestamp
     * @param vaultAddress Vault address (can be null)
     * @return JSON response
     */
    public JsonNode multiSig(
            String multiSigUser,
            Map<String, Object> innerAction,
            List<Map<String, Object>> signatures,
            long nonce,
            String vaultAddress) {
        // Build multiSig action
        Map<String, Object> multiSigAction = new LinkedHashMap<>();
        multiSigAction.put("type", "multiSig");
        multiSigAction.put("signatureChainId", "0x66eee");
        multiSigAction.put("signatures", signatures);

        // Build payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("multiSigUser", multiSigUser.toLowerCase());
        payload.put("outerSigner", apiWallet.getPrimaryWalletAddress().toLowerCase());
        payload.put("action", innerAction);
        multiSigAction.put("payload", payload);

        // Sign
        Map<String, Object> signature = Signing.signMultiSigAction(
                apiWallet.getCredentials(),
                multiSigAction,
                isMainnet(),
                vaultAddress,
                nonce,
                null // expiresAfter
        );

        // Send request
        return postActionWithSignature(multiSigAction, signature, nonce);
    }

    /**
     * Multi-signature operation (simplified version, using current vaultAddress).
     *
     * @param multiSigUser Multi-signature account address
     * @param innerAction  Inner action
     * @param signatures   Signature list
     * @param nonce        Random number/timestamp
     * @return JSON response
     */
    public JsonNode multiSig(
            String multiSigUser,
            Map<String, Object> innerAction,
            List<Map<String, Object>> signatures,
            long nonce) {
        return multiSig(multiSigUser, innerAction, signatures, nonce, this.vaultAddress);
    }

    /**
     * PerpDeploy Oracle settings (aligned with Python SDK's
     * perp_deploy_set_oracle).
     * <p>
     * Used for Oracle price updates in Builder-deployed perp dex.
     * </p>
     *
     * @param dex             Perp dex name
     * @param oraclePxs       Oracle price Map (coin name -> price string)
     * @param allMarkPxs      Mark price list (each element is Map<coin, price>)
     * @param externalPerpPxs External perpetual price Map (coin name -> price
     *                        string)
     * @return JSON response
     */

    public JsonNode perpDeploySetOracle(
            String dex,
            Map<String, String> oraclePxs,
            List<Map<String, String>> allMarkPxs,
            Map<String, String> externalPerpPxs) {
        // 1. Sort oraclePxs
        List<List<String>> oraclePxsWire = new ArrayList<>();
        if (oraclePxs != null) {
            List<Map.Entry<String, String>> sorted = new ArrayList<>(oraclePxs.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, String> entry : sorted) {
                oraclePxsWire.add(Arrays.asList(entry.getKey(), entry.getValue()));
            }
        }

        // 2. Sort markPxs
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

        // 3. Sort externalPerpPxs
        List<List<String>> externalPerpPxsWire = new ArrayList<>();
        if (externalPerpPxs != null) {
            List<Map.Entry<String, String>> sorted = new ArrayList<>(externalPerpPxs.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, String> entry : sorted) {
                externalPerpPxsWire.add(Arrays.asList(entry.getKey(), entry.getValue()));
            }
        }

        // 4. Construct action
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

    /**
     * EVM BigBlocks switch (aligned with Python SDK's use_big_blocks).
     * <p>
     * Used to enable/disable EVM Big Blocks functionality.
     * </p>
     *
     * @param enable true means enable, false means disable
     * @return JSON response
     */
    public JsonNode useBigBlocks(boolean enable) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "evmUserModify");
        action.put("usingBigBlocks", enable);

        return postAction(action);
    }

    // ==================== C Validator Operations (Professional Features)
    // ====================

    /**
     * C Validator registration (aligned with Python SDK's c_validator_register).
     * <p>
     * Used to register new validator nodes in the Hyperliquid consensus protocol.
     * This is a professional feature for advanced users who want to participate
     * in network validation and earn rewards.
     * </p>
     *
     * <p>
     * <strong>Important considerations:</strong>
     * </p>
     * <ul>
     * <li>Requires significant technical expertise to operate a validator node</li>
     * <li>Validators must maintain high uptime and network connectivity</li>
     * <li>Risks include slashing penalties for malicious or negligent behavior</li>
     * <li>Initial staking amount should be carefully considered</li>
     * <li>Commission rates affect validator competitiveness and earnings</li>
     * </ul>
     *
     * @param nodeIp              Node IP address (publicly accessible endpoint)
     * @param name                Validator name (public identifier)
     * @param description         Validator description (public information)
     * @param delegationsDisabled Whether to disable delegations from other users
     * @param commissionBps       Commission ratio in basis points (1 bps = 0.01%)
     * @param signer              Signer address (responsible for signing blocks)
     * @param unjailed            Whether to unjail (set to false for new
     *                            validators)
     * @param initialWei          Initial staking amount in wei (minimum required
     *                            stake)
     * @return JSON response containing transaction details and validator status
     * @see #cValidatorChangeProfile(String, String, String, boolean, Boolean,
     *      Integer, String)
     * @see #cValidatorUnregister()
     */
    public JsonNode cValidatorRegister(
            String nodeIp,
            String name,
            String description,
            boolean delegationsDisabled,
            int commissionBps,
            String signer,
            boolean unjailed,
            long initialWei) {
        // Construct profile
        Map<String, Object> nodeIpMap = new LinkedHashMap<>();
        nodeIpMap.put("Ip", nodeIp);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("node_ip", nodeIpMap);
        profile.put("name", name);
        profile.put("description", description);
        profile.put("delegations_disabled", delegationsDisabled);
        profile.put("commission_bps", commissionBps);
        profile.put("signer", signer);

        // Build register
        Map<String, Object> register = new LinkedHashMap<>();
        register.put("profile", profile);
        register.put("unjailed", unjailed);
        register.put("initial_wei", initialWei);

        // Build action
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CValidatorAction");
        action.put("register", register);

        return postAction(action);
    }

    /**
     * C Validator change configuration (aligned with Python SDK's
     * c_validator_change_profile).
     * <p>
     * Used to modify validator node configuration information. All parameters can
     * be null, only non-null parameters are updated.
     * </p>
     *
     * @param nodeIp             Node IP address (can be null)
     * @param name               Validator name (can be null)
     * @param description        Validator description (can be null)
     * @param unjailed           Whether to unjail
     * @param disableDelegations Whether to disable delegations (can be null)
     * @param commissionBps      Commission ratio (can be null)
     * @param signer             Signer address (can be null)
     * @return JSON response
     */
    public JsonNode cValidatorChangeProfile(
            String nodeIp,
            String name,
            String description,
            boolean unjailed,
            Boolean disableDelegations,
            Integer commissionBps,
            String signer) {
        // Construct changeProfile
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

        // Build action
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CValidatorAction");
        action.put("changeProfile", changeProfile);

        return postAction(action);
    }

    /**
     * C Validator unregistration (aligned with Python SDK's
     * c_validator_unregister).
     * <p>
     * Used to unregister validator nodes.
     * </p>
     *
     * @return JSON response
     */
    public JsonNode cValidatorUnregister() {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CValidatorAction");
        action.put("unregister", null);

        return postAction(action);
    }

    /**
     * C Signer jail self (aligned with Python SDK's c_signer_jail_self).
     * <p>
     * Used for validators to actively jail their own signers.
     * </p>
     *
     * @return JSON response
     */
    public JsonNode cSignerJailSelf() {
        return cSignerInner("jailSelf");
    }

    /**
     * C Signer unjail self (aligned with Python SDK's c_signer_unjail_self).
     * <p>
     * Used for validators to remove the jailed status of their signers.
     * </p>
     *
     * @return JSON response
     */
    public JsonNode cSignerUnjailSelf() {
        return cSignerInner("unjailSelf");
    }

    /**
     * Internal implementation of C Signer operations.
     *
     * @param variant Operation type (jailSelf or unjailSelf)
     * @return JSON response
     */
    private JsonNode cSignerInner(String variant) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CSignerAction");
        action.put(variant, null);

        return postAction(action);
    }

    /**
     * Noop test operation (aligned with Python SDK's noop).
     * <p>
     * Used to test signatures and network connectivity, without executing any
     * actual operations.
     * </p>
     *
     * @param nonce Random number/timestamp
     * @return JSON response
     */
    public JsonNode noop(long nonce) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "noop");

        // Sign with a custom nonce
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
