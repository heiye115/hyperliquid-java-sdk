package io.github.hyperliquid.sdk.model.wallet;

/**
 * API 钱包
 **/
public class ApiWallet {

    /**
     * 主钱包地址（Primary Wallet Address）
     */
    private String primaryWalletAddress;

    /**
     * API 钱包对应的私钥（用于签名交易请求）
     */
    private String apiWalletPrivateKey;

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
}
