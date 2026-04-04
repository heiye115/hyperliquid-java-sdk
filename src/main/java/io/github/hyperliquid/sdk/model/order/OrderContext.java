package io.github.hyperliquid.sdk.model.order;

/**
 * Resolved context for a single order before wire encoding: global asset id plus the prepared {@link OrderRequest}.
 *
 * @param assetId Globally unique asset identifier used in L1 order actions (perp index, spot id, etc.)
 * @param request Order request after preprocessing (size/price formatting, market-close inference, etc.)
 */
public record OrderContext(int assetId, OrderRequest request) {
}
