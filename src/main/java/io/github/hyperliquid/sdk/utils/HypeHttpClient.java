package io.github.hyperliquid.sdk.utils;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HypeHttpClient {

    private static final Logger log = LoggerFactory.getLogger(HypeHttpClient.class);

    private final String baseUrl;

    private final OkHttpClient client;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");


    public HypeHttpClient(String baseUrl, OkHttpClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    /**
     * 获取基础URL。
     *
     * @return 基础URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 发送 POST 请求到指定路径，并返回 JSON 结果。
     *
     * @param path    相对路径，例如 "/info"、"/exchange"
     * @param payload JSON 可序列化对象（会被 ObjectMapper 序列化）
     * @return 返回的 JSON 节点
     * @throws HypeError.ClientHypeError 当响应为 4xx
     * @throws HypeError.ServerHypeError 当响应为 5xx
     */
    public JsonNode post(String path, Object payload) {
        String url = baseUrl + path;
        String json = "";
        try {
            json = JSONUtil.writeValueAsString(payload);
            log.debug("POST: {} ", url);
            log.debug("Request: {}", json);
            RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                // 统一处理响应体，避免重复读取
                String responseBody = response.body() != null ? response.body().string() : "{}";

                if (!response.isSuccessful()) {
                    int code = response.code();
                    String errorMsg = String.format("HTTP %d: %s", code, responseBody);

                    if (code >= 400 && code < 500) {
                        throw new HypeError.ClientHypeError(code, errorMsg);
                    } else {
                        throw new HypeError.ServerHypeError(code, errorMsg);
                    }
                }

                log.debug("Response: {}", responseBody);
                return JSONUtil.readTree(responseBody);
            }
        } catch (IOException e) {
            log.error("Network error for POST: {} Request: {}", path, json, e);
            throw new HypeError("Network error for POST " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * 带重试的 POST（可选），仅对 5xx 与网络异常进行重试，4xx 不重试。
     * 使用指数退避策略：backoff = min(backoff * multiplier, maxBackoff).
     *
     * @param path    相对路径，例如 "/info"、"/exchange"
     * @param payload JSON 可序列化对象
     * @param policy  重试策略配置
     * @return 返回的 JSON 节点
     */
    public JsonNode postWithRetry(String path, Object payload, RetryPolicy policy) {
        String url = baseUrl + path;
        String json = "";
        int attempt = 0;
        long backoff = policy == null ? RetryPolicy.defaultPolicy().getInitialBackoffMillis() : policy.getInitialBackoffMillis();
        int maxRetries = policy == null ? RetryPolicy.defaultPolicy().getMaxRetries() : policy.getMaxRetries();
        double multiplier = policy == null ? RetryPolicy.defaultPolicy().getBackoffMultiplier() : policy.getBackoffMultiplier();
        long maxBackoff = policy == null ? RetryPolicy.defaultPolicy().getMaxBackoffMillis() : policy.getMaxBackoffMillis();

        while (true) {
            try {
                json = JSONUtil.writeValueAsString(payload);
                log.info("POST(retry:{}): {}", attempt, url);
                log.debug("Request: {}", json);
                RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Accept", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        int code = response.code();
                        String errorMsg = String.format("HTTP %d: %s", code, responseBody);

                        if (code >= 400 && code < 500) {
                            log.warn("Non-retryable 4xx: {}", errorMsg);
                            throw new HypeError.ClientHypeError(code, errorMsg);
                        } else {
                            log.warn("Retryable 5xx: {} (attempt {}/{})", errorMsg, attempt + 1, maxRetries);
                            if (++attempt > maxRetries) {
                                throw new HypeError.ServerHypeError(code, errorMsg);
                            }
                        }
                    } else {
                        log.debug("Response: {}", responseBody);
                        return JSONUtil.readTree(responseBody);
                    }
                }
            } catch (IOException e) {
                log.warn("Network error on attempt {}/{} for POST {}: {}", attempt + 1, maxRetries, path, e.toString());
                if (++attempt > maxRetries) {
                    log.error("Max retries exceeded for POST {}", path);
                    throw new HypeError("Network error for POST " + path + ": " + e.getMessage(), e);
                }
            }

            try {
                log.info("Backoff {} ms before next retry", backoff);
                Thread.sleep(backoff);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new HypeError("Retry interrupted for POST " + path);
            }
            backoff = Math.min((long) (backoff * multiplier), maxBackoff);
        }
    }

    public Result<JsonNode> postWithResult(String path, Object payload) {
        try {
            JsonNode node = post(path, payload);
            return Result.ok(node);
        } catch (HypeError.ClientHypeError e) {
            ApiError apiErr = new ApiError("HTTP_4XX", e.getMessage(), e.getStatusCode());
            return Result.err(apiErr);
        } catch (HypeError.ServerHypeError e) {
            ApiError apiErr = new ApiError("HTTP_5XX", e.getMessage(), e.getStatusCode());
            return Result.err(apiErr);
        } catch (HypeError e) {
            ApiError apiErr = new ApiError("HTTP_IO", e.getMessage());
            return Result.err(apiErr);
        }
    }
}
