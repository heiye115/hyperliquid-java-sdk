package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.wallet.ApiWallet;
import io.github.hyperliquid.sdk.utils.Constants;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.HypeHttpClient;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified Hyperliquid client facade for trading and query operations.
 * <p>
 * This client coordinates {@link Info} and one or more {@link Exchange}
 * instances, enabling multi-wallet management and alias-based routing.
 * </p>
 */
public class HyperliquidClient {

    private static final Logger log = LoggerFactory.getLogger(HyperliquidClient.class);

    /**
     * Info service for read-only query operations.
     */
    private final Info info;

    /**
     * Shared HTTP client wrapper used by API services.
     */
    private final HypeHttpClient hypeHttpClient;

    /**
     * Mapping from wallet alias to bound Exchange instance.
     */
    private final Map<String, Exchange> exchangesByAlias;

    /**
     * Managed API wallet list.
     */
    private final List<ApiWallet> apiWallets;

    /**
     * Construct a HyperliquidClient instance.
     *
     * @param info             Info client for query operations
     * @param hypeHttpClient   Shared HTTP client wrapper
     * @param exchangesByAlias Wallet alias to Exchange mapping
     * @param apiWallets       Managed API wallet list
     */
    public HyperliquidClient(Info info, HypeHttpClient hypeHttpClient, Map<String, Exchange> exchangesByAlias,
            List<ApiWallet> apiWallets) {
        this.info = info;
        this.hypeHttpClient = hypeHttpClient;
        this.exchangesByAlias = exchangesByAlias;
        this.apiWallets = apiWallets;
    }

    /**
     * Returns the Info service instance.
     *
     * @return Info service
     */
    public Info getInfo() {
        return info;
    }

    /**
     * Returns an immutable snapshot of alias-to-Exchange mappings.
     *
     * @return Alias-to-Exchange mapping snapshot
     */
    public synchronized Map<String, Exchange> getExchangesByAlias() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(exchangesByAlias));
    }

    /**
     * Returns an immutable snapshot of managed API wallets.
     *
     * @return API wallet list snapshot
     */
    public synchronized List<ApiWallet> getApiWallets() {
        return Collections.unmodifiableList(new ArrayList<>(apiWallets));
    }

    /**
     * Returns one available Exchange instance.
     * <p>
     * If multiple exchanges are configured, this method returns the first one in
     * insertion order.
     * </p>
     *
     * @return Exchange instance
     * @throws HypeError If no exchange is configured
     */
    public synchronized Exchange getExchange() {
        if (exchangesByAlias.isEmpty()) {
            throw new HypeError("No exchange instances available.");
        }
        return exchangesByAlias.values().iterator().next();
    }

    /**
     * Returns an Exchange instance by wallet alias.
     *
     * @param alias Wallet alias
     * @return Matching Exchange instance
     * @throws HypeError If alias is blank or does not exist
     */
    public synchronized Exchange getExchange(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new HypeError("Wallet alias cannot be null or empty.");
        }
        Exchange ex = exchangesByAlias.get(alias);
        if (ex == null) {
            String availableAliases = String.join(", ", exchangesByAlias.keySet());
            throw new HypeError(String.format("Wallet alias '%s' not found. Available aliases: [%s]", alias, availableAliases));
        }
        return ex;
    }


    /**
     * Checks whether a wallet alias exists.
     *
     * @param alias Wallet alias
     * @return true if the alias exists; false otherwise
     */
    public synchronized boolean hasWallet(String alias) {
        return alias != null && exchangesByAlias.containsKey(alias);
    }

    /**
     * Returns all available primary wallet addresses as an immutable set.
     *
     * @return Primary wallet address set
     */
    public synchronized Set<String> getAvailableAddresses() {
        return apiWallets.stream().map(ApiWallet::getPrimaryWalletAddress).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the number of managed wallets.
     *
     * @return Wallet count
     */
    public synchronized int getWalletCount() {
        return apiWallets.size();
    }

    /**
     * Returns the primary address of the first configured wallet.
     *
     * @return Primary wallet address
     * @throws HypeError If no wallet is configured
     */
    public synchronized String getSingleAddress() {
        if (apiWallets == null || apiWallets.isEmpty()) {
            throw new HypeError("No wallets available. Please add at least one wallet.");
        }
        return apiWallets.getFirst().getPrimaryWalletAddress();
    }

    /**
     * Adds an API wallet after client construction.
     *
     * @param apiWallet API wallet
     * @return Current client instance for fluent chaining
     * @throws HypeError If wallet data is invalid
     */
    public synchronized HyperliquidClient addApiWallet(ApiWallet apiWallet) {
        addApiWalletInternal(apiWallet);
        return this;
    }

    /**
     * Adds an API wallet after client construction.
     *
     * @param primaryWalletAddress Primary wallet address
     * @param apiWalletPrivateKey  API wallet private key
     * @return Current client instance for fluent chaining
     * @throws HypeError If wallet data is invalid
     */
    public synchronized HyperliquidClient addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey) {
        addApiWalletInternal(new ApiWallet(primaryWalletAddress, apiWalletPrivateKey));
        return this;
    }

    /**
     * Adds an API wallet with an explicit alias after client construction.
     *
     * @param alias                Wallet alias
     * @param primaryWalletAddress Primary wallet address
     * @param apiWalletPrivateKey  API wallet private key
     * @return Current client instance for fluent chaining
     * @throws HypeError If wallet data is invalid
     */
    public synchronized HyperliquidClient addApiWallet(String alias, String primaryWalletAddress,
            String apiWalletPrivateKey) {
        addApiWalletInternal(new ApiWallet(alias, primaryWalletAddress, apiWalletPrivateKey));
        return this;
    }

    /**
     * Adds a private key after client construction.
     *
     * @param privateKey Private key
     * @return Current client instance for fluent chaining
     * @throws HypeError If private key is invalid
     */
    public synchronized HyperliquidClient addPrivateKey(String privateKey) {
        addApiWalletInternal(new ApiWallet(null, privateKey));
        return this;
    }

    /**
     * Adds a private key with an explicit alias after client construction.
     *
     * @param alias      Wallet alias
     * @param privateKey Private key
     * @return Current client instance for fluent chaining
     * @throws HypeError If private key is invalid
     */
    public synchronized HyperliquidClient addPrivateKey(String alias, String privateKey) {
        addApiWalletInternal(new ApiWallet(alias, null, privateKey));
        return this;
    }

    private void addApiWalletInternal(ApiWallet apiWallet) {
        ApiWallet normalizedWallet = normalizeApiWallet(apiWallet);
        exchangesByAlias.put(normalizedWallet.getAlias(), new Exchange(hypeHttpClient, normalizedWallet, info));
        apiWallets.add(normalizedWallet);
    }

    private static ApiWallet normalizeApiWallet(ApiWallet apiWallet) {
        if (apiWallet == null) {
            throw new HypeError("ApiWallet cannot be null.");
        }
        validatePrivateKey(apiWallet.getApiWalletPrivateKey());
        Credentials credentials = Credentials.create(apiWallet.getApiWalletPrivateKey());
        apiWallet.setCredentials(credentials);
        if (apiWallet.getPrimaryWalletAddress() == null || apiWallet.getPrimaryWalletAddress().trim().isEmpty()) {
            apiWallet.setPrimaryWalletAddress(credentials.getAddress());
        }
        if (apiWallet.getAlias() == null || apiWallet.getAlias().trim().isEmpty()) {
            apiWallet.setAlias(apiWallet.getPrimaryWalletAddress());
        }
        return apiWallet;
    }

    private static void validatePrivateKey(String privateKey) {
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

    /**
     * Create a new builder for constructing {@link HyperliquidClient}.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link HyperliquidClient}.
     * <p>
     * Provides fluent configuration for network endpoint, timeout, wallet list,
     * WebSocket usage, and cache warm-up behavior.
     * </p>
     */
    public static class Builder {
        /**
         * API base URL.
         */
        private String baseUrl = Constants.MAINNET_API_URL;

        /**
         * Connect/read/write timeout in seconds. Default is 10.
         */
        private int timeout = 10;

        /**
         * Whether to skip WebSocket initialization. Default is false.
         */
        private boolean skipWs = false;

        /**
         * API wallets collected for client construction.
         */
        private final List<ApiWallet> apiWallets = new ArrayList<>();

        /**
         * Optional preconfigured OkHttpClient.
         */
        private OkHttpClient okHttpClient = null;

        /**
         * Whether to automatically warm up caches. Default is true.
         * <p>
         * When enabled, build() preloads commonly used metadata caches to reduce
         * latency on first API calls.
         * </p>
         */
        private boolean autoWarmUpCache = true;

        /**
         * Set custom API base URL.
         *
         * @param baseUrl API base URL
         * @return Builder instance
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Switch base URL to Hyperliquid testnet endpoint.
         *
         * @return Builder instance
         */
        public Builder testNetUrl() {
            this.baseUrl = Constants.TESTNET_API_URL;
            return this;
        }

        /**
         * Add one API wallet private key.
         * <p>
         * Alias is auto-filled with primary address when not provided.
         * </p>
         *
         * @param privateKey API wallet private key
         * @return Builder instance
         */
        public Builder addPrivateKey(String privateKey) {
            addApiWallet(null, null, privateKey);
            return this;
        }

        /**
         * Add one API wallet private key with explicit alias.
         *
         * @param alias      Wallet alias
         * @param privateKey API wallet private key
         * @return Builder instance
         */
        public Builder addPrivateKey(String alias, String privateKey) {
            addApiWallet(alias, null, privateKey);
            return this;
        }

        /**
         * Batch add private keys.
         *
         * @param pks Private key list
         * @return Builder instance
         */
        public Builder addPrivateKeys(List<String> pks) {
            for (String pk : pks) {
                addPrivateKey(pk);
            }
            return this;
        }

        /**
         * Configure whether to skip WebSocket initialization.
         *
         * @param skipWs true to disable WebSocket features
         * @return Builder instance
         */
        public Builder skipWs(boolean skipWs) {
            this.skipWs = skipWs;
            return this;
        }

        /**
         * Set connect/read/write timeout in seconds.
         *
         * @param timeout Timeout in seconds
         * @return Builder instance
         */
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Add one API wallet object.
         *
         * @param apiWallet API wallet
         * @return Builder instance
         */
        public Builder addApiWallet(ApiWallet apiWallet) {
            apiWallets.add(apiWallet);
            return this;
        }

        /**
         * Add one API wallet by primary wallet address and API wallet private key.
         *
         * @param primaryWalletAddress Primary wallet address
         * @param apiWalletPrivateKey  API wallet private key
         * @return Builder instance
         */
        public Builder addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey) {
            apiWallets.add(new ApiWallet(primaryWalletAddress, apiWalletPrivateKey));
            return this;
        }

        /**
         * Add one API wallet by alias, primary wallet address, and private key.
         *
         * @param alias                Wallet alias
         * @param primaryWalletAddress Primary wallet address
         * @param apiWalletPrivateKey  API wallet private key
         * @return Builder instance
         */
        public Builder addApiWallet(String alias, String primaryWalletAddress, String apiWalletPrivateKey) {
            apiWallets.add(new ApiWallet(alias, primaryWalletAddress, apiWalletPrivateKey));
            return this;
        }

        /**
         * Batch add API wallets.
         *
         * @param apiWallets API wallet list
         * @return Builder instance
         */
        public Builder addApiWallets(List<ApiWallet> apiWallets) {
            this.apiWallets.addAll(apiWallets);
            return this;
        }

        /**
         * Set a preconfigured OkHttpClient instance.
         *
         * @param client OkHttpClient instance
         * @return Builder instance
         */
        public Builder okHttpClient(OkHttpClient client) {
            this.okHttpClient = client;
            return this;
        }

        /**
         * Disable automatic cache warm-up (advanced option).
         * <p>
         * By default, build() warms up cache to improve first-call performance.
         * Disable only in scenarios such as:
         * 1. Application startup time requirements are extremely strict (millisecond
         * level)
         * 2. Network instability where build() must not block
         * 3. Test scenarios that require precise cache behavior control
         * </p>
         *
         * @return Builder instance
         */
        public Builder disableAutoWarmUpCache() {
            this.autoWarmUpCache = false;
            return this;
        }

        private OkHttpClient getOkHttpClient() {
            return okHttpClient != null ? okHttpClient
                    : new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .readTimeout(Duration.ofSeconds(timeout))
                    .writeTimeout(Duration.ofSeconds(timeout))
                    .build();
        }

        /**
         * Build a {@link HyperliquidClient} instance.
         * <p>
         * During build, wallets are normalized and bound to Exchange instances.
         * When auto warm-up is enabled, commonly used metadata caches are loaded.
         * </p>
         *
         * @return Initialized HyperliquidClient
         * @throws HypeError If wallet private key format is invalid
         */
        public HyperliquidClient build() {
            OkHttpClient httpClient = getOkHttpClient();
            HypeHttpClient hypeHttpClient = new HypeHttpClient(baseUrl, httpClient);
            Info info = new Info(baseUrl, hypeHttpClient, skipWs);
            Map<String, Exchange> exchangesByAlias = new LinkedHashMap<>();
            List<ApiWallet> builtApiWallets = new ArrayList<>();
            for (ApiWallet apiWallet : apiWallets) {
                ApiWallet normalizedWallet = normalizeApiWallet(apiWallet);
                exchangesByAlias.put(normalizedWallet.getAlias(), new Exchange(hypeHttpClient, normalizedWallet, info));
                builtApiWallets.add(normalizedWallet);
            }

            // Automatic cache warming (improves first-call performance)
            if (autoWarmUpCache) {
                try {
                    info.warmUpCache();
                } catch (Exception e) {
                    log.warn(
                            "[HyperliquidClient] Warning: Cache warm-up failed, but client is still usable. First API calls may be slower. Error: {}",
                            e.getMessage());
                }
            }

            return new HyperliquidClient(
                    info,
                    hypeHttpClient,
                    exchangesByAlias,
                    builtApiWallets);
        }

    }
}
