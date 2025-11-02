**Languages:** [中文](README.zh-CN.md) 
# Hyperliquid Java SDK

A Software Development Kit (SDK) for the Hyperliquid decentralized exchange.

## Project Overview

- HTTP client with JSON serialization and robust error handling (ClientError/ServerError)
- Info client: market data, order books, user state, typed convenience methods
- Exchange client: order placement and action signing with Web3j
- WebSocket manager: connection lifecycle, heartbeat (ping/pong), resubscription, reconnect with backoff, network monitoring
- Compatibility helpers: automatic Accept: application/json header and coin name/ID conversion for request bodies

## Installation

Prerequisites:
- Java 21 or later
- Maven 3.8+

Option A: Build from source
- Run tests and build:
  ```
  mvn -q -DskipTests=false clean test
  mvn -q package
  ```
- The JAR will be available under `target/hyperliquid-java-sdk-0.1.2.jar`.

Option B: Use as a Maven dependency (local install)
- Install into your local repository:
  ```
  mvn clean install -DskipTests=true
  ```
- Add dependency in your project’s `pom.xml`:
  ```xml
  <!-- Coordinates from pom.xml -->
  <dependency>
    <groupId>io.github.heiye115</groupId>
    <artifactId>hyperliquid-java-sdk</artifactId>
    <version>0.1.2</version>
  </dependency>
  ```

## Usage

### 1) Info client: query mids

```java
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.info.Info;

// Create Info client (skipWs=true to avoid starting WebSocket in simple demos)
String baseUrl = "https://api.hyperliquid.xyz"; // mainnet endpoint
Info info = new Info(baseUrl, 10, true);

// Query all mids and print the JSON
JsonNode mids = info.allMids();
System.out.println(mids.toPrettyString());
```

### 2) Candles: 1h snapshot for BTC (typed)

```java
import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.model.info.Candle;
import java.util.List;

// Create Info client
String baseUrl = "https://api.hyperliquid.xyz";
Info info = new Info(baseUrl, 10, true);

// Prepare time range (last 24h)
long endTime = System.currentTimeMillis();
long startTime = endTime - 24L * 60 * 60 * 1000;

// Preferred call using coin name (e.g., "BTC" for perp or "@107" for spot)
List<Candle> candlesByName = info.candleSnapshotTyped("BTC", "1h", startTime, endTime);
System.out.println("candles(count by coin name) = " + (candlesByName == null ? 0 : candlesByName.size()));

// Alternative: using coin ID (the SDK converts coinId -> proper string for server compatibility)
int btcCoinId = 0; // example ID; consult info.meta() or your universe mapping
List<Candle> candlesById = info.candleSnapshotTyped(btcCoinId, "1h", startTime, endTime);
System.out.println("candles(count by coin id) = " + (candlesById == null ? 0 : candlesById.size()));
```

### 3) WebSocket subscriptions

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;

// Create Info client with WebSocket enabled
String baseUrl = "https://api.hyperliquid.xyz";
Info info = new Info(baseUrl, 10, false);

// Optional: add a connection listener to observe lifecycle events
info.addConnectionListener(new WebsocketManager.ConnectionListener() {
    @Override public void onConnecting(String url) { System.out.println("WS connecting: " + url); }
    @Override public void onConnected(String url) { System.out.println("WS connected:  " + url); }
    @Override public void onDisconnected(String url, int code, String reason, Throwable cause) {
        System.out.printf("WS disconnected: %s code=%d reason=%s\n", url, code, reason);
    }
    @Override public void onReconnecting(String url, int attempt, long nextDelayMs) {
        System.out.printf("WS reconnecting: %s attempt=%d nextDelayMs=%d\n", url, attempt, nextDelayMs);
    }
    @Override public void onReconnectFailed(String url, int attempted, Throwable lastError) {
        System.out.printf("WS reconnect failed: %s attempted=%d error=%s\n", url, attempted, lastError);
    }
    @Override public void onNetworkUnavailable(String url) { System.out.println("Network unavailable: " + url); }
    @Override public void onNetworkAvailable(String url) { System.out.println("Network available:   " + url); }
});

// Subscribe to 1m candles for ETH using coin name
ObjectMapper mapper = new ObjectMapper();
JsonNode sub = mapper.readTree("{\"type\":\"candle\",\"coin\":\"ETH\",\"interval\":\"1m\"}");
info.subscribe(sub, msg -> {
    // Handle incoming messages
    System.out.println("candle msg: " + msg.toString());
});

// Configure reconnection policy (optional)
info.setMaxReconnectAttempts(8);
info.setReconnectBackoffMs(1000, 30000); // initial 1s, max 30s
info.setNetworkCheckIntervalSeconds(5);
```

### 4) Exchange client: place a GTC limit order

```java
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.exchange.Exchange;
import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.model.order.LimitOrderType;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.OrderType;
import org.web3j.crypto.Credentials;

// Create Info and Exchange clients
String baseUrl = "https://api.hyperliquid.xyz";
Info info = new Info(baseUrl, 10, true);
Credentials wallet = Credentials.create("<YOUR_PRIVATE_KEY>");
Exchange ex = new Exchange(baseUrl, 10, wallet, info);

// Construct a GTC limit order: buy 0.10 ETH @ 3500.0
OrderType type = new OrderType(new LimitOrderType("Gtc"), null);
OrderRequest req = new OrderRequest("ETH", true, 0.10, 3500.0, type, false, null);
JsonNode resp = ex.order(req);
System.out.println(resp.toPrettyString());
```

Notes:
- The examples use mainnet endpoints and will hit the public API; outputs vary by live market/user state.
- Ensure your wallet has sufficient balance and that you understand trading risks before sending orders.

## Configuration

- Info(baseUrl, timeoutSeconds, skipWs): controls HTTP timeout and whether to start WebSocket
- WebSocket reconnection and monitoring via Info delegates:
  - `addConnectionListener(listener)` / `removeConnectionListener(listener)`
  - `setMaxReconnectAttempts(int)`
  - `setReconnectBackoffMs(long initialMs, long maxMs)`
  - `setNetworkCheckIntervalSeconds(int)`
- Candle intervals: string values like `"1m"`, `"5m"`, `"1h"`, `"1d"`
- Coin identifiers: prefer coin names (e.g., "BTC" or spot "@107"); integer coin IDs are converted internally for server compatibility when needed
- HTTP headers: the SDK automatically adds `Accept: application/json`

## API Reference (selected)

Info client:
- `JsonNode allMids()` / `Map<String,String> allMidsTyped()`
- `JsonNode meta()` / `JsonNode spotMeta()` / `JsonNode metaAndAssetCtxs()` / `JsonNode spotMetaAndAssetCtxs()`
- `JsonNode l2Snapshot(int coin)`
- `List<Candle> candleSnapshotTyped(String coinName, String interval, long startTime, long endTime)`
- `List<Candle> candleSnapshotTyped(int coinId, String interval, long startTime, long endTime)`
- `List<FrontendOpenOrder> frontendOpenOrdersTyped(String address [, String dex])`
- `List<io.github.hyperliquid.sdk.model.info.OpenOrder> openOrdersTyped(String address [, String dex])`
- `List<Fill> userFillsTyped(String address, boolean aggregateByTime)`
- `ClearinghouseState clearinghouseStateTyped(String address, String dex)`
- `SpotClearinghouseState spotClearinghouseStateTyped(String address)`
- WebSocket: `subscribe(JsonNode subscription, MessageCallback callback)`, `unsubscribe(JsonNode)`, `closeWs()`

Exchange client:
- `JsonNode order(OrderRequest req)` – converts `OrderRequest` to wire format and signs the action
- `JsonNode postAction(Map<String,Object> action, String vaultAddress, Long expiresAfter)` – low-level action submit

## Examples and Testing

Quick examples are provided inline in this README. If you want to compile and run them as standalone files:

1. Build and test the SDK:
   ```
   mvn -q -DskipTests=false clean test
   mvn -q package
   ```
2. Copy dependencies for running examples:
   ```
   mvn -q dependency:copy-dependencies
   ```
3. Create a file `QuickStart.java` in the project root and paste the usage snippet (e.g., Info mids or candles). Then compile:
   ```
   javac -cp target/classes;target/dependency/* QuickStart.java
   ```
4. Run:
   ```
   java -cp .;target/classes;target/dependency/* QuickStart
   ```

Run unit tests:
- All tests:
  ```
  mvn -q -DskipTests=false test
  ```
- Specific test class:
  ```
  mvn -q -Dtest=io.github.hyperliquid.sdk.CandleSnapshotBtc1hTest test
  ```

Note: If you prefer to organize examples under an `examples` folder, simply create it and place your example classes there. Adjust the `javac/java -cp` classpath accordingly.

## Security and Signing Notes

- The SDK provides two mechanisms related to L1 order/action signing:
  - `Signing.actionHash(...)`: computes a keccak256 over a MessagePack payload that includes the action JSON, nonce, optional vault address, and optional expiry.
  - `Signing.signTypedData(...)`: signs the supplied EIP-712 typed data. If a Base64 `actionHash` is present in the message, it is signed directly; otherwise the method falls back to signing the keccak256 of the typed payload JSON (a pragmatic simplification when full EIP-712 structures are not available).
- Ensure you understand which signing path your integration uses. For production-grade EIP-712 flows, supply domain/types/message per your backend’s specification.
- Always safeguard your private keys and avoid logging sensitive material.

## Contribution

We welcome contributions. Please:
- Fork the repository and create a feature branch
- Add class/method comments and keep code well-documented
- Follow Java conventions and keep methods cohesive
- Add or update unit tests where applicable
- Ensure `mvn -q -DskipTests=false clean test` passes locally
- Submit a pull request with a clear description

## License

Apache License 2.0. See `LICENSE` at the project root.

Copyright notice:
- Copyright (c) 2025 Hyperliquid Java SDK contributors
- Distributed under Apache 2.0 on an "AS IS" basis without warranties or conditions of any kind
