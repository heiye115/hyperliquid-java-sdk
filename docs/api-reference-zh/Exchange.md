# Exchange

**类**：`io.github.hyperliquid.sdk.apis.Exchange`

对应 Hyperliquid **`POST /exchange`**：对 L1 action 使用 `Signing.signL1Action` 等签名后提交。构造器为包级/框架使用：`new Exchange(HypeHttpClient, ApiWallet, Info)`，通常通过 `HyperliquidClient` 获取实例。

## 账户与请求级参数

| 方法 | 说明 |
|------|------|
| `String getVaultAddress()` | 当前设置的 vault 地址（可为 null） |
| `void setVaultAddress(String vaultAddress)` | 子账户/金库场景：非主钱包地址时，参与 `vaultAddress` 有效载荷字段（部分 action 例外，见下） |
| `void setExpiresAfter(Long expiresAfter)` | 全局默认 action 过期时间（毫秒）；单请求可用 `postAction(action, expiresAfter)` 或订单内覆盖 |
| `void setDefaultSlippage(String slippage)` | 全局默认滑点字符串，如 `"0.05"` 表示 5% |
| `void setDefaultSlippage(String coin, String slippage)` | 按币种覆盖默认滑点 |

**`vaultAddress` 规则（与 Python 对齐）**：`usdClassTransfer`、`sendAsset` **不**附带 `vaultAddress`；若设置的 vault 与签名主地址相同，则请求中也不传 vault。

## 核心发送

| 方法 | 说明 |
|------|------|
| `JsonNode postAction(Map<String, Object> action)` | L1 签名 + `POST /exchange`；nonce 为毫秒时间戳 |
| `JsonNode postAction(Map<String, Object> action, Long expiresAfter)` | 可覆盖过期时间；订单类 action 可与 `OrderRequest.getExpiresAfter()` 等配合 |

用户签名类（EIP-712 `signKnownUserSignedAction`）通过内部 `postActionWithSignature` 提交，不经过 `signL1Action` 的同一分支。

## 订单与持仓

| 方法 | 说明 |
|------|------|
| `Order order(OrderRequest req)` | 单笔下单；永续会走 `prepareRequest`（市价开仓滑价、平仓推断、触发单默认价等）+ 数量/价格精度格式化 |
| `Order order(OrderRequest req, Map<String, Object> builder)` | 可选 **builder**：仅保留官方字段 `b`（builder 地址）、`f`（非负整数 fee） |
| `BulkOrder bulkOrders(List<OrderRequest> requests)` | 批量，`grouping` 默认由内部 `buildOrderAction` 设为 `"na"`，若再传 `grouping` 会覆盖 |
| `BulkOrder bulkOrders(List<OrderRequest> requests, Map<String, Object> builder, String grouping)` | `grouping`：`"na"` \| `"normalTpsl"` \| `"positionTpsl"` |
| `BulkOrder bulkOrders(OrderGroup orderGroup)` | 从 `OrderGroup` 推断 grouping |
| `BulkOrder bulkOrders(OrderGroup orderGroup, Map<String, Object> builder)` | `positionTpsl` 且缺方向/数量时，会按持仓**自动补全** |
| `Cancel cancel(String coinName, long oid)` | 按 OID 撤单 |
| `Cancel cancels(List<CancelRequest> requests)` | 批量撤单 |
| `Cancel cancelByCloid(String coinName, Cloid cloid)` | 按客户端订单 ID 撤单 |
| `Cancel cancelByCloids(List<CancelByCloidRequest> requests)` | 批量按 cloid |
| `ModifyOrder modifyOrder(ModifyOrderRequest request)` | 改单 |
| `ModifyOrder modifyOrder(ModifyOrderRequest request, Long expiresAfter)` | 改单 + 过期 |
| `ModifyOrder modifyOrders(List<ModifyOrderRequest> requests)` | 批量改单 `batchModify` |
| `ModifyOrder modifyOrders(List<ModifyOrderRequest> requests, Long expiresAfter)` | |

**注意**：单笔 `order()` 对「市价平仓占位」「限价平仓方向」等有自动推断；**`bulkOrders` 批量平仓场景要求调用方自行写全方向与数量**（与源码注释一致）。`positionTpsl` 分组例外，可由 SDK 推断。

| 方法 | 说明 |
|------|------|
| `UpdateLeverage updateLeverage(String coinName, boolean crossed, int leverage)` | 修改杠杆与全仓/逐仓 |
| `JsonNode updateIsolatedMargin(String amount, String coinName)` | 逐仓保证金调整；`amount` 为字符串数字，内部转为 USD 整数单位 |

## 便捷平仓与滑点

| 方法 | 说明 |
|------|------|
| `String computeSlippagePrice(String coin, boolean isBuy, String slippage)` | 基于 `getCachedAllMids`（按 `dex:` 前缀选 dex）与中间价计算滑点后价格 |
| `Order closePositionMarket(String coin)` | 市价平掉该币全部仓位（`OrderRequest.Close.marketAll`） |
| `Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid)` | 可部分平仓、自定义滑点、cloid |
| `Order closePositionMarket(String coin, String sz, String slippage, Cloid cloid, Map<String, Object> builder)` | 带 builder |
| `Order closePositionLimit(Tif tif, String coin, String limitPx, Cloid cloid)` | 限价平仓，数量取绝对持仓 |
| `BulkOrder closeAllPositions()` | 遍历账户持仓，逐币市价平仓；使用批量 `bulkOrders`（`na`） |

## 其他交易相关

| 方法 | 说明 |
|------|------|
| `JsonNode scheduleCancel(Long timeMs)` | `scheduleCancel`；`null` 表示立即取消计划 |

## 转账、授权与账户结构

| 方法 | 说明 |
|------|------|
| `JsonNode usdTransfer(String amount, String destination)` | USDC 转账（用户签名） |
| `JsonNode spotTransfer(String amount, String destination, String token)` | 现货代币转账（用户签名） |
| `JsonNode withdrawFromBridge(String amount, String destination)` | `withdraw3` 跨链提款（用户签名） |
| `JsonNode usdClassTransfer(boolean toPerp, String amount)` | Spot ↔ Perp USDC；若设置了 `vaultAddress`，金额字符串会附加 ` subaccount:0x...`（与 Python 一致） |
| `JsonNode sendAsset(String destination, String sourceDex, String destinationDex, String token, String amount, String fromSubAccount)` | 跨 DEX 转资产；`fromSubAccount` 可 null，否则可用当前 `vaultAddress` |
| `ApproveBuilderFee approveBuilderFee(String builder, String maxFeeRate)` | 授权 builder 费率上限；返回类型为 `io.github.hyperliquid.sdk.model.approve.ApproveBuilderFee` |
| `JsonNode setReferrer(String code)` | 绑定推荐码；使用 `signL1Action` 路径，载荷含 `expiresAfter` |
| `JsonNode createSubAccount(String name)` | 创建子账户 |
| `JsonNode subAccountTransfer(String subAccountUser, boolean isDeposit, long usd)` | 主子账户 USDC；`usd` 为 **micro USDC**（如 1_000_000 = 1 USDC） |
| `JsonNode subAccountSpotTransfer(String subAccountUser, boolean isDeposit, String token, String amount)` | 子账户现货划转 |
| `JsonNode vaultTransfer(String vaultAddress, boolean isDeposit, long usd)` | 金库存取；`usd` 为 micro 单位 |
| `JsonNode tokenDelegate(String validator, long wei, boolean isUndelegate)` | HYPE 质押/解质押 |
| `JsonNode convertToMultiSigUser(String signersJson)` | 转为多签账户 |
| `ApproveAgentResult approveAgent(String name)` | 随机生成 agent 私钥，用户签名 `approveAgent`；返回 `ApproveAgentResult`（含新 agent 私钥与地址） |
| `JsonNode agentEnableDexAbstraction()` | 启用 Agent 侧 Dex 抽象 |
| `JsonNode agentSetAbstraction(String abstraction)` | 设置 Agent 抽象模式（如与 Python 一致的 `"u"`/`"p"`/`"i"`） |
| `JsonNode userDexAbstraction(String user, boolean enabled)` | 用户侧 Dex 抽象开关（用户签名） |
| `UserSetAbstraction userSetAbstraction(String user, UserAbstractionMode mode)` | 用户抽象（`signUserSetAbstractionAction`） |
| `UserSetAbstraction userSetAbstraction(String user, UserAbstractionMode mode, String signatureChainId)` | 可选 EIP-712 `signatureChainId`（十六进制链 ID 字符串） |

## Spot 部署（spotDeploy）

均为 `type: spotDeploy` 下不同子字段，返回 `JsonNode`。

| 方法 | 用途 |
|------|------|
| `spotDeployRegisterToken(...)` | 注册新 token |
| `spotDeployUserGenesis(...)` | 创世分配 |
| `spotDeployEnableFreezePrivilege(int token)` | 冻结权限 |
| `spotDeployRevokeFreezePrivilege(int token)` | 撤销冻结权限 |
| `spotDeployFreezeUser(int token, String user, boolean freeze)` | 冻结/解冻用户 |
| `spotDeployEnableQuoteToken(int token)` | 启用为报价 token |
| `spotDeployGenesis(int token, String maxSupply, boolean noHyperliquidity)` | 完成 genesis |
| `spotDeployRegisterSpot(int baseToken, int quoteToken)` | 注册交易对 |
| `spotDeployRegisterHyperliquidity(int spot, double startPx, double orderSz, int nOrders, Integer nSeededLevels)` | Hyperliquidity |
| `spotDeploySetDeployerTradingFeeShare(int token, String share)` | 部署者分成 |

## Perp 部署与协议

| 方法 | 说明 |
|------|------|
| `JsonNode perpDeployRegisterAsset(String dex, Integer maxGas, String coin, int szDecimals, String oraclePx, int marginTableId, boolean onlyIsolated, Map<String, Object> schema)` | 注册永续资产；`schema` 可含 `fullName`、`collateralToken`、`oracleUpdater` |
| `JsonNode perpDeploySetOracle(String dex, Map<String, String> oraclePxs, List<Map<String, String>> allMarkPxs, Map<String, String> externalPerpPxs)` | Oracle 更新；各 Map 按键排序后编码 |

## 多签与杂项

| 方法 | 说明 |
|------|------|
| `JsonNode multiSig(String multiSigUser, Map<String, Object> innerAction, List<Map<String, Object>> signatures, long nonce, String vaultAddress)` | 多签执行 |
| `JsonNode multiSig(String multiSigUser, Map<String, Object> innerAction, List<Map<String, Object>> signatures, long nonce)` | 使用当前 `vaultAddress` |
| `JsonNode useBigBlocks(boolean enable)` | `evmUserModify` / `usingBigBlocks` |
| `JsonNode noop(long nonce)` | 空操作测签名；使用自定义 `nonce` 的 L1 签名 |

## C Validator / C Signer（高级）

| 方法 | 说明 |
|------|------|
| `JsonNode cValidatorRegister(...)` | 验证者注册（参数见源码：nodeIp、commissionBps、initialWei 等） |
| `JsonNode cValidatorChangeProfile(...)` | 修改验证者资料 |
| `JsonNode cValidatorUnregister()` | 注销 |
| `JsonNode cSignerJailSelf()` | `CSignerAction` / `jailSelf` |
| `JsonNode cSignerUnjailSelf()` | `unjailSelf` |

## 资产 ID 解析说明

- 永续/通用：`ensureAssetId` 支持**正整数字符串**直接作为 asset id，或通过 `Info.nameToAsset` 解析名称（含 `dex:COIN`）。
- 现货下单：`resolveSpotAssetId` 支持数字字符串或符号（如 `WETH/USDC`），且 asset id 必须 **≥ 10000**。
