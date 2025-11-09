package io.github.hyperliquid.sdk;

import io.github.hyperliquid.sdk.model.info.ClearinghouseState;
import io.github.hyperliquid.sdk.model.info.UpdateLeverage;
import org.junit.jupiter.api.Test;

 
public class SimpleTest {

    String privateKey = "your_private_key_here";
    ExchangeManager manager = ExchangeManager.builder().addPrivateKey(privateKey).enableDebugLogs().build();

    @Test
    public void updateLeverage() {
        UpdateLeverage leverage = manager.getSingleExchangeClient().updateLeverage("BTC", true, 10);
        System.out.println(leverage);

        /*return to the result:
         POST: https://api.hyperliquid.xyz/exchange
         Request: {"action":{"type":"updateLeverage","asset":0,"isCross":true,"leverage":10},"nonce":1762549309755,"signature":{"r":"xxx","s":"xxx","v":27},"vaultAddress":null,"expiresAfter":null}
         Response: {"status":"ok","response":{"type":"default"}}
         UpdateLeverage{status='ok', response=Response{type='default'}}
        * */
    }

    @Test
    public void clearinghouseState() {
        ClearinghouseState clearinghouseState = manager.getInfoClient().clearinghouseState(manager.getSingleAddress());
        System.out.println(clearinghouseState);
    }
}
