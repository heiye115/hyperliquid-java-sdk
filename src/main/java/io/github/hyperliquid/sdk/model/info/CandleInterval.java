package io.github.hyperliquid.sdk.model.info;

import io.github.hyperliquid.sdk.utils.HypeError;

import java.time.Duration;

/**
 * K线时间间隔枚举
 */
public enum CandleInterval {

    // 分钟级别
    MINUTE_1("1m", Duration.ofMinutes(1)),
    MINUTE_3("3m", Duration.ofMinutes(3)),
    MINUTE_5("5m", Duration.ofMinutes(5)),
    MINUTE_15("15m", Duration.ofMinutes(15)),
    MINUTE_30("30m", Duration.ofMinutes(30)),

    // 小时级别
    HOUR_1("1h", Duration.ofHours(1)),
    HOUR_2("2h", Duration.ofHours(2)),
    HOUR_4("4h", Duration.ofHours(4)),
    HOUR_8("8h", Duration.ofHours(8)),
    HOUR_12("12h", Duration.ofHours(12)),

    // 天级别
    DAY_1("1d", Duration.ofDays(1)),
    DAY_3("3d", Duration.ofDays(3)),

    // 周级别
    WEEK_1("1w", Duration.ofDays(7)),

    // 月级别（近似30天）
    MONTH_1("1M", Duration.ofDays(30));

    /**
     *  获取间隔代码（如 "1m", "1h"）
     */
    private final String code;
    /**
     *  获取时间间隔
     */
    private final Duration duration;

    CandleInterval(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    /**
     * 获取间隔代码（如 "1m", "1h"）
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取时间间隔
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * 获取间隔毫秒数
     */
    public long toMillis() {
        return duration.toMillis();
    }

    /**
     * 获取间隔秒数
     */
    public long toSeconds() {
        return duration.toSeconds();
    }

    /**
     * 获取间隔分钟数
     */
    public long toMinutes() {
        return duration.toMinutes();
    }

    /**
     * 根据代码获取枚举实例
     *
     * @param code 间隔代码（如 "1m", "1h"）
     * @return 对应的枚举实例
     * @throws HypeError 若代码不受支持
     */
    public static CandleInterval fromCode(String code) {
        if (code == null) {
            throw new HypeError("Interval code cannot be null");
        }

        for (CandleInterval interval : values()) {
            if (interval.code.equals(code)) {
                return interval;
            }
        }

        throw new HypeError("Unsupported interval code: " + code);
    }

    /**
     * 根据代码获取间隔毫秒数（便捷方法）
     *
     * @param code 间隔代码
     * @return 间隔毫秒数
     * @throws HypeError 若代码不受支持
     */
    public static long toMillis(String code) {
        return fromCode(code).toMillis();
    }


}