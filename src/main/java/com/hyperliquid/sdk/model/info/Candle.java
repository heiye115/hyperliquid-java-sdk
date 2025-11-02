package com.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Candle（K 线）类型模型。
 *
 * 说明：
 * - 官方返回的 K 线字段包含时间戳（t/T）、价格（o/c/h/l）、交易量（v）、间隔（i）、交易次数（n）、币种（s）等；
 * - 为提升容错性，保留未知/新增字段到 {@link #extensions}，避免接口变更导致反序列化失败；
 * - 数值型价格/交易量按官方返回保持字符串形式，以避免精度损失与解析异常，使用方可自行转为 BigDecimal。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candle {
    /** 结束时间戳（毫秒），对应官方字段 "T" */
    @JsonProperty("T")
    private Long T;
    /** 起始时间戳（毫秒），对应官方字段 "t" */
    @JsonProperty("t")
    private Long t;
    /** 收盘价（字符串） */
    @JsonProperty("c")
    private String c;
    /** 最高价（字符串） */
    @JsonProperty("h")
    private String h;
    /** 最低价（字符串） */
    @JsonProperty("l")
    private String l;
    /** 开盘价（字符串） */
    @JsonProperty("o")
    private String o;
    /** 交易量（字符串） */
    @JsonProperty("v")
    private String v;
    /** 间隔字符串（如 "1m"、"15m"、"1h"、"1d" 等），对应官方字段 "i" */
    @JsonProperty("i")
    private String i;
    /** 币种名称（如 "BTC"），对应官方字段 "s" */
    @JsonProperty("s")
    private String s;
    /** 交易次数（整数），对应官方字段 "n" */
    @JsonProperty("n")
    private Integer n;

    /** 扩展字段容器（用于兼容返回结构变化） */
    private Map<String, Object> extensions = new HashMap<>();

    public Candle() {
    }

    /**
     * Builder 便于友好构建。
     */
    public static class Builder {
        private final Candle k = new Candle();

        public Builder T(Long v) {
            k.T = v;
            return this;
        }

        public Builder t(Long v) {
            k.t = v;
            return this;
        }

        public Builder c(String v) {
            k.c = v;
            return this;
        }

        public Builder h(String v) {
            k.h = v;
            return this;
        }

        public Builder l(String v) {
            k.l = v;
            return this;
        }

        public Builder o(String v) {
            k.o = v;
            return this;
        }

        public Builder v(String v) {
            k.v = v;
            return this;
        }

        public Builder i(String v) {
            k.i = v;
            return this;
        }

        public Builder s(String v) {
            k.s = v;
            return this;
        }

        public Builder n(Integer v) {
            k.n = v;
            return this;
        }

        public Builder putExtra(String k1, Object v1) {
            k.extensions.put(k1, v1);
            return this;
        }

        public Candle build() {
            return k;
        }
    }

    @JsonAnySetter
    public void put(String key, Object value) {
        extensions.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return extensions;
    }

    // Getter / Setter（含方法注释）

    /** 获取结束时间戳（毫秒） */
    @JsonProperty("T")
    public Long getT() {
        return T;
    }

    /** 设置结束时间戳（毫秒） */
    @JsonProperty("T")
    public void setT(Long tEnd) {
        this.T = tEnd;
    }

    /** 获取起始时间戳（毫秒） */
    @JsonProperty("t")
    public Long getTStart() {
        return t;
    }

    /** 设置起始时间戳（毫秒） */
    @JsonProperty("t")
    public void setTStart(Long tStart) {
        this.t = tStart;
    }

    /** 获取收盘价（字符串） */
    public String getC() {
        return c;
    }

    /** 设置收盘价（字符串） */
    public void setC(String c) {
        this.c = c;
    }

    /** 获取最高价（字符串） */
    public String getH() {
        return h;
    }

    /** 设置最高价（字符串） */
    public void setH(String h) {
        this.h = h;
    }

    /** 获取最低价（字符串） */
    public String getL() {
        return l;
    }

    /** 设置最低价（字符串） */
    public void setL(String l) {
        this.l = l;
    }

    /** 获取开盘价（字符串） */
    public String getO() {
        return o;
    }

    /** 设置开盘价（字符串） */
    public void setO(String o) {
        this.o = o;
    }

    /** 获取交易量（字符串） */
    public String getV() {
        return v;
    }

    /** 设置交易量（字符串） */
    public void setV(String v) {
        this.v = v;
    }

    /** 获取间隔字符串（如 "15m"） */
    public String getI() {
        return i;
    }

    /** 设置间隔字符串（如 "15m"） */
    public void setI(String i) {
        this.i = i;
    }

    /** 获取币种名称（如 "BTC"） */
    public String getS() {
        return s;
    }

    /** 设置币种名称（如 "BTC"） */
    public void setS(String s) {
        this.s = s;
    }

    /** 获取交易次数 */
    public Integer getN() {
        return n;
    }

    /** 设置交易次数 */
    public void setN(Integer n) {
        this.n = n;
    }

    /** 获取扩展字段映射 */
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /** 设置扩展字段映射 */
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
}
