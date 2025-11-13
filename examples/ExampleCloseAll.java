import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Exchange;
import io.github.hyperliquid.sdk.model.order.Cloid;
import io.github.hyperliquid.sdk.model.order.Order;
import io.github.hyperliquid.sdk.model.order.Tif;
import io.github.hyperliquid.sdk.utils.HypeError;

public class ExampleCloseAll {
    public static void main(String[] args) {
        String pk = System.getenv("HYPERLIQUID_PRIVATE_KEY");
        if (pk == null || pk.isBlank())
            throw new IllegalStateException("Set HYPERLIQUID_PRIVATE_KEY");

        HyperliquidClient client = HyperliquidClient.builder()
                .testNetUrl()
                .addPrivateKey(pk)
                .build();

        Exchange ex = client.getSingleExchange();
        try {
            Order mkt = ex.closePositionAtMarketAll("ETH");
            System.out.println(mkt.getStatus());

            Order lmt = ex.closePositionLimitAll(Tif.GTC, "ETH", 4000.0, Cloid.auto());
            System.out.println(lmt.getStatus());
        } catch (HypeError e) {
            System.err.println(e.getMessage());
        }
    }
}
