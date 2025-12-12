package io.github.hyperliquid.sdk.config;

/**
 * Cache configuration class, used to customize cache parameters for Info client.
 */
public class CacheConfig {

    /**
     * Meta cache maximum capacity (default 20)
     */
    private int metaCacheMaxSize = 20;

    /**
     * SpotMeta cache maximum capacity (default 10)
     */
    private int spotMetaCacheMaxSize = 10;

    /**
     * Cache expiration time (minutes, default 30 minutes)
     */
    private long expireAfterWriteMinutes = 30;

    /**
     * Whether to enable cache statistics (default true)
     */
    private boolean recordStats = true;

    /**
     * Default constructor (uses default configuration)
     */
    public CacheConfig() {
    }

    /**
     * Full parameter constructor
     *
     * @param metaCacheMaxSize          Meta cache maximum capacity
     * @param spotMetaCacheMaxSize      SpotMeta cache maximum capacity
     * @param expireAfterWriteMinutes   Cache expiration time (minutes)
     * @param recordStats               Whether to enable cache statistics
     */
    public CacheConfig(int metaCacheMaxSize, int spotMetaCacheMaxSize, long expireAfterWriteMinutes, boolean recordStats) {
        this.metaCacheMaxSize = metaCacheMaxSize;
        this.spotMetaCacheMaxSize = spotMetaCacheMaxSize;
        this.expireAfterWriteMinutes = expireAfterWriteMinutes;
        this.recordStats = recordStats;
    }

    /**
     * Create default configuration
     *
     * @return Default CacheConfig instance
     */
    public static CacheConfig defaultConfig() {
        return new CacheConfig();
    }

    /**
     * Builder pattern builder
     *
     * @return Builder instance
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
     * CacheConfig Builder class
     */
    public static class Builder {
        private int metaCacheMaxSize = 20;
        private int spotMetaCacheMaxSize = 10;
        private long expireAfterWriteMinutes = 30;
        private boolean recordStats = true;

        /**
         * Set Meta cache maximum capacity
         *
         * @param maxSize Maximum capacity
         * @return Builder instance
         */
        public Builder metaCacheMaxSize(int maxSize) {
            this.metaCacheMaxSize = maxSize;
            return this;
        }

        /**
         * Set SpotMeta cache maximum capacity
         *
         * @param maxSize Maximum capacity
         * @return Builder instance
         */
        public Builder spotMetaCacheMaxSize(int maxSize) {
            this.spotMetaCacheMaxSize = maxSize;
            return this;
        }

        /**
         * Set cache expiration time (minutes)
         *
         * @param minutes Expiration time
         * @return Builder instance
         */
        public Builder expireAfterWriteMinutes(long minutes) {
            this.expireAfterWriteMinutes = minutes;
            return this;
        }

        /**
         * Set whether to enable cache statistics
         *
         * @param recordStats Whether to enable
         * @return Builder instance
         */
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        /**
         * Build CacheConfig instance
         *
         * @return CacheConfig instance
         */
        public CacheConfig build() {
            return new CacheConfig(metaCacheMaxSize, spotMetaCacheMaxSize, expireAfterWriteMinutes, recordStats);
        }
    }
}
