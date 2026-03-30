package io.github.hyperliquid.sdk.config;

/**
 * Cache configuration for the Info service.
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
     * allMids cache maximum capacity (default 64)
     */
    private int allMidsCacheMaxSize = 64;

    /**
     * Cache expiration time (minutes, default 240 minutes)
     */
    private long expireAfterWriteMinutes = 240;

    /**
     * Whether to enable cache statistics (default true)
     */
    private boolean recordStats = true;

    /**
     * Creates a configuration with default values.
     */
    public CacheConfig() {
    }

    /**
     * Creates a configuration with explicit values.
     *
     * @param metaCacheMaxSize        Meta cache maximum capacity
     * @param spotMetaCacheMaxSize    SpotMeta cache maximum capacity
     * @param allMidsCacheMaxSize     allMids cache maximum capacity
     * @param expireAfterWriteMinutes Cache expiration time (minutes)
     * @param recordStats             Whether to enable cache statistics
     */
    public CacheConfig(int metaCacheMaxSize, int spotMetaCacheMaxSize, int allMidsCacheMaxSize,
            long expireAfterWriteMinutes, boolean recordStats) {
        this.metaCacheMaxSize = metaCacheMaxSize;
        this.spotMetaCacheMaxSize = spotMetaCacheMaxSize;
        this.allMidsCacheMaxSize = allMidsCacheMaxSize;
        this.expireAfterWriteMinutes = expireAfterWriteMinutes;
        this.recordStats = recordStats;
    }

    /**
     * Returns the default cache configuration.
     *
     * @return Default CacheConfig instance
     */
    public static CacheConfig defaultConfig() {
        return new CacheConfig();
    }

    /**
     * Returns a builder for CacheConfig.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the maximum size of the meta cache.
     *
     * @return Meta cache maximum size
     */
    public int getMetaCacheMaxSize() {
        return metaCacheMaxSize;
    }

    /**
     * Sets the maximum size of the meta cache.
     *
     * @param metaCacheMaxSize Meta cache maximum size
     */
    public void setMetaCacheMaxSize(int metaCacheMaxSize) {
        this.metaCacheMaxSize = metaCacheMaxSize;
    }

    /**
     * Returns the maximum size of the spot meta cache.
     *
     * @return Spot meta cache maximum size
     */
    public int getSpotMetaCacheMaxSize() {
        return spotMetaCacheMaxSize;
    }

    /**
     * Sets the maximum size of the spot meta cache.
     *
     * @param spotMetaCacheMaxSize Spot meta cache maximum size
     */
    public void setSpotMetaCacheMaxSize(int spotMetaCacheMaxSize) {
        this.spotMetaCacheMaxSize = spotMetaCacheMaxSize;
    }

    /**
     * Returns the maximum size of the allMids cache.
     *
     * @return allMids cache maximum size
     */
    public int getAllMidsCacheMaxSize() {
        return allMidsCacheMaxSize;
    }

    /**
     * Sets the maximum size of the allMids cache.
     *
     * @param allMidsCacheMaxSize allMids cache maximum size
     */
    public void setAllMidsCacheMaxSize(int allMidsCacheMaxSize) {
        this.allMidsCacheMaxSize = allMidsCacheMaxSize;
    }

    /**
     * Returns cache expiration time in minutes.
     *
     * @return Expiration time in minutes
     */
    public long getExpireAfterWriteMinutes() {
        return expireAfterWriteMinutes;
    }

    /**
     * Sets cache expiration time in minutes.
     *
     * @param expireAfterWriteMinutes Expiration time in minutes
     */
    public void setExpireAfterWriteMinutes(long expireAfterWriteMinutes) {
        this.expireAfterWriteMinutes = expireAfterWriteMinutes;
    }

    /**
     * Returns whether cache statistics are enabled.
     *
     * @return true if statistics are enabled
     */
    public boolean isRecordStats() {
        return recordStats;
    }

    /**
     * Sets whether cache statistics are enabled.
     *
     * @param recordStats true to enable statistics
     */
    public void setRecordStats(boolean recordStats) {
        this.recordStats = recordStats;
    }

    /**
     * CacheConfig Builder class
     */
    public static class Builder {
        private int metaCacheMaxSize = 20;
        private int spotMetaCacheMaxSize = 10;
        private int allMidsCacheMaxSize = 64;
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
         * Set allMids cache maximum capacity
         *
         * @param maxSize Maximum capacity
         * @return Builder instance
         */
        public Builder allMidsCacheMaxSize(int maxSize) {
            this.allMidsCacheMaxSize = maxSize;
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
            return new CacheConfig(metaCacheMaxSize, spotMetaCacheMaxSize, allMidsCacheMaxSize,
                    expireAfterWriteMinutes, recordStats);
        }
    }
}
