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
 * HyperliquidClient unified management client, responsible for order placement,
 * cancellation, transfers, and other L1/L2 operations.
 * Supports management and switching of multiple wallet credentials.
 */
public class HyperliquidClient {

    private static final Logger log = LoggerFactory.getLogger(HyperliquidClient.class);

    /**
     * Info client
     **/
    private final Info info;

    /**
     * HTTP client
     **/
    private final HypeHttpClient hypeHttpClient;

    /**
     * K:Wallet alias V:Exchange
     **/
    private final Map<String, Exchange> exchangesByAlias;

    /**
     * API wallet list
     **/
    private final List<ApiWallet> apiWallets;

    public HyperliquidClient(Info info, HypeHttpClient hypeHttpClient, Map<String, Exchange> exchangesByAlias,
            List<ApiWallet> apiWallets) {
        this.info = info;
        this.hypeHttpClient = hypeHttpClient;
        this.exchangesByAlias = exchangesByAlias;
        this.apiWallets = apiWallets;
    }

    /**
     * Get Info client
     *
     * @return Info client instance
     */
    public Info getInfo() {
        return info;
    }

    /**
     * Get wallet alias to Exchange mapping
     *
     * @return Wallet alias to Exchange mapping
     */
    public synchronized Map<String, Exchange> getExchangesByAlias() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(exchangesByAlias));
    }

    /**
     * Get API wallet list
     *
     * @return API wallet list
     */
    public synchronized List<ApiWallet> getApiWallets() {
        return Collections.unmodifiableList(new ArrayList<>(apiWallets));
    }

    /**
     * Get single Exchange instance, if there are multiple, return the first one
     **/
    public synchronized Exchange getExchange() {
        if (exchangesByAlias.isEmpty()) {
            throw new HypeError("No exchange instances available.");
        }
        return exchangesByAlias.values().iterator().next();
    }

    /**
     * Get Exchange instance by wallet address
     *
     * @param alias Wallet alias or primary wallet address
     * @return Corresponding Exchange instance
     * @throws HypeError If alias does not exist, throw exception and prompt
     *                   available alias list
     **/
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
     * Check if wallet exists for specified address
     *
     * @param alias Wallet alias
     * @return Returns true if exists, false otherwise
     */
    public synchronized boolean hasWallet(String alias) {
        return alias != null && exchangesByAlias.containsKey(alias);
    }

    /**
     * Get all available wallet address collection (immutable)
     *
     * @return Wallet address collection
     */
    public synchronized Set<String> getAvailableAddresses() {
        return apiWallets.stream().map(ApiWallet::getPrimaryWalletAddress).collect(Collectors.toUnmodifiableSet());
    }


    /**
     * Get total number of wallets
     *
     * @return Number of wallets
     */
    public synchronized int getWalletCount() {
        return apiWallets.size();
    }

    /**
     * Get single address (primary address of the first wallet)
     *
     * @return Primary wallet address
     * @throws HypeError If no wallets are available
     */
    public synchronized String getSingleAddress() {
        if (apiWallets == null || apiWallets.isEmpty()) {
            throw new HypeError("No wallets available. Please add at least one wallet.");
        }
        return apiWallets.getFirst().getPrimaryWalletAddress();
    }

    /**
     * Add API wallet dynamically after client is built.
     *
     * @param apiWallet API wallet object
     * @return Current HyperliquidClient instance for fluent calls
     * @throws HypeError If wallet information is invalid
     */
    public synchronized HyperliquidClient addApiWallet(ApiWallet apiWallet) {
        addApiWalletInternal(apiWallet);
        return this;
    }

    /**
     * Add API wallet dynamically after client is built.
     *
     * @param primaryWalletAddress Primary wallet address
     * @param apiWalletPrivateKey  API wallet private key
     * @return Current HyperliquidClient instance for fluent calls
     * @throws HypeError If wallet information is invalid
     */
    public synchronized HyperliquidClient addApiWallet(String primaryWalletAddress, String apiWalletPrivateKey) {
        addApiWalletInternal(new ApiWallet(primaryWalletAddress, apiWalletPrivateKey));
        return this;
    }

    /**
     * Add API wallet dynamically after client is built.
     *
     * @param alias                Wallet alias
     * @param primaryWalletAddress Primary wallet address
     * @param apiWalletPrivateKey  API wallet private key
     * @return Current HyperliquidClient instance for fluent calls
     * @throws HypeError If wallet information is invalid
     */
    public synchronized HyperliquidClient addApiWallet(String alias, String primaryWalletAddress,
            String apiWalletPrivateKey) {
        addApiWalletInternal(new ApiWallet(alias, primaryWalletAddress, apiWalletPrivateKey));
        return this;
    }

    /**
     * Add private key dynamically after client is built.
     *
     * @param privateKey Private key
     * @return Current HyperliquidClient instance for fluent calls
     * @throws HypeError If private key is invalid
     */
    public synchronized HyperliquidClient addPrivateKey(String privateKey) {
        addApiWalletInternal(new ApiWallet(null, privateKey));
        return this;
    }

    /**
     * Add private key with alias dynamically after client is built.
     *
     * @param alias      Wallet alias
     * @param privateKey Private key
     * @return Current HyperliquidClient instance for fluent calls
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        /**
         * API node address
         **/
        private String baseUrl = Constants.MAINNET_API_URL;

        /**
         * Timeout (seconds, default 10 seconds)
         **/
        private int timeout = 10;

        /**
         * Whether to skip WebSocket (default: do not skip)
         **/
        private boolean skipWs = false;

        /**
         * API wallet list
         **/
        private final List<ApiWallet> apiWallets = new ArrayList<>();

        /**
         * OkHttpClient instance
         **/
        private OkHttpClient okHttpClient = null;

        /**
         * Whether to automatically warm up cache (default: enabled)
         * When enabled, build() will automatically load commonly used data (meta,
         * spotMeta, coin mapping) into cache,
         * avoiding delays during first API calls and improving user experience.
         */
        private boolean autoWarmUpCache = true;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder testNetUrl() {
            this.baseUrl = Constants.TESTNET_API_URL;
            return this;
        }

        public Builder addPrivateKey(String privateKey) {
            addApiWallet(null, null, privateKey);
            return this;
        }

        public Builder addPrivateKey(String alias, String privateKey) {
            addApiWallet(alias, null, privateKey);
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

        public Builder addApiWallet(String alias, String primaryWalletAddress, String apiWalletPrivateKey) {
            apiWallets.add(new ApiWallet(alias, primaryWalletAddress, apiWalletPrivateKey));
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

        /**
         * Disable automatic cache warm-up (advanced option).
         * <p>
         * By default, build() will automatically warm up cache to improve performance.
         * Only disable in the following scenarios:
         * 1. Application startup time requirements are extremely strict (millisecond
         * level)
         * 2. Unstable network environment, don't want build() to block
         * 3. Used for testing scenarios, need precise control over cache behavior
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
