package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.wallet.ApiWallet;
import io.github.hyperliquid.sdk.utils.Constants;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.HypeHttpClient;
import okhttp3.OkHttpClient;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.time.Duration;
import java.util.*;


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
     * K:钱包地址 V:Exchange
     **/
    private final Map<String, Exchange> exchangesByAddress;

    /**
     * API钱包列表
     **/
    private final List<ApiWallet> apiWallets;


    public HyperliquidClient(Info info, Map<String, Exchange> exchangesByAddress, List<ApiWallet> apiWallets) {
        this.info = info;
        this.exchangesByAddress = exchangesByAddress;
        this.apiWallets = apiWallets;
    }

    /**
     * 获取 Info 客户端
     *
     * @return Info 客户端实例
     */
    public Info getInfo() {
        return info;
    }

    /**
     * 获取钱包地址到 Exchange 的映射
     *
     * @return 钱包地址到 Exchange 的映射
     */
    public Map<String, Exchange> getExchangesByAddress() {
        return exchangesByAddress;
    }

    /**
     * 获取 API 钱包列表
     *
     * @return API 钱包列表
     */
    public List<ApiWallet> getApiWallets() {
        return apiWallets;
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
     *
     * @param address 钱包地址（42位十六进制格式，0x开头）
     * @return 对应的 Exchange 实例
     * @throws HypeError 如果地址不存在，抛出异常并提示可用地址列表
     **/
    public Exchange useExchange(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new HypeError("Wallet address cannot be null or empty.");
        }
        Exchange ex = exchangesByAddress.get(address);
        if (ex == null) {
            String availableAddresses = String.join(", ", exchangesByAddress.keySet());
            throw new HypeError(String.format(
                    "Wallet address '%s' not found. Available addresses: [%s]",
                    address,
                    availableAddresses.isEmpty() ? "none" : availableAddresses
            ));
        }
        return ex;
    }

    /**
     * 检查指定地址的钱包是否存在
     *
     * @param address 钱包地址
     * @return 存在返回 true，否则返回 false
     */
    public boolean hasWallet(String address) {
        return address != null && exchangesByAddress.containsKey(address);
    }

    /**
     * 获取所有可用的钱包地址集合（不可变）
     *
     * @return 钱包地址集合
     */
    public Set<String> getAvailableAddresses() {
        return Collections.unmodifiableSet(exchangesByAddress.keySet());
    }

    /**
     * 获取所有钱包地址列表（按注册顺序）
     *
     * @return 钱包地址列表
     */
    public List<String> listWallets() {
        return new ArrayList<>(exchangesByAddress.keySet());
    }

    /**
     * 根据索引获取 Exchange 实例（按注册顺序）
     *
     * @param index 索引（从0开始）
     * @return 对应的 Exchange 实例
     * @throws HypeError 如果索引越界
     */
    public Exchange useExchangeByIndex(int index) {
        List<String> addresses = listWallets();
        if (index < 0 || index >= addresses.size()) {
            throw new HypeError(String.format(
                    "Wallet index %d out of bounds. Valid range: [0, %d]",
                    index,
                    addresses.size() - 1
            ));
        }
        return exchangesByAddress.get(addresses.get(index));
    }

    /**
     * 获取钱包总数
     *
     * @return 钱包数量
     */
    public int getWalletCount() {
        return exchangesByAddress.size();
    }

    /**
     * 获取单个地址（第一个钱包的主地址）
     *
     * @return 主钱包地址
     * @throws HypeError 如果没有可用钱包
     */
    public String getSingleAddress() {
        if (apiWallets == null || apiWallets.isEmpty()) {
            throw new HypeError("No wallets available. Please add at least one wallet.");
        }
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
            return new HyperliquidClient(
                    info,
                    Collections.unmodifiableMap(exchangesByAddress),
                    Collections.unmodifiableList(new ArrayList<>(this.apiWallets))
            );
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
