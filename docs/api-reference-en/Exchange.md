# Exchange

**Class:** `io.github.hyperliquid.sdk.apis.Exchange`

Maps to Hyperliquid **`POST /exchange`**: L1 actions signed with `Signing.signL1Action` (and related helpers), then posted. Constructed as `new Exchange(HypeHttpClient, ApiWallet, Info)` inside the SDK; obtain instances via `HyperliquidClient`.

## Account and request-level settings

| Method | Description |
|--------|-------------|
| `String getVaultAddress()` | Current vault address (may be null) |
| `void setVaultAddress(String vaultAddress)` | For subaccount/vault flows: affects `vaultAddress` in payload (exceptions below) |
| `void setExpiresAfter(Long expiresAfter)` | Default action expiry (ms); per-call `postAction(action, expiresAfter)` or order fields can override |
| `void setDefaultSlippage(String slippage)` | Global slippage string, e.g. `"0.05"` = 5% |
| `void setDefaultSlippage(String coin, String slippage)` | Per-coin override |

**`vaultAddress` rules (Python-aligned):** `usdClassTransfer` and `sendAsset` **omit** `vaultAddress`; if vault equals signer primary, it is also omitted.

## Core submission

| Method | Description |
|--------|-------------|
| `JsonNode postAction(Map<String, Object> action)` | L1 sign + `POST /exchange`; nonce = ms timestamp |
| `JsonNode postAction(Map<String, Object> action, Long expiresAfter)` | Optional expiry; orders may use `OrderRequest.getExpiresAfter()` |

User-signed actions (`signKnownUserSignedAction`) use internal `postActionWithSignature`, not the same path as plain `signL1Action` for all fields.

## Orders and positions

| Method | Description |
|--------|-------------|
| `Order order(OrderRequest req)` | Single order; perp runs `prepareRequest` + size/price rounding |
| `Order order(OrderRequest req, Map<String, Object> builder)` | Optional **builder**: only `b` (address), `f` (non-negative integer fee) |
| `BulkOrder bulkOrders(List<OrderRequest> requests)` | Batch; default grouping `"na"` from `buildOrderAction` unless overridden |
| `BulkOrder bulkOrders(List<OrderRequest> requests, Map<String, Object> builder, String grouping)` | `grouping`: `"na"` \| `"normalTpsl"` \| `"positionTpsl"` |
| `BulkOrder bulkOrders(OrderGroup orderGroup)` | Grouping from `OrderGroup` |
| `BulkOrder bulkOrders(OrderGroup orderGroup, Map<String, Object> builder)` | For `positionTpsl`, may auto-fill side/size from position |
| `Cancel cancel(String coinName, long oid)` | Cancel by OID |
| `Cancel cancels(List<CancelRequest> requests)` | Batch cancel |
| `Cancel cancelByCloid(String coinName, Cloid cloid)` | Cancel by client order id |
| `Cancel cancelByCloids(List<CancelByCloidRequest> requests)` | Batch by cloid |
| `ModifyOrder modifyOrder(ModifyOrderRequest request)` | Modify order |
| `ModifyOrder modifyOrder(ModifyOrderRequest request, Long expiresAfter)` | With expiry |
| `ModifyOrder modifyOrders(List<ModifyOrderRequest> requests)` | `batchModify` |
| `ModifyOrder modifyOrders(List<ModifyOrderRequest> requests, Long expiresAfter)` | |

**Note:** Single `order()` auto-fills some close-position and trigger defaults; **`bulkOrders` batch closes require explicit side/size** (see source). `positionTpsl` is the exception.

| Method | Description |
|--------|-------------|
| `UpdateLeverage updateLeverage(String coinName, boolean crossed, int leverage)` | Leverage and cross/isolated |
| `JsonNode updateIsolatedMargin(String amount, String coinName)` | Isolated margin update; `amount` string parsed to internal USD int |

## Closing helpers and slippage

| Method | Description |
|--------|-------------|
| `String computeSlippagePrice(String coin, boolean isBuy, String slippage)` | Uses `getCachedAllMids` (DEX from `dex:` prefix) and mid |
| `Order closePositionMarket(String coin)` | Market-close full position (`OrderRequest.Close.marketAll`) |
| `Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid)` | Partial, slippage, cloid |
| `Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid, Map<String, Object> builder)` | With builder |
| `Order closePositionLimit(Tif tif, String coin, String limitPx, Cloid cloid)` | Limit close, size = |position| |
| `BulkOrder closeAllPositions()` | One IOC market close per coin via `bulkOrders` (`na`) |

## Other trading

| Method | Description |
|--------|-------------|
| `JsonNode scheduleCancel(Long timeMs)` | `scheduleCancel`; null = cancel scheduled cancel immediately |

## Transfers, approvals, account structure

| Method                                                                                                                                | Description |
|---------------------------------------------------------------------------------------------------------------------------------------|-------------|
| `JsonNode usdTransfer(String amount, String destination)`                                                                             | USDC send (user-signed) |
| `JsonNode spotTransfer(String amount, String destination, String token)`                                                              | Spot token send (user-signed) |
| `JsonNode withdrawFromBridge(String amount, String destination)`                                                                      | `withdraw3` bridge withdraw (user-signed) |
| `JsonNode usdClassTransfer(boolean toPerp, String amount)`                                                                            | Spot ↔ perp USDC; if `vaultAddress` set, amount string may append ` subaccount:0x...` |
| `JsonNode sendAsset(String destination, String sourceDex, String destinationDex, String token, String amount, String fromSubAccount)` | Cross-DEX send; `fromSubAccount` null or vault |
| `ApproveBuilderFee approveBuilderFee(String builder, String maxFeeRate)`                                                              | Approve builder fee cap; returns `io.github.hyperliquid.sdk.model.approve.ApproveBuilderFee` |
| `JsonNode setReferrer(String code)`                                                                                                   | Referral bind; uses `signL1Action` payload with `expiresAfter` |
| `JsonNode createSubAccount(String name)`                                                                                              | Create subaccount |
| `JsonNode subAccountTransfer(String subAccountUser, boolean isDeposit, long usd)`                                                     | Main ↔ sub USDC; **micro USDC** (e.g. 1_000_000 = 1 USDC) |
| `JsonNode subAccountSpotTransfer(String subAccountUser, boolean isDeposit, String token, String amount)`                              | Spot subaccount transfer |
| `JsonNode vaultTransfer(String vaultAddress, boolean isDeposit, long usd)`                                                            | Vault deposit/withdraw; micro units |
| `JsonNode tokenDelegate(String validator, long wei, boolean isUndelegate)`                                                            | HYPE stake / unstake |
| `JsonNode convertToMultiSigUser(String signersJson)`                                                                                  | Convert to multisig |
| `ApproveAgent approveAgent(String name)`                                                                                              | Random agent key + `approveAgent`; returns new agent key/address |
| `JsonNode agentEnableDexAbstraction()`                                                                                                | Enable agent-side DEX abstraction |
| `JsonNode agentSetAbstraction(String abstraction)`                                                                                    | Agent abstraction mode (`"u"`/`"p"`/`"i"` etc., Python-aligned) |
| `JsonNode userDexAbstraction(String user, boolean enabled)`                                                                           | User-side DEX abstraction (user-signed) |
| `UserSetAbstraction userSetAbstraction(String user, UserAbstractionMode mode)`                                                        | User abstraction (`signUserSetAbstractionAction`) |
| `UserSetAbstraction userSetAbstraction(String user, UserAbstractionMode mode, String signatureChainId)`                               | Optional EIP-712 `signatureChainId` hex string |

## Spot deploy (`spotDeploy`)

All use `type: spotDeploy` with different inner keys; return `JsonNode`.

| Method | Purpose |
|--------|---------|
| `spotDeployRegisterToken(...)` | Register token |
| `spotDeployUserGenesis(...)` | Genesis allocation |
| `spotDeployEnableFreezePrivilege(int token)` | Enable freeze privilege |
| `spotDeployRevokeFreezePrivilege(int token)` | Revoke freeze privilege |
| `spotDeployFreezeUser(int token, String user, boolean freeze)` | Freeze/unfreeze user |
| `spotDeployEnableQuoteToken(int token)` | Enable as quote token |
| `spotDeployGenesis(int token, String maxSupply, boolean noHyperliquidity)` | Finalize genesis |
| `spotDeployRegisterSpot(int baseToken, int quoteToken)` | Register pair |
| `spotDeployRegisterHyperliquidity(...)` | Hyperliquidity |
| `spotDeploySetDeployerTradingFeeShare(int token, String share)` | Deployer fee share |

## Perp deploy

| Method | Description |
|--------|-------------|
| `JsonNode perpDeployRegisterAsset(String dex, Integer maxGas, String coin, int szDecimals, String oraclePx, int marginTableId, boolean onlyIsolated, Map<String, Object> schema)` | Register perp asset; optional `schema` keys `fullName`, `collateralToken`, `oracleUpdater` |
| `JsonNode perpDeploySetOracle(String dex, Map<String, String> oraclePxs, List<Map<String, String>> allMarkPxs, Map<String, String> externalPerpPxs)` | Oracle update; maps sorted by key when wired |

## Multisig and misc

| Method | Description |
|--------|-------------|
| `JsonNode multiSig(String multiSigUser, Map<String, Object> innerAction, List<Map<String, Object>> signatures, long nonce, String vaultAddress)` | Multisig execution |
| `JsonNode multiSig(String multiSigUser, Map<String, Object> innerAction, List<Map<String, Object>> signatures, long nonce)` | Uses current `vaultAddress` |
| `JsonNode useBigBlocks(boolean enable)` | `evmUserModify` / `usingBigBlocks` |
| `JsonNode noop(long nonce)` | No-op test with custom nonce L1 signature |

## C Validator / C Signer (advanced)

| Method | Description |
|--------|-------------|
| `JsonNode cValidatorRegister(...)` | Validator registration (see source for parameters) |
| `JsonNode cValidatorChangeProfile(...)` | Update validator profile |
| `JsonNode cValidatorUnregister()` | Unregister |
| `JsonNode cSignerJailSelf()` | `CSignerAction` / `jailSelf` |
| `JsonNode cSignerUnjailSelf()` | `unjailSelf` |

## Asset ID resolution

- Perp / general: `ensureAssetId` accepts **positive integer strings** as raw ids, or resolves names via `Info.nameToAsset` (including `dex:COIN`).
- Spot orders: `resolveSpotAssetId` accepts numeric strings or symbols (e.g. `WETH/USDC`); id must be **≥ 10000**.
