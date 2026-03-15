package io.github.hyperliquid.sdk.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified error model, carrying error code, message, and context.
 */
public final class ApiError {

    private final String code;
    private final String message;
    private final Integer statusCode;

    private final Map<String, Object> context;

    /**
     * Creates an ApiError with code and message.
     *
     * @param code    Error code
     * @param message Error message
     */
    public ApiError(String code, String message) {
        this(code, message, null, Collections.emptyMap());
    }

    /**
     * Creates an ApiError with code, message, and HTTP status code.
     *
     * @param code       Error code
     * @param message    Error message
     * @param statusCode HTTP status code
     */
    public ApiError(String code, String message, Integer statusCode) {
        this(code, message, statusCode, Collections.emptyMap());
    }

    /**
     * Creates an ApiError with full fields.
     *
     * @param code       Error code
     * @param message    Error message
     * @param statusCode HTTP status code
     * @param context    Additional context map
     */
    public ApiError(String code, String message, Integer statusCode, Map<String, Object> context) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
        this.context = context == null ? Collections.emptyMap() : new LinkedHashMap<>(context);
    }

    /**
     * Returns the error code.
     *
     * @return Error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the error message.
     *
     * @return Error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the HTTP status code when available.
     *
     * @return HTTP status code or null
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Returns an immutable view of the context map.
     *
     * @return Context map
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }
}
