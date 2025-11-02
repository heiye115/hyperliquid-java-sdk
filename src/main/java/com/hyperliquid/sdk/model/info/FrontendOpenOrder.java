package com.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hyperliquid.sdk.model.order.OrderWire;

import java.util.HashMap;
import java.util.Map;

/**
 * 前端未成交订单实体封装。
 *
 * <p>说明：接口返回通常包含原始订单结构以及前端附加字段（如订单 ID、创建时间、
 * 可视化状态等）。本封装将原始订单映射为 {@link OrderWire}，其余字段
 * 通过 {@link #extra} 动态保留，使用者可以无负担地获取所需信息。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FrontendOpenOrder {
    /** 原始订单线格式 */
    private OrderWire order;

    /** 附加字段容器 */
    private Map<String, Object> extensions = new HashMap<>();

    public FrontendOpenOrder() {}

    /** Builder 便于友好构建 */
    public static class Builder {
        private final FrontendOpenOrder o = new FrontendOpenOrder();
        public Builder order(OrderWire ow) { o.order = ow; return this; }
        public Builder putExtra(String k, Object v) { o.extensions.put(k, v); return this; }
        public FrontendOpenOrder build() { return o; }
    }

    @JsonAnySetter
    public void put(String key, Object value) { extensions.put(key, value); }

    @JsonAnyGetter
    public Map<String, Object> any() { return extensions; }

    /** 获取原始订单 */
    public OrderWire getOrder() { return order; }
    /** 设置原始订单 */
    public void setOrder(OrderWire order) { this.order = order; }

    /** 获取附加字段 */
    public Map<String, Object> getExtensions() { return extensions; }
    /** 设置附加字段 */
    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }
}
