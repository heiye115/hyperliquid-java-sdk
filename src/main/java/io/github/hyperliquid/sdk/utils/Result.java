package io.github.hyperliquid.sdk.utils;

import java.util.Objects;
import java.util.function.Function;

/**
 * Generic result wrapper, unifying success/failure returns.
 * @param <T> Data type carried on success
 */
public final class Result<T> {

    private final T ok;
    private final ApiError err;

    private Result(T ok, ApiError err) {
        this.ok = ok;
        this.err = err;
    }

    /** Create success result */
    public static <T> Result<T> ok(T value) {
        return new Result<>(Objects.requireNonNull(value), null);
    }

    /** Create failure result */
    public static <T> Result<T> err(ApiError error) {
        return new Result<>(null, Objects.requireNonNull(error));
    }

    /** Whether it's successful */
    public boolean isOk() {
        return err == null;
    }

    /** Whether it's a failure */
    public boolean isErr() {
        return err != null;
    }

    /** Get success data (only valid when isOk) */
    public T getOk() {
        return ok;
    }

    /** Get error information (only valid when isErr) */
    public ApiError getErr() {
        return err;
    }

    /** Success mapping */
    public <U> Result<U> map(Function<T, U> mapper) {
        if (isOk()) {
            return Result.ok(mapper.apply(ok));
        }
        return Result.err(err);
    }

    /** Failure mapping */
    public Result<T> mapErr(Function<ApiError, ApiError> mapper) {
        if (isErr()) {
            return Result.err(mapper.apply(err));
        }
        return this;
    }
}

