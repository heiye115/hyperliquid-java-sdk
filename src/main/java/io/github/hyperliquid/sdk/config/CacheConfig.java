package io.github.hyperliquid.sdk.config;

/**
 * 缓存配置类，用于自定义 Info 客户端的缓存参数。
 */
public class CacheConfig {

    /**
     * Meta 缓存最大容量（默认 20）
     */
    private int metaCacheMaxSize = 20;

    /**
     * SpotMeta 缓存最大容量（默认 10）
     */
    private int spotMetaCacheMaxSize = 10;

    /**
     * 缓存过期时间（分钟，默认 30 分钟）
     */
    private long expireAfterWriteMinutes = 30;

    /**
     * 是否启用缓存统计（默认 true）
     */
    private boolean recordStats = true;

    /**
     * 默认构造函数（使用默认配置）
     */
    public CacheConfig() {
    }

    /**
     * 全参构造函数
     *
     * @param metaCacheMaxSize          Meta 缓存最大容量
     * @param spotMetaCacheMaxSize      SpotMeta 缓存最大容量
     * @param expireAfterWriteMinutes   缓存过期时间（分钟）
     * @param recordStats               是否启用缓存统计
     */
    public CacheConfig(int metaCacheMaxSize, int spotMetaCacheMaxSize, long expireAfterWriteMinutes, boolean recordStats) {
        this.metaCacheMaxSize = metaCacheMaxSize;
        this.spotMetaCacheMaxSize = spotMetaCacheMaxSize;
        this.expireAfterWriteMinutes = expireAfterWriteMinutes;
        this.recordStats = recordStats;
    }

    /**
     * 创建默认配置
     *
     * @return 默认 CacheConfig 实例
     */
    public static CacheConfig defaultConfig() {
        return new CacheConfig();
    }

    /**
     * Builder 模式构建器
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getMetaCacheMaxSize() {
        return metaCacheMaxSize;
    }

    public void setMetaCacheMaxSize(int metaCacheMaxSize) {
        this.metaCacheMaxSize = metaCacheMaxSize;
    }

    public int getSpotMetaCacheMaxSize() {
        return spotMetaCacheMaxSize;
    }

    public void setSpotMetaCacheMaxSize(int spotMetaCacheMaxSize) {
        this.spotMetaCacheMaxSize = spotMetaCacheMaxSize;
    }

    public long getExpireAfterWriteMinutes() {
        return expireAfterWriteMinutes;
    }

    public void setExpireAfterWriteMinutes(long expireAfterWriteMinutes) {
        this.expireAfterWriteMinutes = expireAfterWriteMinutes;
    }

    public boolean isRecordStats() {
        return recordStats;
    }

    public void setRecordStats(boolean recordStats) {
        this.recordStats = recordStats;
    }

    /**
     * CacheConfig Builder 类
     */
    public static class Builder {
        private int metaCacheMaxSize = 20;
        private int spotMetaCacheMaxSize = 10;
        private long expireAfterWriteMinutes = 30;
        private boolean recordStats = true;

        /**
         * 设置 Meta 缓存最大容量
         *
         * @param maxSize 最大容量
         * @return Builder 实例
         */
        public Builder metaCacheMaxSize(int maxSize) {
            this.metaCacheMaxSize = maxSize;
            return this;
        }

        /**
         * 设置 SpotMeta 缓存最大容量
         *
         * @param maxSize 最大容量
         * @return Builder 实例
         */
        public Builder spotMetaCacheMaxSize(int maxSize) {
            this.spotMetaCacheMaxSize = maxSize;
            return this;
        }

        /**
         * 设置缓存过期时间（分钟）
         *
         * @param minutes 过期时间
         * @return Builder 实例
         */
        public Builder expireAfterWriteMinutes(long minutes) {
            this.expireAfterWriteMinutes = minutes;
            return this;
        }

        /**
         * 设置是否启用缓存统计
         *
         * @param recordStats 是否启用
         * @return Builder 实例
         */
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        /**
         * 构建 CacheConfig 实例
         *
         * @return CacheConfig 实例
         */
        public CacheConfig build() {
            return new CacheConfig(metaCacheMaxSize, spotMetaCacheMaxSize, expireAfterWriteMinutes, recordStats);
        }
    }
}
