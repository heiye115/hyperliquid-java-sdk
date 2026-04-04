package io.github.hyperliquid.sdk.utils;

/**
 * Immutable retry policy for operations that may be retried with exponential backoff.
 * <p>
 * Values are clamped: {@code maxRetries} is at least 0, {@code maxBackoffMillis} is at least
 * {@code initialBackoffMillis}, and {@code backoffMultiplier} defaults to 2.0 if {@code <= 1.0}.
 * </p>
 */
public final class RetryPolicy {

    private final int maxRetries;
    private final long initialBackoffMillis;
    private final long maxBackoffMillis;
    private final double backoffMultiplier;

    /**
     * @param maxRetries           Maximum retry attempts (0 = no retries beyond the first try)
     * @param initialBackoffMillis First delay before retry (milliseconds)
     * @param maxBackoffMillis     Upper bound on delay between retries (milliseconds)
     * @param backoffMultiplier    Factor applied to increase delay after each failed attempt
     */
    public RetryPolicy(int maxRetries, long initialBackoffMillis, long maxBackoffMillis, double backoffMultiplier) {
        this.maxRetries = Math.max(0, maxRetries);
        this.initialBackoffMillis = Math.max(0, initialBackoffMillis);
        this.maxBackoffMillis = Math.max(initialBackoffMillis, maxBackoffMillis);
        this.backoffMultiplier = backoffMultiplier <= 1.0 ? 2.0 : backoffMultiplier;
    }

    /**
     * @return Policy with 3 retries, 500ms initial backoff, 5000ms cap, multiplier 2.0
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 500, 5000, 2.0);
    }

    /** @return Configured maximum retry count */
    public int getMaxRetries() {
        return maxRetries;
    }

    /** @return Initial backoff delay in milliseconds */
    public long getInitialBackoffMillis() {
        return initialBackoffMillis;
    }

    /** @return Maximum backoff delay in milliseconds */
    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }

    /** @return Multiplier applied to the backoff delay after each failure */
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
}

