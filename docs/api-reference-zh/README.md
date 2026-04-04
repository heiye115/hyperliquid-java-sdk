# hyperliquid-java-sdk API 中文文档

**其他语言**：[English](../api-en/README.md)

本文档对应 SDK 版本：**0.2.21**（见项目根目录 `pom.xml`）。

## 包与入口

| 包路径 | 说明 |
|--------|------|
| `io.github.hyperliquid.sdk` | `HyperliquidClient` 统一入口 |
| `io.github.hyperliquid.sdk.apis` | `Info`（只读查询）、`Exchange`（交易与签名操作） |
| `io.github.hyperliquid.sdk.websocket` | `WebsocketManager`（WebSocket；通常通过 `Info` 间接使用） |
| `io.github.hyperliquid.sdk.utils` | `Constants` 等工具类 |

## 网络地址

| 常量 | 值 |
|------|-----|
| `Constants.MAINNET_API_URL` | `https://api.hyperliquid.xyz` |
| `Constants.TESTNET_API_URL` | `https://api.hyperliquid-testnet.xyz` |

## 文档索引

| 文档 | 内容 |
|------|------|
| [HyperliquidClient.md](./HyperliquidClient.md) | 客户端门面、多钱包、`Builder` 配置 |
| [Info.md](./Info.md) | `/info` 查询、缓存、K 线、用户数据、WebSocket 订阅封装 |
| [Exchange.md](./Exchange.md) | `/exchange` 下单、撤单、划转、杠杆、协议与高级操作 |
| [WebSocket.md](./WebSocket.md) | `WebsocketManager` 直连用法与订阅说明 |

## 典型用法

```java
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.utils.Constants;

HyperliquidClient client = HyperliquidClient.builder()
    .baseUrl(Constants.MAINNET_API_URL)  // 或 .testNetUrl()
    .addPrivateKey("0x...")              // API 钱包私钥，64 位十六进制（可带 0x）
    .timeout(10)
    .build();

var info = client.getInfo();
var exchange = client.getExchange();
```

- **只读数据**：使用 `client.getInfo()` 返回的 `Info`。
- **需要签名的操作**：使用 `client.getExchange()` 或 `client.getExchange("别名")` 返回的 `Exchange`。
- **禁用 WebSocket**（仅 REST）：`HyperliquidClient.builder().skipWs(true)...`。

## 与官方 API 的对应关系

- **REST**：`Info` 主要对应 `POST /info`；`Exchange` 主要对应 `POST /exchange`（L1 动作签名后提交）。
- **WebSocket**：`wss://.../ws`（由 `baseUrl` 推导），订阅体字段与官方文档一致；SDK 会对部分 `coin` 字段做规范化（见 [WebSocket.md](./WebSocket.md)）。
