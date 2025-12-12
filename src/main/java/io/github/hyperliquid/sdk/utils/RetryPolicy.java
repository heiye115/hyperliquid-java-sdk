package io.github.hyperliquid.sdk.utils;

/**
 * Retry policy configuration, supports exponential backoff.
 */
public final class RetryPolicy {

    private final int maxRetries;
    private final long initialBackoffMillis;
    private final long maxBackoffMillis;
    private final double backoffMultiplier;

    public RetryPolicy(int maxRetries, long initialBackoffMillis, long maxBackoffMillis, double backoffMultiplier) {
        this.maxRetries = Math.max(0, maxRetries);
        this.initialBackoffMillis = Math.max(0, initialBackoffMillis);
        this.maxBackoffMillis = Math.max(initialBackoffMillis, maxBackoffMillis);
        this.backoffMultiplier = backoffMultiplier <= 1.0 ? 2.0 : backoffMultiplier;
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 500, 5000, 2.0);
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getInitialBackoffMillis() {
        return initialBackoffMillis;
    }

    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
}

