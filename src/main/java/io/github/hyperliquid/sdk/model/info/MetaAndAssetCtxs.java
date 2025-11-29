package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 元数据与资产上下文数组（形如 [meta, assetCtxs]） */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class MetaAndAssetCtxs {
    /** 索引 0：市场元数据 */
    @JsonProperty(index = 0)
    private MetaInfo metaInfo;

    /** 索引 1：各资产上下文列表 */
    @JsonProperty(index = 1)
    private List<AssetCtx> assetCtxs;

    /** 无参构造函数 */
    public MetaAndAssetCtxs() {
    }

    /** 全参构造函数 */
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