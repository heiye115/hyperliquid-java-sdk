**Languages:** [中文](README.zh-CN.md)

# Hyperliquid Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.github.heiye115/hyperliquid-java-sdk.svg)](https://central.sonatype.com/artifact/io.github.heiye115/hyperliquid-java-sdk)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-21%2B-orange)](pom.xml)
[![Stars](https://img.shields.io/github/stars/heiye115/hyperliquid-java-sdk?style=social)](https://github.com/heiye115/hyperliquid-java-sdk)
[![Issues](https://img.shields.io/github/issues/heiye115/hyperliquid-java-sdk)](https://github.com/heiye115/hyperliquid-java-sdk/issues)

A pure Java SDK for the Hyperliquid decentralized exchange: market data, WebSocket subscriptions, orders, signing, and
multi-wallet management.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Contribution](#contribution)
- [License](#license)

## Overview

- Unified client to access Info (market data) and Exchange (trading) with one builder.
- Multi-wallet management: register multiple private keys and switch Exchange per-wallet.
- Robust WebSocket manager with auto-reconnect, backoff, and network monitoring.
- EIP-712 signing compatible with Hyperliquid actions, MessagePack-based hashing.

## Architecture

```mermaid
classDiagram
    class HyperliquidClient {
      +builder()
      +getInfo()
      +useExchange(privateKey)
      +getAddress(privateKey)
    }
    class Info {
      +l2Book(coin)
      +candles(coin, interval)
      +userState(address)
      +subscribe(subscription, callback)
    }
    class Exchange {
      +order(req)
      +bulkOrders(requests)
      +cancel(coin, oid)
      +updateLeverage(coin, crossed, leverage)
    }
    class WebsocketManager {
      +subscribe(JsonNode, MessageCallback)
      +setMaxReconnectAttempts(int)
      +setReconnectBackoffMs(initial, max)
    }
    class HypeHttpClient {
      +post(String path, Object payload) 
    }
    HyperliquidClient --> Info
    HyperliquidClient --> Exchange
    Info --> WebsocketManager
    Info --> HypeHttpClient
    Exchange --> HypeHttpClient
```

## Features

- Market data: `l2Book`, candles, user fills, open orders, clearinghouse state.
- Trading: limit/market/trigger orders, bulk orders, modify/cancel, leverage and margin updates.
- WebSocket: channel subscriptions with callback and error listener, ping/pong and auto-reconnect.
- Signing: EIP-712 typed data signing for L1 and user actions, MessagePack hashing.
- Utilities: JSON conversions, constants for mainnet/testnet URLs.

## Installation

- Requirements: JDK `21+`, Maven or Gradle.
- Maven:

```xml

<dependency>
    <groupId>io.github.heiye115</groupId>
    <artifactId>hyperliquid-java-sdk</artifactId>
    <version>0.2.0</version>
</dependency>
```

- Gradle (Groovy):

```gradle
implementation 'io.github.heiye115:hyperliquid-java-sdk:0.2.0'
```

- Alternative: JitPack (if not on Central yet):

```gradle
repositories { maven { url 'https://jitpack.io' } }
implementation 'com.github.heiye115:hyperliquid-java-sdk:main-SNAPSHOT'
```

## Quick Start

- Use environment variables for private keys; never hardcode secrets.

```java
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.L2Book;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.JSONUtil;

public class Demo {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank()) throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Info info = client.getInfo();
        L2Book book = info.l2Book("ETH");
        System.out.println("Best bid: " + book.getLevels().get(0).get(0).getPx());

        Exchange ex = client.getSingleExchange();
        OrderRequest req = OrderRequest.Open.limit(Tif.GTC, "ETH", true, 0.001, 3500.0);
        try {
            Order order = ex.order(req);
            System.out.println("Order status: " + order.getStatus());
        } catch (HypeError e) {
            System.err.println("Order failed: " + e.getMessage());
        }

        JsonNode sub = JSONUtil.convertValue(java.util.Map.of("type", "l2Book", "coin", "ETH"), JsonNode.class);
        info.subscribe(sub, msg -> System.out.println("WS msg: " + msg));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
        info.closeWs();
    }
}
```

## Configuration

- Builder options (`HyperliquidClient.java:91`):
    - `baseUrl(String)` and `testNetUrl()` use `Constants` mainnet/testnet (`Constants.java:11`, `Constants.java:16`).
    - `addPrivateKey(String)` and `addPrivateKeys(List<String>)` register wallets by private key.
    - `skipWs(boolean)` disables WebSocket manager for `Info` when true.
    - `timeout(int)` sets OkHttp timeouts.
    - `okHttpClient(OkHttpClient)` injects a custom client.
    - `enableDebugLogs()` enables package-level DEBUG logs.
- WebSocket tuning via `Info`:
    - `setMaxReconnectAttempts(int)` (`src/main/java/io/github/hyperliquid/sdk/apis/Info.java:897`).
    - `setNetworkCheckIntervalSeconds(int)` (`src/main/java/io/github/hyperliquid/sdk/apis/Info.java:910`).
    - `setReconnectBackoffMs(initialMs, maxMs)` (`src/main/java/io/github/hyperliquid/sdk/apis/Info.java:924`).

## Contribution

- Fork the repo and create feature branches.
- Follow Java naming and Clean Code practices; keep Controllers thin in integrations and put business logic in Services
  when embedding this SDK in MVC systems.
- Run `mvn -q -DskipTests package` locally; ensure Java 21.
- Add unit tests for critical logic where applicable.
- Open a Pull Request with a clear description and references.

## License

- Apache License 2.0. See `LICENSE`.

## Notes

- Never commit real private keys. Prefer environment variables and secure secret managers.
- Mainnet and testnet base URLs are defined in `Constants` (
  `src/main/java/io/github/hyperliquid/sdk/utils/Constants.java:11`, `:16`).
