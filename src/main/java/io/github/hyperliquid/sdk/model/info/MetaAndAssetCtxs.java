package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Metadata and asset contexts array (in the form of [meta, assetCtxs]) */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class MetaAndAssetCtxs {
    /** Index 0: market metadata */
    @JsonProperty(index = 0)
    private MetaInfo metaInfo;

    /** Index 1: list of asset contexts */
    @JsonProperty(index = 1)
    private List<AssetCtx> assetCtxs;

    /** No-argument constructor */
    public MetaAndAssetCtxs() {
    }

    /** Full-parameter constructor */
    public MetaAndAssetCtxs(MetaInfo metaInfo, List<AssetCtx> assetCtxs) {
        this.metaInfo = metaInfo;
        this.assetCtxs = assetCtxs;
    }

    // Getter and Setter methods
    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(MetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public List<AssetCtx> getAssetCtxs() {
        return assetCtxs;
    }

    public void setAssetCtxs(List<AssetCtx> assetCtxs) {
        this.assetCtxs = assetCtxs;
    }
}