package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.utils.Constants;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.HypeHttpClient;
import okhttp3.OkHttpClient;
import org.web3j.crypto.Credentials;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * HyperliquidClient 统一管理客户端，负责下单、撤单、转账等 L1/L2 操作。
 * 支持多个钱包凭证的管理与切换。
 */
public class HyperliquidClient {

    /**
     * Info 客户端
     **/
    private final Info info;

    /**
     * K:私钥 V:Exchange
     **/
    private final Map<String, Exchange> exchangesByPrivateKey;

    /**
     * K:私钥 V:地址
     **/
    private final Map<String, String> addressesByPrivateKey;


    private HyperliquidClient(Info info, Map<String, Exchange> exchangesByPrivateKey, Map<String, String> addressesByPrivateKey) {
        this.info = info;
        this.exchangesByPrivateKey = exchangesByPrivateKey;
        this.addressesByPrivateKey = addressesByPrivateKey;
    }

    public Info getInfo() {
        return info;
    }

    /**
     * 获取单个Exchange  , 如果有多个则返回第一个
     **/
    public Exchange getSingleExchange() {
        if (exchangesByPrivateKey.isEmpty()) {
            throw new HypeError("No exchange instances available.");
        }
        return exchangesByPrivateKey.values().iterator().next();
    }

    /**
     * 根据私钥获取 Exchange 实例
     **/
    public Exchange useExchange(String privateKey) {
        Exchange ex = exchangesByPrivateKey.get(privateKey);
        if (ex == null) {
            throw new HypeError("No exchange instance found for the provided private key.");
        }
        return ex;
    }

    /**
     * 获取钱包地址
     **/
    public String getAddress(String privateKey) {
        return addressesByPrivateKey.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(privateKey))
                .map(Map.Entry::getValue).findFirst()
                .orElseThrow(() -> new HypeError("No address found for the provided private key."));
    }

    /**
     * 获取单个钱包地址 , 如果有多个则返回第一个
     **/
    public String getSingleAddress() {
        if (addressesByPrivateKey.isEmpty()) {
            throw new HypeError("No addresses available.");
        }
        return addressesByPrivateKey.values().iterator().next();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String baseUrl = Constants.MAINNET_API_URL;

        private int timeout = 10;

        private boolean skipWs = false;

        private final List<String> privateKeys = new ArrayList<>();

        private OkHttpClient okHttpClient = null;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder testNetUrl() {
            this.baseUrl = Constants.TESTNET_API_URL;
            return this;
        }

        public Builder addPrivateKey(String privateKey) {
            privateKeys.add(privateKey);
            return this;
        }

        public Builder addPrivateKeys(List<String> pks) {
            privateKeys.addAll(pks);
            return this;
        }

        public Builder skipWs(boolean skipWs) {
            this.skipWs = skipWs;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder okHttpClient(OkHttpClient client) {
            this.okHttpClient = client;
            return this;
        }

        public Builder enableDebugLogs() {
            System.setProperty("org.slf4j.simpleLogger.log.io.github.hyperliquid", "DEBUG");
            return this;
        }


        private OkHttpClient getOkHttpClient() {
            return okHttpClient != null ? okHttpClient : new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .readTimeout(Duration.ofSeconds(timeout))
                    .writeTimeout(Duration.ofSeconds(timeout))
                    .build();
        }


        public HyperliquidClient build() {
            OkHttpClient httpClient = getOkHttpClient();
            HypeHttpClient hypeHttpClient = new HypeHttpClient(baseUrl, httpClient);
            Info info = new Info(baseUrl, hypeHttpClient, skipWs);
            Map<String, Exchange> exchangeMap = new LinkedHashMap<>();
            Map<String, String> privateKeyMap = new LinkedHashMap<>();
            if (!privateKeys.isEmpty()) {
                for (String key : privateKeys) {
                    Credentials credentials = Credentials.create(key);
                    privateKeyMap.put(key, credentials.getAddress());
                    exchangeMap.put(key, new Exchange(hypeHttpClient, credentials, info));
                }
            }
            return new HyperliquidClient(info, exchangeMap, privateKeyMap);
        }
    }
}
