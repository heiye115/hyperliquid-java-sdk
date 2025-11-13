package io.github.hyperliquid.sdk.utils;

import java.util.Objects;
import java.util.function.Function;

/**
 * 泛型结果封装，统一成功/失败返回。
 * @param <T> 成功时承载的数据类型
 */
public final class Result<T> {

    private final T ok;
    private final ApiError err;

    private Result(T ok, ApiError err) {
        this.ok = ok;
        this.err = err;
    }

    /** 创建成功结果 */
    public static <T> Result<T> ok(T value) {
        return new Result<>(Objects.requireNonNull(value), null);
    }

    /** 创建失败结果 */
    public static <T> Result<T> err(ApiError error) {
        return new Result<>(null, Objects.requireNonNull(error));
    }

    /** 是否成功 */
    public boolean isOk() {
        return err == null;
    }

    /** 是否失败 */
    public boolean isErr() {
        return err != null;
    }

    /** 获取成功数据（仅在 isOk 时有效） */
    public T getOk() {
        return ok;
    }

    /** 获取错误信息（仅在 isErr 时有效） */
    public ApiError getErr() {
        return err;
    }

    /** 成功映射 */
    public <U> Result<U> map(Function<T, U> mapper) {
        if (isOk()) {
            return Result.ok(mapper.apply(ok));
        }
        return Result.err(err);
    }

    /** 失败映射 */
    public Result<T> mapErr(Function<ApiError, ApiError> mapper) {
        if (isErr()) {
            return Result.err(mapper.apply(err));
        }
        return this;
    }
}

