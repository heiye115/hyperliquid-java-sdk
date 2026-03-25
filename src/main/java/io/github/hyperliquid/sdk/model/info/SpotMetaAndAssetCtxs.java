package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class SpotMetaAndAssetCtxs {
    @JsonProperty(index = 0)
    private SpotMeta spotMeta;

    @JsonProperty(index = 1)
    private List<SpotAssetCtx> assetCtxs;

    public SpotMetaAndAssetCtxs() {
    }

    public SpotMetaAndAssetCtxs(SpotMeta spotMeta, List<SpotAssetCtx> assetCtxs) {
        this.spotMeta = spotMeta;
        this.assetCtxs = assetCtxs;
    }

    public SpotMeta getSpotMeta() {
        return spotMeta;
    }

    public void setSpotMeta(SpotMeta spotMeta) {
        this.spotMeta = spotMeta;
    }

    public List<SpotAssetCtx> getAssetCtxs() {
        return assetCtxs;
    }

    public void setAssetCtxs(List<SpotAssetCtx> assetCtxs) {
        this.assetCtxs = assetCtxs;
    }
}
