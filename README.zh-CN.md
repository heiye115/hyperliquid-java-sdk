**Languages:** [English](README.md)

# Hyperliquid Java SDK

Hyperliquid 去中心化交易所的软件开发工具包 (SDK)。

## 项目概述

- HTTP 客户端：JSON 序列化与健壮的错误处理（ClientError/ServerError）
- Info 客户端：行情、订单簿、用户状态与类型化便捷方法
- Exchange 客户端：下单与 L1 动作签名，基于 Web3j
- WebSocket 管理器：连接生命周期、心跳（ping/pong）、自动重订阅、指数退避重连、网络监控
- 兼容性增强：自动添加 `Accept: application/json` 头；请求体中 coin 字段支持名称/ID 转换

## 安装

环境要求：

- Java 21 或更高版本
- Maven 3.8+

方式一：从源码构建

- 运行测试并构建：
  ```
  mvn -q -DskipTests=false clean test
  mvn -q package
  ```
- 构建产物位于 `target/hyperliquid-java-sdk-0.1.2.jar`。

方式二：作为 Maven 依赖（本地安装）

- 安装到本地仓库：
  ```
  mvn clean install -DskipTests=true
  ```
- 在你的项目 `pom.xml` 中添加依赖：
  ```xml
  <!-- 坐标以 pom.xml 为准 -->
  <dependency>
    <groupId>io.github.heiye115</groupId>
    <artifactId>hyperliquid-java-sdk</artifactId>
    <version>0.1.5</version>
  </dependency>
  ```

## 使用示例

### 1）Info 客户端：查询中间价

```java
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.info.Info;

// 创建 Info 客户端（skipWs=true：示例中不启用 WebSocket）
String baseUrl = "https://api.hyperliquid.xyz"; // 主网地址
        Info info = new Info(baseUrl, 10, true);

        // 查询所有中间价并打印 JSON
        JsonNode mids = info.allMids();
System.out.

        println(mids.toPrettyString());
```

### 2）K线：查询 BTC 的 1 小时快照（类型化）

```java
import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.model.info.Candle;

import java.util.List;

// 创建 Info 客户端
String baseUrl = "https://api.hyperliquid.xyz";
        Info info = new Info(baseUrl, 10, true);

        // 准备时间范围（过去 24 小时）
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24L * 60 * 60 * 1000;

        // 推荐：使用币种名称（如 perp 的 "BTC"；或现货的 "@107"）
        List<Candle> candlesByName = info.candleSnapshotTyped("BTC", "1h", startTime, endTime);
System.out.

        println("candles(count by coin name) = "+(candlesByName ==null?0:candlesByName.size()));

        // 兼容：使用 coin 整数 ID（SDK 会在内部转换为服务器兼容的字符串格式）
        int btcCoinId = 0; // 示例 ID；可通过 info.meta() / universe 映射确认
        List<Candle> candlesById = info.candleSnapshotTyped(btcCoinId, "1h", startTime, endTime);
System.out.

        println("candles(count by coin id) = "+(candlesById ==null?0:candlesById.size()));
```

### 3）WebSocket 订阅

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.websocket.WebsocketManager;

// 启用 WebSocket 的 Info 客户端
String baseUrl = "https://api.hyperliquid.xyz";
        Info info = new Info(baseUrl, 10, false);

// 可选：添加连接状态监听器，观察连接/断开/重连/网络变化
info.

        addConnectionListener(new WebsocketManager.ConnectionListener() {
            @Override public void onConnecting (String url){
                System.out.println("WS connecting: " + url);
            }
            @Override public void onConnected (String url){
                System.out.println("WS connected:  " + url);
            }
            @Override public void onDisconnected (String url,int code, String reason, Throwable cause){
                System.out.printf("WS disconnected: %s code=%d reason=%s\n", url, code, reason);
            }
            @Override public void onReconnecting (String url,int attempt, long nextDelayMs){
                System.out.printf("WS reconnecting: %s attempt=%d nextDelayMs=%d\n", url, attempt, nextDelayMs);
            }
            @Override public void onReconnectFailed (String url,int attempted, Throwable lastError){
                System.out.printf("WS reconnect failed: %s attempted=%d error=%s\n", url, attempted, lastError);
            }
            @Override public void onNetworkUnavailable (String url){
                System.out.println("Network unavailable: " + url);
            }
            @Override public void onNetworkAvailable (String url){
                System.out.println("Network available:   " + url);
            }
        });

        // 订阅 ETH 的 1 分钟 K 线（使用币种名称）
        ObjectMapper mapper = new ObjectMapper();
        JsonNode sub = mapper.readTree("{\"type\":\"candle\",\"coin\":\"ETH\",\"interval\":\"1m\"}");
info.

        subscribe(sub, msg ->{
        // 处理收到的消息
        System.out.

        println("candle msg: "+msg.toString());
        });

// 可选：配置重连策略
        info.

        setMaxReconnectAttempts(8);
info.

        setReconnectBackoffMs(1000,30000); // 初始 1s，最大 30s
info.

        setNetworkCheckIntervalSeconds(5);
```

### 4）Exchange 客户端：提交 GTC 限价单

```java
import com.fasterxml.jackson.databind.JsonNode;
import io.github.hyperliquid.sdk.exchange.Exchange;
import io.github.hyperliquid.sdk.info.Info;
import io.github.hyperliquid.sdk.model.order.LimitOrderType;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.OrderType;
import org.web3j.crypto.Credentials;

// 创建 Info 与 Exchange 客户端
String baseUrl = "https://api.hyperliquid.xyz";
        Info info = new Info(baseUrl, 10, true);
        Credentials wallet = Credentials.create("<你的私钥>");
        Exchange ex = new Exchange(baseUrl, 10, wallet, info);

        // 构造 GTC 限价单：买入 0.10 ETH @ 3500.0
        OrderType type = new OrderType(new LimitOrderType("Gtc"), null);
        OrderRequest req = new OrderRequest("ETH", true, 0.10, 3500.0, type, false, null);
        JsonNode resp = ex.order(req);
System.out.

        println(resp.toPrettyString());
```

说明：

- 示例使用主网公共 API，输出随实时行情/账户状态而变化。
- 下单前请确保账户有充足余额并了解交易风险。

## 配置项

- Info(baseUrl, timeoutSeconds, skipWs)：控制 HTTP 超时与是否启用 WebSocket
- WebSocket 重连与网络监控（通过 Info 委托）：
    - `addConnectionListener(listener)` / `removeConnectionListener(listener)`
    - `setMaxReconnectAttempts(int)`
    - `setReconnectBackoffMs(long initialMs, long maxMs)`
    - `setNetworkCheckIntervalSeconds(int)`
- K 线时间粒度：字符串如 `"1m"`, `"5m"`, `"1h"`, `"1d"`
- 币种标识：推荐使用名称（如 "BTC"；或现货 "@107"）；当使用整数 ID 时，SDK 会在内部转换为服务器兼容的字符串格式
- HTTP 头部：SDK 会自动添加 `Accept: application/json`

## API 参考（部分）

Info 客户端：

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
- WebSocket：`subscribe(JsonNode subscription, MessageCallback callback)`, `unsubscribe(JsonNode)`, `closeWs()`

Exchange 客户端：

- `JsonNode order(OrderRequest req)` —— 将 `OrderRequest` 转换为 wire 格式并签名后发送
- `JsonNode postAction(Map<String,Object> action, String vaultAddress, Long expiresAfter)` —— 低层级动作提交

## 示例与测试

本文已提供了内嵌的快速示例代码片段。若你希望以独立文件运行：

1. 构建与测试 SDK：
   ```
   mvn -q -DskipTests=false clean test
   mvn -q package
   ```
2. 拷贝依赖：
   ```
   mvn -q dependency:copy-dependencies
   ```
3. 在项目根目录创建 `QuickStart.java` 文件，粘贴 README 中的使用示例（如 Info 查询 mids 或 K 线示例），然后编译：
   ```
   javac -cp target/classes;target/dependency/* QuickStart.java
   ```
4. 运行：
   ```
   java -cp .;target/classes;target/dependency/* QuickStart
   ```

运行单元测试：

- 全部测试：
  ```
  mvn -q -DskipTests=false test
  ```
- 指定测试类：
  ```
  mvn -q -Dtest=io.github.hyperliquid.sdk.CandleSnapshotBtc1hTest test
  ```

备注：如果你更喜欢将示例置于 `examples` 文件夹，可自行创建该文件夹并将示例类置于其中，按需调整 `javac/java -cp` 的类路径。

## 安全与签名说明

- SDK 提供两类与 L1 下单/动作相关的签名能力：
    - `Signing.actionHash(...)`：对包含 action JSON、nonce、可选 vault 地址与可选过期时间的 MsgPack 载荷进行 keccak256。
    - `Signing.signTypedData(...)`：对给定的 EIP-712 typed data 进行签名。若 message 中包含 Base64 的 `actionHash`
      字段，则直接对其签名；否则回退为对 typed payload 的 JSON 进行 keccak 再签名（当后端未完整提供 EIP-712 结构时的务实降级）。
- 请确认你的集成路径使用了哪种签名方式。生产环境推荐按后端规范提供完整的 domain/types/message。
- 请妥善保管私钥，避免在日志中输出敏感信息。

## 贡献指南

欢迎贡献代码！请遵循以下流程：

- Fork 仓库并创建特性分支
- 为类与方法补充必要注释，保持良好文档化
- 遵循 Java 代码规范，保持方法内聚与清晰
- 适当新增或更新单元测试
- 确保本地执行 `mvn -q -DskipTests=false clean test` 通过
- 提交 Pull Request，并附上清晰的变更说明

## 许可证

Apache License 2.0。详见项目根目录 `LICENSE` 文件。

版权声明：

- Copyright (c) 2025 Hyperliquid Java SDK contributors
- 按 Apache 2.0 许可“现状”分发，不附带任何明示或暗示的担保或条件。
