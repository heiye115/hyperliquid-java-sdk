package io.github.hyperliquid.sdk.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一错误模型，携带错误码、消息与上下文。
 */
public final class ApiError {

    private final String code;
    private final String message;
    private final Integer statusCode;
    private final Map<String, Object> context;

    public ApiError(String code, String message) {
        this(code, message, null, Collections.emptyMap());
    }

    public ApiError(String code, String message, Integer statusCode) {
        this(code, message, statusCode, Collections.emptyMap());
    }

    public ApiError(String code, String message, Integer statusCode, Map<String, Object> context) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
        this.context = context == null ? Collections.emptyMap() : new LinkedHashMap<>(context);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }
}

