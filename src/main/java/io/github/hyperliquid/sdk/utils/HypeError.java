package io.github.hyperliquid.sdk.utils;

/**
 * Base exception type for SDK-specific failures.
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

    /**
     * Constructs a base error with message and cause.
     *
     * @param message Error message
     * @param e       Root cause
     */
    public HypeError(String message, Throwable e) {
        super(message, e);
    }

    /**
     * Client-side error category (HTTP 4xx).
     */
    public static class ClientHypeError extends HypeError {
        /**
         * Get HTTP status code.
         */
        private final int statusCode;

        /**
         * Constructs a client error.
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
     * Server-side error category (HTTP 5xx).
     */
    public static class ServerHypeError extends HypeError {
        /**
         * Get HTTP status code.
         */
        private final int statusCode;

        /**
         * Constructs a server error.
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
