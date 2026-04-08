# HyperliquidClient

**类**：`io.github.hyperliquid.sdk.HyperliquidClient`

统一封装：只读 `Info`、一个或多个按别名绑定的 `Exchange`、共享 HTTP 客户端。支持多钱包，别名不区分大小写，存储为**小写**。

## 静态方法

| 方法 | 说明 |
|------|------|
| `static Builder builder()` | 构造 `Builder`，用于链式创建客户端 |

## 实例方法 — 访问器

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `Info getInfo()` | `Info` | 只读查询服务 |
| `Map<String, Exchange> getExchangesByAlias()` | 不可变快照 | 别名 → `Exchange`，插入顺序保留 |
| `List<ApiWallet> getApiWallets()` | 不可变快照 | 当前管理的 API 钱包列表 |
| `Exchange getExchange()` | `Exchange` | 返回**第一个**（按插入顺序）`Exchange`；若无钱包则抛 `HypeError` |
| `Exchange getExchange(String alias)` | `Exchange` | 按别名获取；别名不存在则抛 `HypeError`（提示可用别名） |
| `boolean hasWallet(String alias)` | `boolean` | 别名是否存在；`null` 或空白视为不存在 |
| `Set<String> getAvailableAddresses()` | `Set<String>` | 所有主钱包地址（不可变集） |
| `int getWalletCount()` | `int` | 钱包数量 |
| `String getSingleAddress()` | `String` | **第一个**钱包的主地址；无钱包抛 `HypeError` |

## 实例方法 — 运行时增删钱包

重复钱包（同别名、同主地址或同私钥）会被**忽略**（幂等）。

| 方法 | 说明 |
|------|------|
| `HyperliquidClient addApiWallet(ApiWallet apiWallet)` | 添加钱包；校验并规范化私钥与别名 |
| `HyperliquidClient addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey)` | 同上，无显式别名时别名为规范化后的主地址 |
| `HyperliquidClient addApiWallet(String alias, String primaryWalletAddress, String apiWalletPrivateKey)` | 显式别名 |
| `HyperliquidClient addPrivateKey(String privateKey)` | 仅私钥；主地址从私钥推导；别名为规范化主地址 |
| `HyperliquidClient addPrivateKey(String alias, String privateKey)` | 私钥 + 别名 |
| `HyperliquidClient removeWallet(String alias)` | 按别名移除；不存在抛 `HypeError` |
| `boolean removeWalletIfExists(String alias)` | 存在则移除并返回 `true`；不存在返回 `false`；别名为空抛 `HypeError` |

私钥要求：非空、十六进制、**64 字符**（可带 `0x` 前缀），且能通过椭圆曲线校验，否则抛 `HypeError`。

## Builder（`HyperliquidClient.Builder`）

| 方法 | 说明 |
|------|------|
| `Builder baseUrl(String baseUrl)` | API 根 URL，默认 `Constants.MAINNET_API_URL` |
| `Builder testNetUrl()` | 设为 `Constants.TESTNET_API_URL` |
| `Builder timeout(int timeout)` | 连接/读/写超时（**秒**），默认 10 |
| `Builder skipWs(boolean skipWs)` | `true` 时不初始化 WebSocket（`Info` 内相关能力不可用或会抛错） |
| `Builder okHttpClient(OkHttpClient client)` | 自定义 `OkHttpClient`；未设置则按 `timeout` 构建 |
| `Builder disableAutoWarmUpCache()` | 关闭构建时自动预热缓存（默认 `build()` 会调用 `info.warmUpCache()`，失败仅打 warn，客户端仍可用） |
| `Builder perpDexs(List<String> perpDexs)` | 预加载 builder-deployed 永续 DEX 元数据（如 `List.of("xyz")` 用于订阅 `xyz:SP500` 的 WebSocket） |
| `Builder addPerpDex(String dex)` | 添加单个 DEX 名称到预加载列表 |
| `Builder addPrivateKey(String privateKey)` | 添加一个钱包（私钥） |
| `Builder addPrivateKey(String alias, String privateKey)` | 私钥 + 别名 |
| `Builder addPrivateKeys(List<String> pks)` | 批量添加私钥 |
| `Builder addApiWallet(ApiWallet apiWallet)` | 添加 `ApiWallet` |
| `Builder addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey)` | 主地址 + API 私钥 |
| `Builder addApiWallet(String alias, String primaryWalletAddress, String apiWalletPrivateKey)` | 别名 + 主地址 + 私钥 |
| `Builder addApiWallets(List<ApiWallet> apiWallets)` | 批量添加 |
| `HyperliquidClient build()` | 规范化钱包、创建 `Info`/`Exchange`、可选预热缓存；私钥非法抛 `HypeError` |

## ApiWallet（简要）

**类**：`io.github.hyperliquid.sdk.model.wallet.ApiWallet`

- 字段：`alias`、`primaryWalletAddress`、`apiWalletPrivateKey`；`Credentials` 由客户端在规范化时注入。
- 构造：`ApiWallet(alias, primary, pk)`、`ApiWallet(primary, pk)`、`ApiWallet(pk)`。
