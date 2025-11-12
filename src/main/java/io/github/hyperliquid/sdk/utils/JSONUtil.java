package io.github.hyperliquid.sdk.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.List;

public class JSONUtil {
    private static final ObjectMapper mapper = createSharedMapper();

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
     * 注册自定义模块（例如 JavaTimeModule、Jdk8Module 或自定义序列化器）。
     * 该方法线程安全，应在应用启动时统一注册。
     *
     * @param module Jackson 模块
     */
    public static synchronized void registerModule(com.fasterxml.jackson.databind.Module module) {
        if (module != null) {
            mapper.registerModule(module);
        }
    }

    public static JsonNode readTree(String resp) throws JsonProcessingException {
        return mapper.readTree(resp);
    }

    public static String writeValueAsString(Object payload) throws JsonProcessingException {
        return mapper.writeValueAsString(payload);
    }


    public static <T> T treeToValue(TreeNode var1, Class<T> var2) throws JsonProcessingException {
        return mapper.treeToValue(var1, var2);
    }

    public static <T> T treeToValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }

    public static <T> T convertValue(JsonNode node, MapType mapType) {
        return mapper.convertValue(node, mapType);
    }

    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) throws IllegalArgumentException {
        return mapper.convertValue(fromValue, toValueTypeRef);
    }

    public static <T> T convertValue(Object fromValue, JavaType toValueType) throws IllegalArgumentException {
        return mapper.convertValue(fromValue, toValueType);
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) throws IllegalArgumentException {
        return mapper.convertValue(fromValue, toValueType);
    }

    public static byte[] writeValueAsBytes(Object action) throws JsonProcessingException {
        return mapper.writeValueAsBytes(action);
    }

    public static <T> T readValue(JsonParser var1, Class<T> var2) throws IOException {
        return mapper.readValue(var1, var2);
    }

    public static <T> T readValue(String content, Class<T> valueType) throws JsonProcessingException {
        return mapper.readValue(content, valueType);
    }

    public static <T> List<T> toList(JsonNode jsonNode, Class<T> valueType) {
        return mapper.convertValue(jsonNode, TypeFactory.defaultInstance().constructCollectionType(List.class, valueType));
    }

}
