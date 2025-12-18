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
    <version>0.2.9</version> <!-- Replace with the latest version -->
</dependency>
```

- **Gradle (Groovy)**:

```gradle
implementation 'io.github.heiye115:hyperliquid-java-sdk:0.2.9' // Replace with the latest version
```

## ⚡ 5-Minute Quick Start

Get up and running in minutes with this complete, runnable example.

**Prerequisites:**

1. Have a Hyperliquid account. For this example, use the **Testnet**.
2. Obtain your wallet's private key.
3. **IMPORTANT:** Store your private key securely. The recommended way is to use an environment variable.

```bash
export HYPERLIQUID_TESTNET_PRIVATE_KEY="0xYourPrivateKey"
```

### Runnable Example

This example demonstrates how to:

1. Build the client.
2. Query market data (`l2Book`).
3. Place a limit order (`order`).
4. Handle potential API errors (`HypeError`).

```java
public class QuickStart {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuickStart.class);

    public static void main(String[] args) {
        // 1. Recommended: Use API Wallet for better security
        // API Wallet: Sub-wallet authorized by main wallet, with limited permissions, main private key not exposed
        String primaryWalletAddress = "";  // Primary wallet address
        String apiWalletPrivateKey = "";   // API wallet private key

        // 2. Build the client for the testnet
        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl() // Use the testnet environment
                .addApiWallet(primaryWalletAddress, apiWalletPrivateKey)
                .build();

        Info info = client.getInfo();
        Exchange exchange = client.getExchange(); // Get the exchange instance for the added wallet

        // 3. Query market data: Get the L2 book for "ETH"
        try {
            LOGGER.info("Querying L2 book for ETH...");
            L2Book l2Book = info.l2Book("ETH");
            // Print the top 3 levels of bids and asks
            LOGGER.info("Successfully retrieved L2 book for {}:", l2Book.getCoin());
            l2Book.getLevels().get(0).subList(0, 3).forEach(level ->
                    LOGGER.info("  Ask - Price: {}, Size: {}", level.getPx(), level.getSz())
            );
            l2Book.getLevels().get(1).subList(0, 3).forEach(level ->
                    LOGGER.info("  Bid - Price: {}, Size: {}", level.getPx(), level.getSz())
            );
        } catch (HypeError e) {
            LOGGER.error("Failed to query L2 book.  Message: {}", e.getMessage());
        }

        // 4. Execute a trade: Create a limit buy order for ETH
        try {
            LOGGER.info("Placing a limit buy order for ETH...");
            // Create a limit buy order for 0.01 ETH at a price of $1500
            OrderRequest orderRequest = OrderRequest.builder()
                    .perp("ETH") // Perpetual contract for ETH
                    .buy("0.01") // Buying 0.01 ETH
                    .limitPrice("1500") // Limit price for the order
                    .gtc() // Good Till Cancel (GTC) order
                    .build();

            Order order = exchange.order(orderRequest);
            LOGGER.info("Order placed successfully. Response: {}", JSONUtil.writeValueAsString(order));

        } catch (HypeError | JsonProcessingException e) {
            // Example of handling a specific error, e.g., insufficient margin
            LOGGER.error("Order placement failed. Message: {}",  e.getMessage(), e);
        }
    }
}
```

## 📚 Core Features Guide

### Client Configuration

The `HyperliquidClient.builder()` provides a fluent API for configuration.

```java
// Full configuration example
HyperliquidClient client = HyperliquidClient.builder()
        // Select network (or provide a custom URL)
        .testNetUrl() // or .baseUrl("https://api.hyperliquid.xyz")

        // --- Wallet Management ---
        // Option 1: Add a single main private key
        .addPrivateKey("0xYourMainPrivateKey")

        // Option 2: Add multiple API Wallets (recommended for security)
        // An API wallet is a sub-wallet authorized by your main wallet.
        .addApiWallet("0xYourMainAddress1", "0xYourApiPrivateKey1")
        .addApiWallet("0xYourMainAddress2", "0xYourApiPrivateKey2")

        // --- Performance ---
        // Cache warm-up is enabled by default; disable only when startup latency is critical
        // .disableAutoWarmUpCache()

        // --- (optional)Network ---
        // Set custom HTTP client
        .okHttpClient(customClient)
        // Set timeout (seconds) for the underlying OkHttpClient (applies to connect/read/write)
        .timeout(15)

        // Build the immutable client instance
        .build();

// Accessing exchange instances for different wallets
Exchange exchange1 = client.getExchange("0xYourMainAddress1");
Exchange exchange2 = client.getExchange("0xYourMainAddress2");
```

### Querying Data (`Info` API)

The `Info` API provides access to all public market data and private user data.

**Get User State:**

```java
UserState userState = info.userState("0xYourAddress");
LOGGER.info("Total margin usage: {}", userState.getMarginSummary().getTotalMarginUsed());
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
JsonNode bulkResponse = exchange.bulkOrders(orders);
```

**Cancel an Order:**

```java
// Assumes 'oid' is the ID of an open order
JsonNode cancelResponse = exchange.cancel("ETH", oid);
```

**Update Leverage:**

```java
UpdateLeverage leverageResponse = exchange.updateLeverage("ETH", 20, false); // 20x leverage, non-cross-margin
```

### Real-time Data (WebSocket)

Subscribe to real-time data streams. The `WebsocketManager` handles connection stability automatically.

```java
// Define a subscription for order updates events
OrderUpdatesSubscription orderUpdatesSubscription = OrderUpdatesSubscription.of("0x....");
// Subscribe with a message handler and an error handler
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
    LOGGER.error("An error occurred. Code: [{}], Message: [{}]", e.getCode(), e.getMessage());
    // You can also access the original JSON error response if available
    if (e.getJsonNode() != null) {
        LOGGER.error("Raw error response: {}", e.getJsonNode().toString());
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
