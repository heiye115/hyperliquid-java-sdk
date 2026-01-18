**语言切换：** [English](README.md)

# Hyperliquid Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.github.heiye115/hyperliquid-java-sdk.svg)](https://central.sonatype.com/artifact/io.github.heiye115/hyperliquid-java-sdk)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-21%2B-orange)](pom.xml)
[![Stars](https://img.shields.io/github/stars/heiye115/hyperliquid-java-sdk?style=social)](https://github.com/heiye115/hyperliquid-java-sdk)
[![Issues](https://img.shields.io/github/issues/heiye115/hyperliquid-java-sdk)](https://github.com/heiye115/hyperliquid-java-sdk/issues)

一个类型安全、功能完善的 Java SDK，用于 Hyperliquid 交易与实时行情数据。

## 🎯 项目概述

本 SDK 提供了一个与 Hyperliquid 去中心化交易所交互的纯 Java 客户端，帮助你轻松构建交易机器人、数据分析流水线和平台集成应用。

### ✨ 功能亮点

- **🚀 高性能:** 针对低延迟交易进行优化，具备高效的数据处理能力。
- **🛡️ 类型安全:** 流式构建器和强类型模型可防止常见错误，并增强代码的清晰度。
- **🔐 安全设计:** 稳健的 EIP-712 签名和清晰的钱包管理模式。
- **💼 多钱包管理:** 无缝管理和切换多个交易账户（主钱包和 API 钱包）。
- **🌐 强大的 WebSocket:** 支持自动重连、指数退避和类型安全的实时数据订阅。
- **🧩 流畅直观的 API:** 简洁、现代的 API 设计，旨在提供卓越的开发者体验。

## 🛠️ 安装部署

- **环境要求**: JDK `21+`，Maven 或 Gradle。
- **Maven**:

```xml
<dependency>
    <groupId>io.github.heiye115</groupId>
    <artifactId>hyperliquid-java-sdk</artifactId>
    <version>0.2.12</version> <!-- 建议替换为最新版本 -->
</dependency>
```

- **Gradle (Groovy)**:

```gradle
implementation 'io.github.heiye115:hyperliquid-java-sdk:0.2.12' // 建议替换为最新版本
```

## ⚡ 5分钟快速体验

通过这个完整的、可运行的示例，在几分钟内快速上手。

**前置条件:**

1. 拥有一个 Hyperliquid 账户。在本示例中，请使用 **测试网 (Testnet)**。
2. 获取您钱包的私钥。
3. **重要提示:** 请安全地存储您的私钥。推荐的方式是使用环境变量。

```bash
export HYPERLIQUID_TESTNET_PRIVATE_KEY="0x您的私钥"
```

### 可运行示例

此示例将演示如何：

1. 构建客户端。
2. 查询市场数据 (`l2Book`)。
3. 下一个限价单 (`order`)。
4. 处理潜在的 API 错误 (`HypeError`)。

```java
public class QuickStart {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuickStart.class);

    public static void main(String[] args) {
        // **1. 推荐：使用 API 钱包以获得更好的安全性**
        // **API 钱包: 由主钱包授权的子钱包，权限有限，不暴露主私钥**
        // **主私钥: 直接使用主钱包私钥，拥有完全控制权，风险较高**
        String primaryWalletAddress = "";  // **主钱包地址**
        String apiWalletPrivateKey = "";   // **API 钱包私钥**

        // **2. 使用 API 钱包构建客户端 (推荐)**
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl() // **使用测试网环境**
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey) // **添加您的 API 钱包**
                .build(); // **构建客户端实例**

        // **备选方案: 使用主私钥构建客户端 (生产环境不推荐)**
        // String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        // HyperliquidClient client = HyperliquidClient.builder()
        //         .testNetUrl() // **使用测试网环境**
        //         .addPrivateKey(pk) // **添加您的主私钥**
        //         .build(); // **构建客户端实例**

        Info info = client.getInfo(); // **获取 Info 客户端实例**
        Exchange exchange = client.getExchange(); // **获取已添加钱包的交易实例**

        // 3. 查询市场数据: 获取 "ETH" 的 L2 订单簿
        try {
            LOGGER.info("正在查询 ETH 的 L2 订单簿..."); // **记录日志：开始查询**
            L2Book l2Book = info.l2Book("ETH"); // **调用 Info API 查询 ETH 的 L2 订单簿**
            // 打印前3档的买卖盘
            LOGGER.info("成功获取 {} 的 L2 订单簿:", l2Book.getCoin()); // **记录日志：查询成功**
            l2Book.getLevels().get(0).subList(0, 3).forEach(level -> // **遍历卖盘前3档**
                    LOGGER.info("  卖盘 - 价格: {}, 数量: {}", level.getPx(), level.getSz()) // **打印卖盘价格和数量**
            );
            l2Book.getLevels().get(1).subList(0, 3).forEach(level -> // **遍历买盘前3档**
                    LOGGER.info("  买盘 - 价格: {}, 数量: {}", level.getPx(), level.getSz()) // **打印买盘价格和数量**
            );
        } catch (HypeError e) { // **捕获 HypeError 异常**
            LOGGER.error("查询 L2 订单簿失败。代码: {}, 消息: {}", e.getCode(), e.getMessage()); // **记录错误日志**
        }

        // 4. 执行交易: 创建一个 ETH 的限价买单
        try {
            LOGGER.info("正在下一个 ETH 的限价买单..."); // **记录日志：开始下单**
            // 创建一个限价买单，以 $1500 的价格购买 0.01 ETH
            OrderRequest orderRequest = OrderRequest.builder() // **使用 OrderRequest 构建器**
                    .perp("ETH") // **指定交易品种为 ETH 永续合约**
                    .buy("0.01") // **买入方向，数量为 0.01**
                    .limitPrice("1500") // **设置限价为 $1500**
                    .gtc() // **设置订单类型为 Good Till Cancel (GTC)，订单在未成交前一直有效**
                    .build(); // **构建订单请求对象**

            Order order = exchange.order(orderRequest); // **调用 Exchange API 下单**
            LOGGER.info("下单成功。响应: {}", JSONUtil.writeValueAsString(order)); // **记录日志：下单成功，并打印响应**

        } catch (HypeError | JsonProcessingException e) { // **捕获 HypeError 异常**
            // 处理特定错误的示例，例如：保证金不足
            LOGGER.error("下单失败。消息: {}", e.getMessage(), e);
        }
    }
}
```

## 📚 核心功能指南

### 客户端配置

`HyperliquidClient.builder()` 提供了流式 API 用于配置。

```java
// 完整配置示例
HyperliquidClient client = HyperliquidClient.builder()
        // 选择网络 (或提供自定义 URL)
        .testNetUrl() // 或 .baseUrl("https://api.hyperliquid.xyz")
        
        // --- 钱包管理 ---
        // 方案一: 添加单个主私钥
        .addPrivateKey("0x您的主私钥")
        
        // 方案二: 添加多个 API 钱包 (为安全起见，推荐此方式)
        // API 钱包是您主钱包授权的子钱包
        .addApiWallet("0x您的主钱包地址1", "0x您的API私钥1")
        .addApiWallet("0x您的主钱包地址2", "0x您的API私钥2")
        
        // --- 性能优化 ---
        // 启动时会自动预热缓存，仅在对启动耗时极其敏感时才建议关闭
        // .disableAutoWarmUpCache()
        
        // --- (可选)网络设置 ---
        // 设置自定义HTTP客户端
        .okHttpClient(customClient)
        // 为底层 OkHttpClient 设置统一超时（单位：秒，作用于连接/读/写）
        .timeout(15)
        
        // 构建不可变的客户端实例
        .build();

// 为不同钱包获取交易实例
Exchange exchange1 = client.getExchange("0x您的主钱包地址1");
Exchange exchange2 = client.getExchange("0x您的主钱包地址2");
```

### 查询数据 (`Info` API)

`Info` API 提供对所有公开市场数据和私有用户数据的访问。

**获取用户状态:**

```java
UserState userState = info.userState("0x您的地址");
LOGGER.info("总保证金使用量: {}", userState.getMarginSummary().getTotalMarginUsed());
```

**获取未结订单:**

```java
List<OpenOrder> openOrders = info.openOrders("0x您的地址");
LOGGER.info("用户有 {} 个未结订单。", openOrders.size());
```

**获取市场元数据:**

```java
Meta meta = info.meta();
// 查找特定资产的详细信息
meta.getUniverse().stream()
    .filter(asset -> "ETH".equals(asset.getName()))
    .findFirst()
    .ifPresent(ethAsset -> LOGGER.info("ETH 的最大杠杆: {}", ethAsset.getMaxLeverage()));
```

### 交易 (`Exchange` API)

`Exchange` API 处理所有需要签名的状态变更操作。

**使用 `OrderRequest.Builder` 构建订单:**
构建器提供了一种类型安全的方式来构造复杂订单。

```java
// 止损市价单
OrderRequest slOrder = OrderRequest.builder()
        .perp("ETH")
        .sell("0.01") // 平多仓的方向
        .stopBelow("2900") // 当价格跌破 2900 时触发
        .marketTrigger() // 触发后作为市价单执行
        .reduceOnly() // 确保它只减少仓位
        .build();

// 止盈限价单
OrderRequest tpOrder = OrderRequest.builder()
        .perp("ETH")
        .sell("0.01")
        .stopAbove("3100") // 当价格上涨超过 3100 时触发
        .limitPrice("3100") // 作为限价单执行
        .reduceOnly()
        .build();
```

**批量下单:**
在单个原子请求中下多个订单。

```java
List<OrderRequest> orders = List.of(slOrder, tpOrder);
JsonNode bulkResponse = exchange.bulkOrders(orders);
```

**修改订单:**

```java
ModifyOrderRequest req = ModifyOrderRequest.byOid("BTC", oid);
req.setLimitPx("86200.0");
req.setSz("0.001");
ModifyOrder result = exchange.modifyOrder(req);
```

**取消订单:**

```java
// 假设 'oid' 是一个未结订单的 ID
JsonNode cancelResponse = exchange.cancel("ETH", oid);
```

**更新杠杆:**

```java
UpdateLeverage leverageResponse = exchange.updateLeverage("ETH", 20, false); // 20倍杠杆，非全仓模式
```

### 实时数据 (WebSocket)

订阅实时数据流。`WebsocketManager` 会自动处理连接稳定性。

```java
// 定义一个OrderUpdatesSubscription 订阅
OrderUpdatesSubscription orderUpdatesSubscription = OrderUpdatesSubscription.of("0x....");

// 使用消息处理器和错误处理器进行订阅
info.subscribe(orderUpdatesSubscription,
    // OnMessage 回调
    (message) -> {
        LOGGER.info("收到 WebSocket 消息: {}", message);
        // 在此添加您处理消息的逻辑
    }
);

// 取消订阅
// info.unsubscribe(orderUpdatesSubscription);
```

### 错误处理 (`HypeError`)

所有 SDK 特定的错误都作为 `HypeError` 抛出。这包括来自服务器的 API 错误和客户端的验证错误。

```java
try {
    // 执行某些交易操作
} catch (HypeError e) {
    LOGGER.error("发生错误。代码: [{}], 消息: [{}]", e.getCode(), e.getMessage());
    // 您还可以访问原始的 JSON 错误响应（如果可用）
    if (e.getJsonNode() != null) {
        LOGGER.error("原始错误响应: {}", e.getJsonNode().toString());
    }
}
```


## 🤝 贡献指南

欢迎各种形式的贡献！请阅读我们的 [贡献指南](CONTRIBUTING.md) 开始。您可以通过报告问题、提出功能建议或提交拉取请求来提供帮助。

## 📄 许可协议

本项目采用 **Apache 2.0 许可证**。详情请参阅 [LICENSE](LICENSE) 文件。

## 📞 作者联系方式：

- 微信：heiye5050
- 邮箱：heiye115@gmail.com
- Telegram：@heiye5050
