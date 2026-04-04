# hyperliquid-java-sdk API Reference (English)

**Other languages:** [中文](../api-zh/README.md)

This documentation matches SDK version **0.2.21** (see the root `pom.xml`).

## Packages and entry points

| Package | Role |
|---------|------|
| `io.github.hyperliquid.sdk` | `HyperliquidClient` facade |
| `io.github.hyperliquid.sdk.apis` | `Info` (read-only queries), `Exchange` (trading and signed actions) |
| `io.github.hyperliquid.sdk.websocket` | `WebsocketManager` (usually used via `Info`) |
| `io.github.hyperliquid.sdk.utils` | `Constants` and utilities |

## Network endpoints

| Constant | Value |
|----------|--------|
| `Constants.MAINNET_API_URL` | `https://api.hyperliquid.xyz` |
| `Constants.TESTNET_API_URL` | `https://api.hyperliquid-testnet.xyz` |

## Table of contents

| File | Topics |
|------|--------|
| [HyperliquidClient.md](./HyperliquidClient.md) | Client facade, multi-wallet, `Builder` |
| [Info.md](./Info.md) | `POST /info`, caches, candles, user queries, WebSocket helpers |
| [Exchange.md](./Exchange.md) | `POST /exchange`, orders, transfers, leverage, protocol actions |
| [WebSocket.md](./WebSocket.md) | `WebsocketManager` usage and subscriptions |

## Typical usage

```java
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.utils.Constants;

HyperliquidClient client = HyperliquidClient.builder()
    .baseUrl(Constants.MAINNET_API_URL)  // or .testNetUrl()
    .addPrivateKey("0x...")              // API wallet key: 64 hex chars (0x optional)
    .timeout(10)
    .build();

var info = client.getInfo();
var exchange = client.getExchange();
```

- **Read-only data:** use `Info` from `client.getInfo()`.
- **Signed actions:** use `Exchange` from `client.getExchange()` or `client.getExchange("alias")`.
- **REST only (no WebSocket):** `HyperliquidClient.builder().skipWs(true)...`.

## Mapping to official APIs

- **REST:** `Info` → mainly `POST /info`; `Exchange` → `POST /exchange` (L1 actions signed then submitted).
- **WebSocket:** `wss://.../ws` (derived from `baseUrl`); subscription payloads follow the official schema; the SDK may normalize some `coin` fields (see [WebSocket.md](./WebSocket.md)).
