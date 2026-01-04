package io.github.hyperliquid.sdk.model.wallet;

import org.web3j.crypto.Credentials;

/**
 * API wallet
 **/
public class ApiWallet {

    /**
     * Wallet alias default to primary wallet address
     */
    private String alias;

    /**
     * Primary wallet address (Primary Wallet Address)
     */
    private String primaryWalletAddress;

    /**
     * API wallet corresponding private key (used for signing transaction requests)
     */
    private String apiWalletPrivateKey;

    /**
     * Credentials
     **/
    private Credentials credentials;

    /**
     * Constructor
     *
     * @param alias                wallet alias
     * @param primaryWalletAddress primary wallet address
     * @param apiWalletPrivateKey  api wallet private key
     */
    public ApiWallet(String alias, String primaryWalletAddress, String apiWalletPrivateKey) {
        this.alias = alias;
        this.primaryWalletAddress = primaryWalletAddress;
        this.apiWalletPrivateKey = apiWalletPrivateKey;
    }

    public ApiWallet(String primaryWalletAddress, String apiWalletPrivateKey) {
        this.primaryWalletAddress = primaryWalletAddress;
        this.apiWalletPrivateKey = apiWalletPrivateKey;
    }

    public ApiWallet(String privateKey) {
        this.apiWalletPrivateKey = privateKey;
    }

    public String getPrimaryWalletAddress() {
        return primaryWalletAddress;
    }

    public void setPrimaryWalletAddress(String primaryWalletAddress) {
        this.primaryWalletAddress = primaryWalletAddress;
    }

    public String getApiWalletPrivateKey() {
        return apiWalletPrivateKey;
    }

    public void setApiWalletPrivateKey(String apiWalletPrivateKey) {
        this.apiWalletPrivateKey = apiWalletPrivateKey;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
