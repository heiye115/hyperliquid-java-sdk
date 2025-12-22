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
     * Build and configure shared ObjectMapper.
     */
    private static ObjectMapper createSharedMapper() {
        // Create a shared ObjectMapper instance.
        // ObjectMapper is thread-safe after configuration and should be reused globally.
        ObjectMapper om = new ObjectMapper();

        // Ignore unknown JSON fields during deserialization.
        // This prevents failures when external APIs (e.g. exchanges) add new fields.
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // When an unknown enum value is encountered, deserialize it as null
        // instead of throwing an exception.
        // This is critical for forward compatibility with evolving API enums.
        om.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

        // Deserialize floating-point numbers as BigDecimal instead of Double.
        // This avoids precision loss for prices, sizes, and financial calculations.
        om.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

        // Serialize date/time values using ISO-8601 format
        // instead of numeric timestamps, improving readability and logging.
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return om;
    }


    /**
     * Register custom modules (e.g., JavaTimeModule, Jdk8Module, or custom serializers).
     * This method is thread-safe and should be registered uniformly at application startup.
     *
     * @param module Jackson module
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
