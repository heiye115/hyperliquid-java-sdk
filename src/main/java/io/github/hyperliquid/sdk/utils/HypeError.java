package io.github.hyperliquid.sdk.utils;

/**
 * SDK custom exception type definitions.
 */
public class HypeError extends RuntimeException {

    /**
     * Construct base error.
     *
     * @param message Error message
     */
    public HypeError(String message) {
        super(message);
    }

    public HypeError(String message, Throwable e) {
        super(message, e);
    }

    /**
     * Client error (4xx)
     */
    public static class ClientHypeError extends HypeError {
        /**
         * Get HTTP status code.
         */
        private final int statusCode;

        /**
         * Construct client error.
         *
         * @param statusCode HTTP status code
         * @param message    Error message
         */
        public ClientHypeError(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * Get HTTP status code.
         *
         * @return HTTP status code
         */
        public int getStatusCode() {
            return statusCode;
        }
    }

    /**
     * Server error (5xx)
     */
    public static class ServerHypeError extends HypeError {
        /**
         * Get HTTP status code.
         */
        private final int statusCode;

        /**
         * Construct server error.
         *
         * @param statusCode HTTP status code
         * @param message    Error message
         */
        public ServerHypeError(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * Get HTTP status code.
         *
         * @return HTTP status code
         */
        public int getStatusCode() {
            return statusCode;
        }
    }
}