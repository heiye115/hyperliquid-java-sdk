package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** Metadata and asset contexts array (in the form of [meta, assetCtxs]) */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@Data
@AllArgsConstructor
public class MetaAndAssetCtxs {
    /** Index 0: market metadata */
    @JsonProperty(index = 0)
    private MetaInfo metaInfo;

    /** Index 1: list of asset contexts */
    @JsonProperty(index = 1)
    private List<AssetCtx> assetCtxs;

}