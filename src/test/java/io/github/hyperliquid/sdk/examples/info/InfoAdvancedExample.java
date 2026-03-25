package io.github.hyperliquid.sdk.examples.info;

import io.github.hyperliquid.sdk.HyperliquidClient;
import io.github.hyperliquid.sdk.apis.Info;
import io.github.hyperliquid.sdk.model.info.SpotMetaAndAssetCtxs;

public class InfoAdvancedExample {
    public static void main(String[] args) {
        String user = System.getenv("USER_ADDRESS");
        if (user == null) {
            throw new IllegalStateException("Set USER_ADDRESS");
        }

        HyperliquidClient client = HyperliquidClient.builder().testNetUrl().build();
        Info info = client.getInfo();

        SpotMetaAndAssetCtxs spotMetaAndAssetCtxs = info.spotMetaAndAssetCtxsTyped();
        System.out.println(spotMetaAndAssetCtxs.getSpotMeta().getUniverse().size());
        System.out.println(info.fundingHistory("ETH", System.currentTimeMillis() - 3_600_000L, (Long) null));
        System.out.println(info.userNonFundingLedgerUpdates(user, System.currentTimeMillis() - 3_600_000L, (Long) null));
        System.out.println(info.historicalOrders(user));
        System.out.println(info.userTwapSliceFills(user));
        System.out.println(info.querySpotDeployAuctionStatus(user));
    }
}
