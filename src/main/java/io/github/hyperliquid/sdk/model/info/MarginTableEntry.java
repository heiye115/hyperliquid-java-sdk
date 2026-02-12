package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/** Margin table entry (in the form of [id, detail]) */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@Data
@ToString
public class MarginTableEntry {

    /** Margin table ID (index 0) */
    @JsonProperty(index = 0)
    private Integer id;

    /** Margin table details (index 1) */
    @JsonProperty(index = 1)
    private MarginTableDetail detail;
}