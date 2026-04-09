**Languages:** [中文](README.zh-CN.md)

# Hyperliquid Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.github.heiye115/hyperliquid-java-sdk.svg)](https://central.sonatype.com/artifact/io.github.heiye115/hyperliquid-java-sdk)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-21%2B-orange)](pom.xml)
[![Stars](https://img.shields.io/github/stars/heiye115/hyperliquid-java-sdk?style=social)](https://github.com/heiye115/hyperliquid-java-sdk)
[![Issues](https://img.shields.io/github/issues/heiye115/hyperliquid-java-sdk)](https://github.com/heiye115/hyperliquid-java-sdk/issues)

Type-safe, feature-rich Java SDK for Hyperliquid trading and real-time market data.

## 🎯 Overview

This SDK is a pure Java client for the Hyperliquid decentralized exchange, making it easy to build trading bots, analytics pipelines, and platform integrations.

### ✨ Feature Highlights

- **🚀 High Performance:** Optimized for low-latency trading with efficient data handling.
- **🛡️ Type-Safe:** Fluent builders and strongly-typed models prevent common errors and enhance code clarity.
- **🔐 Secure by Design:** Robust EIP-712 signing and clear wallet management patterns.
- **💼 Multi-Wallet Management:** Seamlessly manage and switch between multiple trading accounts (Main & API Wallets).
- **🌐 Powerful WebSocket:** Auto-reconnect, exponential backoff, and type-safe real-time data subscriptions.
- **🧩 Fluent & Intuitive API:** A clean, modern API designed for an excellent developer experience.

## 🛠️ Installation

- **Requirements**: JDK `21+`, Maven or Gradle.
- **Maven**:

```xml
<dependency>
    <groupId>io.github.heiye115</groupId>
    <artifactId>hyperliquid-java-sdk</artifactId>
    <version>0.2.23</version> <!-- Replace with the latest version -->
</dependency>
```

- **Gradle (Groovy)**:

```gradle
implementation 'io.github.heiye115:hyperliquid-java-sdk:0.2.23' // Replace with the latest version
```

## ⚡ 5-Minute Quick Start

Use this section as two progressive steps: start with read-only market queries, then add a wallet when you need trading.

### Step 1: Minimal Read-Only Example (No Private Key Required)

```java
public class QuickStartReadOnly {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuickStartReadOnly.class);

    public static void main(String[] args) {
        HyperliquidClient client = HyperliquidClient.builder()
                .build(); // Defaults to mainnet

        Info info = client.getInfo();
        L2Book l2Book = info.l2Book("ETH");
        LOGGER.info("Top ask: {}", l2Book.getLevels().get(0).get(0));
        LOGGER.info("Top bid: {}", l2Book.getLevels().get(1).get(0));
    }
}
```

### Step 2: Minimal Trading Example

**Prerequisites:**

1. By default, the client connects to mainnet.
2. If you want testnet, call `.testNetUrl()` in the builder.
3. Prepare your private key (main wallet or API wallet key).
4. Store the key in an environment variable.

```bash
export HYPERLIQUID_PRIVATE_KEY="0xYourPrivateKey"
```

```java
public class QuickStartTrade {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuickStartTrade.class);

    public static void main(String[] args) {
        String privateKey = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        HyperliquidClient client = HyperliquidClient.builder()
                .addPrivateKey(privateKey)
                .build();

        Exchange exchange = client.getExchange();
        OrderRequest request = OrderRequest.builder()
                .perp("ETH")
                .buy("0.01")
                .limitPrice("1500")
                .gtc()
                .build();

        Order order = exchange.order(request);
        LOGGER.info("Order response: {}", JSONUtil.writeValueAsString(order));
    }
}
```

For more complete scenarios, check the runnable classes under `examples/`.

## 📚 Core Features Guide

### Client Configuration

The `HyperliquidClient.builder()` provides a fluent API for configuration.

```java
// Full configuration example
HyperliquidClient client = HyperliquidClient.builder()
        // Network selection
        // Default is mainnet, no extra configuration required
        // .testNetUrl() // Enable this line for testnet
        // or .baseUrl("https://api.hyperliquid.xyz")

        // --- Wallet Management ---
        // Option 1: Add a single main private key
        .addPrivateKey("0xYourMainPrivateKey")

        // Option 2: Add multiple API Wallets (recommended for security)
        // An API wallet is a sub-wallet authorized by your main wallet.
        .addApiWallet("0xYourMainAddress1", "0xYourApiPrivateKey1")
        .addApiWallet("bot-2", "0xYourMainAddress2", "0xYourApiPrivateKey2")

        // --- Performance ---
        // Cache warm-up is enabled by default; disable only when startup latency is critical
        // .disableAutoWarmUpCache()

        // --- Builder-deployed DEX (for WebSocket subscriptions to symbols like "xyz:SP500") ---
        // .perpDexs(List.of("xyz"))
        // .addPerpDex("anotherDex")

        // --- (optional)Network ---
        // Set custom HTTP client
        .okHttpClient(customClient)
        // Set timeout (seconds) for the underlying OkHttpClient (applies to connect/read/write)
        .timeout(15)

        // Build the immutable client instance
        .build();

// Accessing exchange instances for different wallets
Exchange exchange1 = client.getExchange("0xYourMainAddress1");
Exchange exchange2 = client.getExchange("bot-2");
```

### Querying Data (`Info` API)

The `Info` API provides access to all public market data and private user data.

**Get User State:**

```java
ClearinghouseState userState = info.userState("0xYourAddress");
LOGGER.info("Withdrawable: {}", userState.getWithdrawable());
```

**Get Open Orders:**

```java
List<OpenOrder> openOrders = info.openOrders("0xYourAddress");
LOGGER.info("User has {} open orders.", openOrders.size());
```

**Get Market Metadata:**

```java
Meta meta = info.meta();
// Find details for a specific asset
meta.getUniverse().stream()
    .filter(asset -> "ETH".equals(asset.getName()))
    .findFirst()
    .ifPresent(ethAsset -> LOGGER.info("Max leverage for ETH: {}", ethAsset.getMaxLeverage()));
```

### Trading (`Exchange` API)

The `Exchange` API handles all state-changing actions, which require signing.

**Building Orders with `OrderRequest.Builder`:**
The builder provides a type-safe way to construct complex orders.

```java
// Stop-Loss Market Order
OrderRequest slOrder = OrderRequest.builder()
        .perp("ETH")
        .sell("0.01") // Direction to close a long position
        .stopBelow("2900") // Trigger when price drops below 2900
        .marketTrigger() // Execute as a market order when triggered
        .reduceOnly() // Ensures it only reduces a position
        .build();

// Take-Profit Limit Order
OrderRequest tpOrder = OrderRequest.builder()
        .perp("ETH")
        .sell("0.01")
        .stopAbove("3100") // Trigger when price rises above 3100
        .limitPrice("3100") // Execute as a limit order
        .reduceOnly()
        .build();
```

**Bulk Orders:**
Place multiple orders in a single atomic request.

```java
List<OrderRequest> orders = List.of(slOrder, tpOrder);
BulkOrder bulkResponse = exchange.bulkOrders(orders);
```

**Modify an Order:**

```java
ModifyOrderRequest req = ModifyOrderRequest.byOid("BTC", oid);
req.setLimitPx("86200.0");
req.setSz("0.001");
ModifyOrder result = exchange.modifyOrder(req);
```

**Cancel an Order:**

```java
// Assumes 'oid' is the ID of an open order
Cancel cancelResponse = exchange.cancel("ETH", oid);
```

**Update Leverage:**

```java
UpdateLeverage leverageResponse = exchange.updateLeverage("ETH", false, 20); // 20x leverage, isolated margin
```

### Real-time Data (WebSocket)

Subscribe to real-time data streams. The `WebsocketManager` handles connection stability automatically.

```java
// Define a subscription for order updates events
OrderUpdatesSubscription orderUpdatesSubscription = OrderUpdatesSubscription.of("0x....");
// Subscribe with a message callback
info.subscribe(orderUpdatesSubscription,
    // OnMessage callback
    (message) -> {
        LOGGER.info("Received WebSocket message: {}", message);
        // Add your logic to process the message
    }
);

// To unsubscribe
// info.unsubscribe(orderUpdatesSubscription);
```

### Error Handling (`HypeError`)

All SDK-specific errors are thrown as `HypeError`. This includes API errors from the server and client-side validation errors.

```java
try {
    // Some exchange operation
} catch (HypeError e) {
    if (e instanceof HypeError.ClientHypeError ce) {
        LOGGER.error("Client error (status={}): {}", ce.getStatusCode(), ce.getMessage());
    } else if (e instanceof HypeError.ServerHypeError se) {
        LOGGER.error("Server error (status={}): {}", se.getStatusCode(), se.getMessage());
    } else {
        LOGGER.error("SDK error: {}", e.getMessage());
    }
}
```


## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) to get started. You can help by reporting issues, suggesting features, or submitting pull requests.

## 📄 License

This project is licensed under the **Apache 2.0 License**. See the [LICENSE](LICENSE) file for details.

## 📞 Contact

Contact the author via:

- WeChat: heiye5050
- Email: heiye115@gmail.com
- Telegram: @heiye5050
