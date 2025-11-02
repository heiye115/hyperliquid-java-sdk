package io.github.hyperliquid.sdk.utils;

/**
 * SDK 自定义异常类型定义。
 */
public class Error extends RuntimeException {
    /**
     * 构造基础错误。
     *
     * @param message 错误信息
     */
    public Error(String message) {
        super(message);
    }

    /** 客户端错误（4xx） */
    public static class ClientError extends Error {
        private final int statusCode;

        /**
         * 构造客户端错误。
         *
         * @param statusCode HTTP 状态码
         * @param message    错误信息
         */
        public ClientError(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * 获取 HTTP 状态码。
         *
         * @return 状态码
         */
        public int getStatusCode() {
            return statusCode;
        }
    }

    /** 服务器错误（5xx） */
    public static class ServerError extends Error {
        private final int statusCode;

        /**
         * 构造服务器错误。
         *
         * @param statusCode HTTP 状态码
         * @param message    错误信息
         */
        public ServerError(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * 获取 HTTP 状态码。
         *
         * @return 状态码
         */
        public int getStatusCode() {
            return statusCode;
        }
    }
}
