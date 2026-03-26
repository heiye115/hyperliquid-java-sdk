package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.hyperliquid.sdk.model.userabstraction.UserAbstractionMode;
import io.github.hyperliquid.sdk.model.userabstraction.UserSetAbstraction;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.junit.jupiter.api.Test;

public class UserAbstractionTest {

    /**
     * Private key (testnet)
     * your_private_key_here
     */
    String privateKey = "your_private_key_here";

    /**
     * Unified client (testnet)
     */
    HyperliquidClient client = HyperliquidClient.builder()
            .testNetUrl()
            .addPrivateKey(privateKey)
            .build();


    /**
     * Test user abstraction
     **/
    @Test
    public void userAbstraction() {
        String abstraction = client.getInfo().userAbstraction("0x00000000000000000000000000000000");
        System.out.println(abstraction);
    }

    /**
     * Test user abstraction
     **/
    @Test
    public void userSetAbstraction() throws JsonProcessingException {
        UserSetAbstraction abstraction = client.getExchange().userSetAbstraction("0x00000000000000000000000000000000", UserAbstractionMode.UNIFIED_ACCOUNT, null);
        System.out.println(JSONUtil.writeValueAsString(abstraction));
    }
}
