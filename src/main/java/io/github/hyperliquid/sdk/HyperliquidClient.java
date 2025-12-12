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


/**
 * HyperliquidClient unified management client, responsible for order placement, cancellation, transfers, and other L1/L2 operations.
 * Supports management and switching of multiple wallet credentials.
 */
public class HyperliquidClient {

    private static final Logger log = LoggerFactory.getLogger(HyperliquidClient.class);

    /**
     * Info client
     **/
    private final Info info;

    /**
     * K:Wallet address V:Exchange
     **/
    private final Map<String, Exchange> exchangesByAddress;

    /**
     * API wallet list
     **/
    private final List<ApiWallet> apiWallets;


    public HyperliquidClient(Info info, Map<String, Exchange> exchangesByAddress, List<ApiWallet> apiWallets) {
        this.info = info;
        this.exchangesByAddress = exchangesByAddress;
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
     * Get wallet address to Exchange mapping
     *
     * @return Wallet address to Exchange mapping
     */
    public Map<String, Exchange> getExchangesByAddress() {
        return exchangesByAddress;
    }

    /**
     * Get API wallet list
     *
     * @return API wallet list
     */
    public List<ApiWallet> getApiWallets() {
        return apiWallets;
    }

    /**
     * Get single Exchange, if there are multiple, return the first one
     * 
     * @deprecated This method is deprecated, please use {@link #getExchange()} instead
     * @return Exchange instance
     */
    @Deprecated
    public Exchange getSingleExchange() {
        return getExchange();
    }

    /**
     * Get single Exchange, if there are multiple, return the first one
     **/
    public Exchange getExchange() {
        if (exchangesByAddress.isEmpty()) {
            throw new HypeError("No exchange instances available.");
        }
        return exchangesByAddress.values().iterator().next();
    }

    /**
     * Get Exchange instance by wallet address
     *
     * @param address Wallet address (42-character hexadecimal format, starting with 0x)
     * @return Corresponding Exchange instance
     * @throws HypeError If address does not exist, throw exception and prompt available address list
     * @deprecated This method is deprecated, please use {@link #getExchange(String)} instead
     **/
    @Deprecated
    public Exchange useExchange(String address) {
        return getExchange(address);
    }

    /**
     * Get Exchange instance by wallet address
     *
     * @param address Wallet address (42-character hexadecimal format, starting with 0x)
     * @return Corresponding Exchange instance
     * @throws HypeError If address does not exist, throw exception and prompt available address list
     **/
    public Exchange getExchange(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new HypeError("Wallet address cannot be null or empty.");
        }
        Exchange ex = exchangesByAddress.get(address);
        if (ex == null) {
            String availableAddresses = String.join(", ", exchangesByAddress.keySet());
            throw new HypeError(String.format("Wallet address '%s' not found. Available addresses: [%s]", address, availableAddresses.isEmpty() ? "none" : availableAddresses
            ));
        }
        return ex;
    }

    /**
     * Check if wallet exists for specified address
     *
     * @param address Wallet address
     * @return Returns true if exists, false otherwise
     */
    public boolean hasWallet(String address) {
        return address != null && exchangesByAddress.containsKey(address);
    }

    /**
     * Get all available wallet address collection (immutable)
     *
     * @return Wallet address collection
     */
    public Set<String> getAvailableAddresses() {
        return Collections.unmodifiableSet(exchangesByAddress.keySet());
    }

    /**
     * Get all wallet address list (in registration order)
     *
     * @return Wallet address list
     */
    public List<String> listWallets() {
        return new ArrayList<>(exchangesByAddress.keySet());
    }

    /**
     * Get Exchange instance by index (in registration order)
     *
     * @param index Index (starting from 0)
     * @return Corresponding Exchange instance
     * @throws HypeError If index is out of bounds
     */
    public Exchange getExchangeByIndex(int index) {
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
     * Get total number of wallets
     *
     * @return Number of wallets
     */
    public int getWalletCount() {
        return exchangesByAddress.size();
    }

    /**
     * Get single address (primary address of the first wallet)
     *
     * @return Primary wallet address
     * @throws HypeError If no wallets are available
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
         * When enabled, build() will automatically load commonly used data (meta, spotMeta, coin mapping) into cache,
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

        /**
         * Disable automatic cache warm-up (advanced option).
         * <p>
         * By default, build() will automatically warm up cache to improve performance.
         * Only disable in the following scenarios:
         * 1. Application startup time requirements are extremely strict (millisecond level)
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
                    apiWallet.setCredentials(credentials);
                    if (apiWallet.getPrimaryWalletAddress() == null || apiWallet.getPrimaryWalletAddress().trim().isEmpty()) {
                        apiWallet.setPrimaryWalletAddress(credentials.getAddress());
                    }
                    exchangesByAddress.put(apiWallet.getPrimaryWalletAddress(), new Exchange(hypeHttpClient, apiWallet, info));
                }
            }

            // 自动缓存预热（提升首次调用性能）
            if (autoWarmUpCache) {
                try {
                    info.warmUpCache();
                } catch (Exception e) {
                    log.warn("[HyperliquidClient] Warning: Cache warm-up failed, but client is still usable. First API calls may be slower. Error: {}", e.getMessage());
                }
            }

            return new HyperliquidClient(
                    info,
                    Collections.unmodifiableMap(exchangesByAddress),
                    Collections.unmodifiableList(new ArrayList<>(this.apiWallets))
            );
        }


        /**
         * Private key validation logic:
         * 1. Not empty
         * 2. Length and character set are valid
         * 3. Can be parsed by Web3j into ECKeyPair
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
