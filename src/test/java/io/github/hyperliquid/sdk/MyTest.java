package io.github.hyperliquid.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.FundingHistory;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.TriggerOrderType;
import io.github.hyperliquid.sdk.utils.HypeError;
import io.github.hyperliquid.sdk.utils.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MyTest {

    HyperliquidClient client = HyperliquidClient.builder()
            .testNetUrl()
            .addApiWallet("0x7D71218e32634c93401D87A317acB1396636e339", "0xe84bb15e194bc4f7994d4d9e2e019e05b85424a02b89478c4c0e81135ccd52e3")
            .enableDebugLogs()
            .build();

    /**
     * 生成64位随机字符串
     **/
    @Test
    public void generateRandomCloid() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder cloid = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            int index = (int) (Math.random() * characters.length());
            cloid.append(characters.charAt(index));
        }
        System.out.println(cloid.toString());
    }


    @Test
    public void testFundingHistory() {
        Info info = client.getInfo();
        long start = 1763136000000L;
        long end = 1763532000000L;
        try {
            List<FundingHistory> btc = info.fundingHistory("BTC", start, end);
            System.out.println(btc);
        } catch (HypeError.ServerHypeError e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMarketOrder1() throws JsonProcessingException {
        Exchange exchange = client.getSingleExchange();
        OrderRequest req = OrderRequest.Open.market("ETH", true, 0.01);
        Order order = exchange.order(req);
        System.out.println(JSONUtil.writeValueAsString(order));
    }

    @Test
    public void testOrder() throws JsonProcessingException {
        Exchange exchange = client.getSingleExchange();
        OrderRequest req = OrderRequest.Open.trigger("ETH", true, 0.01, 0.0, 2000.0, Boolean.TRUE, TriggerOrderType.TpslType.SL, Cloid.auto());
        Order order = exchange.order(req);
        System.out.println(JSONUtil.writeValueAsString(order));
    }

}
