package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.wallet.ApiWallet;
import io.github.hyperliquid.sdk.utils.Constants;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.HypeHttpClient;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
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
    @Getter
    private final Info info;

    /**
     * K:钱包地址 V:Exchange
     **/
    @Getter
    private final Map<String, Exchange> exchangesByAddress;

    /**
     * API钱包列表
     **/
    @Getter
    private final List<ApiWallet> apiWallets;


    public HyperliquidClient(Info info, Map<String, Exchange> exchangesByAddress, List<ApiWallet> apiWallets) {
        this.info = info;
        this.exchangesByAddress = exchangesByAddress;
        this.apiWallets = apiWallets;
    }

    /**
     * 获取单个Exchange  , 如果有多个则返回第一个
     **/
    public Exchange getSingleExchange() {
        if (exchangesByAddress.isEmpty()) {
            throw new HypeError("No exchange instances available.");
        }
        return exchangesByAddress.values().iterator().next();
    }

    /**
     * 根据钱包地址获取 Exchange 实例
     **/
    public Exchange useExchange(String address) {
        Exchange ex = exchangesByAddress.get(address);
        if (ex == null) {
            throw new HypeError("No exchange instance found for the provided private key.");
        }
        return ex;
    }

    public String getSingleAddress() {
        return apiWallets.getFirst().getPrimaryWalletAddress();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String baseUrl = Constants.MAINNET_API_URL;

        private int timeout = 10;

        private boolean skipWs = false;

        private final List<ApiWallet> apiWallets = new ArrayList<>();

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
            addApiWallet(null, privateKey);
            return this;
        }

        public Builder addPrivateKeys(List<String> pks) {
            for (String pk : pks) {
                addPrivateKey(pk);
            }
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

        public Builder addApiWallet(ApiWallet apiWallet) {
            apiWallets.add(apiWallet);
            return this;
        }

        public Builder addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey) {
            apiWallets.add(new ApiWallet(primaryWalletAddress, apiWalletPrivateKey));
            return this;
        }

        public Builder addApiWallets(List<ApiWallet> apiWallets) {
            this.apiWallets.addAll(apiWallets);
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
            Map<String, Exchange> exchangesByAddress = new LinkedHashMap<>();
            if (!apiWallets.isEmpty()) {
                for (ApiWallet apiWallet : apiWallets) {
                    validatePrivateKey(apiWallet.getApiWalletPrivateKey());
                    Credentials credentials = Credentials.create(apiWallet.getApiWalletPrivateKey());
                    if (apiWallet.getPrimaryWalletAddress() == null || apiWallet.getPrimaryWalletAddress().trim().isEmpty()) {
                        apiWallet.setPrimaryWalletAddress(credentials.getAddress());
                    }
                    exchangesByAddress.put(apiWallet.getPrimaryWalletAddress(), new Exchange(hypeHttpClient, credentials, info));
                }
            }
            return new HyperliquidClient(info, exchangesByAddress, null);
        }


        /**
         * 私钥验证逻辑：
         * 1. 不为空
         * 2. 长度与字符集合法
         * 3. 能被 Web3j 正常解析为 ECKeyPair
         */
        private void validatePrivateKey(String privateKey) {
            if (privateKey == null || privateKey.trim().isEmpty()) {
                throw new HypeError("Private key cannot be null or empty.");
            }

            String normalizedKey = privateKey.startsWith("0x") ? privateKey.substring(2) : privateKey;

            if (!normalizedKey.matches("^[0-9a-fA-F]+$")) {
                throw new HypeError("Private key contains invalid characters. Must be hex.");
            }

            if (normalizedKey.length() != 64) {
                throw new HypeError("Private key must be 64 hexadecimal characters long.");
            }

            try {
                BigInteger keyInt = Numeric.toBigInt(privateKey);
                ECKeyPair.create(keyInt);
            } catch (Exception e) {
                throw new HypeError("Invalid private key: cryptographic validation failed.", e);
            }
        }
    }
}
