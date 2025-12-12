package io.github.hyperliquid.sdk.model.info;

import io.github.hyperliquid.sdk.utils.HypeError;

import java.time.Duration;

/**
 * Candle time interval enum
 */
public enum CandleInterval {

    // Minute level
    MINUTE_1("1m", Duration.ofMinutes(1)),
    MINUTE_3("3m", Duration.ofMinutes(3)),
    MINUTE_5("5m", Duration.ofMinutes(5)),
    MINUTE_15("15m", Duration.ofMinutes(15)),
    MINUTE_30("30m", Duration.ofMinutes(30)),

    // Hour level
    HOUR_1("1h", Duration.ofHours(1)),
    HOUR_2("2h", Duration.ofHours(2)),
    HOUR_4("4h", Duration.ofHours(4)),
    HOUR_8("8h", Duration.ofHours(8)),
    HOUR_12("12h", Duration.ofHours(12)),

    // Day level
    DAY_1("1d", Duration.ofDays(1)),
    DAY_3("3d", Duration.ofDays(3)),

    // Week level
    WEEK_1("1w", Duration.ofDays(7)),

    // Month level (approximately 30 days)
    MONTH_1("1M", Duration.ofDays(30));

    /**
     *  Get interval code (e.g., "1m", "1h")
     */
    private final String code;
    /**
     *  Get time interval
     */
    private final Duration duration;

    CandleInterval(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    /**
     * Get interval code (e.g., "1m", "1h")
     */
    public String getCode() {
        return code;
    }

    /**
     * Get time interval
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Get interval milliseconds
     */
    public long toMillis() {
        return duration.toMillis();
    }

    /**
     * Get interval seconds
     */
    public long toSeconds() {
        return duration.toSeconds();
    }

    /**
     * Get interval minutes
     */
    public long toMinutes() {
        return duration.toMinutes();
    }

    /**
     * Get enum instance by code
     *
     * @param code interval code (e.g., "1m", "1h")
     * @return corresponding enum instance
     * @throws HypeError if code is not supported
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
     * Get interval milliseconds by code (convenience method)
     *
     * @param code interval code
     * @return interval milliseconds
     * @throws HypeError if code is not supported
     */
    public static long toMillis(String code) {
        return fromCode(code).toMillis();
    }


}