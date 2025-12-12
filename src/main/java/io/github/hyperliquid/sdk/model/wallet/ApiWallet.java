package io.github.hyperliquid.sdk.model.wallet;

import org.web3j.crypto.Credentials;

/**
 * API wallet
 **/
public class ApiWallet {

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
}
