import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.OrderRequest;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;

public class ExampleLimitOpen {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank())
            throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Exchange ex = client.getSingleExchange();
        OrderRequest req = OrderRequest.Open.limit(Tif.GTC, "ETH", true, 0.01, 1800.0);
        try {
            Order order = ex.order(req);
            System.out.println(order.getStatus());
        } catch (HypeError e) {
            System.err.println(e.getMessage());
        }
    }
}
