package io.github.hyperliquid.sdk.utils;

/**
 * SDK 自定义异常类型定义。
 */
public class HypeError extends RuntimeException {

    /**
     * 构造基础错误。
     *
     * @param message 错误信息
     */
    public HypeError(String message) {
        super(message);
    }

    public HypeError(String message, Throwable e) {
        super(message, e);
    }

    /**
     * 客户端错误（4xx）
     */
    public static class ClientHypeError extends HypeError {
        /**
         *  获取 HTTP 状态码。
         */
        private final int statusCode;

        /**
         * 构造客户端错误。
         *
         * @param statusCode HTTP 状态码
         * @param message    错误信息
         */
        public ClientHypeError(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * 获取 HTTP 状态码。
         *
         * @return HTTP 状态码
         */
        public int getStatusCode() {
            return statusCode;
        }
    }

    /**
     * 服务器错误（5xx）
     */
    public static class ServerHypeError extends HypeError {
        /**
         *  获取 HTTP 状态码。
         */
        private final int statusCode;

        /**
         * 构造服务器错误。
         *
         * @param statusCode HTTP 状态码
         * @param message    错误信息
         */
        public ServerHypeError(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * 获取 HTTP 状态码。
         *
         * @return HTTP 状态码
         */
        public int getStatusCode() {
            return statusCode;
        }
    }
}