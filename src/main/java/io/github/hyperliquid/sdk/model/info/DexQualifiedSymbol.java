package io.github.hyperliquid.sdk.model.info;

/**
 * Parsed representation of a dex-qualified symbol.
 *
 * @param dex  Perp dex name
 * @param coin Symbol name inside the dex
 */
public record DexQualifiedSymbol(String dex, String coin) {
}
