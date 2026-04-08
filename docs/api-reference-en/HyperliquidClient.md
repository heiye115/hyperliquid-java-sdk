# HyperliquidClient

**Class:** `io.github.hyperliquid.sdk.HyperliquidClient`

Facade that wires read-only `Info`, one or more alias-bound `Exchange` instances, and a shared HTTP client. Multiple wallets are supported; aliases are case-insensitive and stored in **lowercase**.

## Static methods

| Method | Description |
|--------|-------------|
| `static Builder builder()` | Creates a `Builder` for fluent construction |

## Instance methods — accessors

| Method | Returns | Description |
|--------|---------|-------------|
| `Info getInfo()` | `Info` | Read-only query API |
| `Map<String, Exchange> getExchangesByAlias()` | Immutable snapshot | Alias → `Exchange`, insertion order preserved |
| `List<ApiWallet> getApiWallets()` | Immutable snapshot | Currently registered API wallets |
| `Exchange getExchange()` | `Exchange` | **First** `Exchange` in insertion order; throws `HypeError` if none |
| `Exchange getExchange(String alias)` | `Exchange` | By alias; throws `HypeError` if missing (lists available aliases) |
| `boolean hasWallet(String alias)` | `boolean` | Whether alias exists; `null`/blank → false |
| `Set<String> getAvailableAddresses()` | `Set<String>` | All primary wallet addresses (immutable set) |
| `int getWalletCount()` | `int` | Number of wallets |
| `String getSingleAddress()` | `String` | Primary address of the **first** wallet; throws if empty |

## Instance methods — add/remove wallets at runtime

Duplicate wallets (same alias, same primary address, or same private key) are **ignored** (idempotent).

| Method | Description |
|--------|-------------|
| `HyperliquidClient addApiWallet(ApiWallet apiWallet)` | Adds a wallet; validates and normalizes key and alias |
| `HyperliquidClient addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey)` | Same; alias defaults to normalized primary if omitted |
| `HyperliquidClient addApiWallet(String alias, String primaryWalletAddress, String apiWalletPrivateKey)` | Explicit alias |
| `HyperliquidClient addPrivateKey(String privateKey)` | Key only; primary derived from key; alias = normalized primary |
| `HyperliquidClient addPrivateKey(String alias, String privateKey)` | Key + alias |
| `HyperliquidClient removeWallet(String alias)` | Remove by alias; throws if missing |
| `boolean removeWalletIfExists(String alias)` | Returns true if removed; false if absent; blank alias throws |

Private key rules: non-empty hex, **64 characters** (`0x` allowed), must pass secp256k1 validation, or `HypeError`.

## Builder (`HyperliquidClient.Builder`)

| Method | Description |
|--------|-------------|
| `Builder baseUrl(String baseUrl)` | API root URL; default `Constants.MAINNET_API_URL` |
| `Builder testNetUrl()` | Sets `Constants.TESTNET_API_URL` |
| `Builder timeout(int timeout)` | Connect/read/write timeout in **seconds**; default 10 |
| `Builder skipWs(boolean skipWs)` | If true, WebSocket is not initialized (`Info` WS features throw or no-op) |
| `Builder okHttpClient(OkHttpClient client)` | Custom client; otherwise built from `timeout` |
| `Builder disableAutoWarmUpCache()` | Skips `info.warmUpCache()` on `build()` (default runs it; failure logs warn only) |
| `Builder perpDexs(List<String> perpDexs)` | Preload builder-deployed perp DEX metadata (e.g., `List.of("xyz")` for `xyz:SP500` WebSocket subscriptions) |
| `Builder addPerpDex(String dex)` | Add a single DEX name to the preload list |
| `Builder addPrivateKey(String privateKey)` | Add one wallet |
| `Builder addPrivateKey(String alias, String privateKey)` | Key + alias |
| `Builder addPrivateKeys(List<String> pks)` | Add many keys |
| `Builder addApiWallet(ApiWallet apiWallet)` | Add `ApiWallet` |
| `Builder addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey)` | Primary + key |
| `Builder addApiWallet(String alias, String primaryWalletAddress, String apiWalletPrivateKey)` | Alias + primary + key |
| `Builder addApiWallets(List<ApiWallet> apiWallets)` | Batch add |
| `HyperliquidClient build()` | Normalizes wallets, constructs `Info`/`Exchange`, optional warm-up; bad key → `HypeError` |

## ApiWallet (summary)

**Class:** `io.github.hyperliquid.sdk.model.wallet.ApiWallet`

- Fields: `alias`, `primaryWalletAddress`, `apiWalletPrivateKey`; `Credentials` injected during normalization.
- Constructors: `ApiWallet(alias, primary, pk)`, `ApiWallet(primary, pk)`, `ApiWallet(pk)`.
