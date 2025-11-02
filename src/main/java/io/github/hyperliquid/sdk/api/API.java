package io.github.hyperliquid.sdk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.hyperliquid.sdk.utils.Error;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * API 客户端基类，封装 HTTP 请求与错误处理。
 */
public class API {
    protected final String baseUrl;
    protected final OkHttpClient client;
    protected final ObjectMapper mapper;

    /**
     * 全局共享的 ObjectMapper 单例，统一配置序列化/反序列化策略。
     * 所有 API 子类默认复用该实例，避免多处配置不一致与性能浪费。
     */
    private static final ObjectMapper SHARED_MAPPER = createSharedMapper();

    /**
     * 构建并配置共享 ObjectMapper。
     */
    private static ObjectMapper createSharedMapper() {
        ObjectMapper om = new ObjectMapper();
        // 反序列化容错：忽略未知字段，避免后端新增字段导致解析失败
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 序列化：日期使用 ISO-8601（不写为时间戳），便于日志可读
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return om;
    }

    /**
     * 获取共享的 ObjectMapper。
     */
    public static ObjectMapper getSharedMapper() {
        return SHARED_MAPPER;
    }

    /**
     * 注册自定义模块（例如 JavaTimeModule、Jdk8Module 或自定义序列化器）。
     * 该方法线程安全，应在应用启动时统一注册。
     *
     * @param module Jackson 模块
     */
    public static synchronized void registerModule(Module module) {
        if (module != null) {
            SHARED_MAPPER.registerModule(module);
        }
    }

    /**
     * 构造 API 客户端。
     *
     * @param baseUrl API 根地址（例如 https://api.hyperliquid.xyz）
     * @param timeout 请求超时时间（秒）
     */
    public API(String baseUrl, int timeout) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        // 统一复用共享 ObjectMapper
        this.mapper = SHARED_MAPPER;
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(timeout))
                .connectTimeout(Duration.ofSeconds(timeout))
                .readTimeout(Duration.ofSeconds(timeout))
                .writeTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    /**
     * 发送 POST 请求到指定路径，并返回 JSON 结果。
     *
     * @param path    相对路径，例如 "/info"、"/exchange"
     * @param payload JSON 可序列化对象（会被 ObjectMapper 序列化）
     * @return 返回的 JSON 节点
     * @throws Error.ClientError 当响应为 4xx
     * @throws Error.ServerError 当响应为 5xx
     */
    public JsonNode post(String path, Object payload) {
        try {
            String url = baseUrl + path;
            String json = mapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    int code = response.code();
                    String err = response.body() != null ? response.body().string() : "";
                    if (code >= 400 && code < 500) {
                        throw new Error.ClientError(code, err);
                    } else {
                        throw new Error.ServerError(code, err);
                    }
                }
                String resp = response.body() != null ? response.body().string() : "{}";
                return mapper.readTree(resp);
            }
        } catch (IOException e) {
            throw new Error("Network or I/O error: " + e.getMessage());
        }
    }
}
