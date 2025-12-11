package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Margin table entry (in the form of [id, detail]) */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class MarginTableEntry {

    /** Margin table ID (index 0) */
    @JsonProperty(index = 0)
    private Integer id;

    /** Margin table details (index 1) */
    @JsonProperty(index = 1)
    private MarginTableDetail detail;

    /** No-argument constructor */
    public MarginTableEntry() {
    }

    // Getter and Setter methods
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MarginTableDetail getDetail() {
        return detail;
    }

    public void setDetail(MarginTableDetail detail) {
        this.detail = detail;
    }
}