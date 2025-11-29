package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 保证金表条目（形如 [id, detail]） */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class MarginTableEntry {

    /** 保证金表 ID（索引 0） */
    @JsonProperty(index = 0)
    private Integer id;

    /** 保证金表详情（索引 1） */
    @JsonProperty(index = 1)
    private MarginTableDetail detail;

    /** 无参构造函数 */
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