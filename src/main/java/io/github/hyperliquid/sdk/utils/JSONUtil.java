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

/**
 * Shared JSON utility methods backed by a single ObjectMapper instance.
 */
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

    /**
     * Parses a JSON string into a JsonNode tree.
     *
     * @param resp JSON string
     * @return Parsed JsonNode
     * @throws JsonProcessingException If parsing fails
     */
    public static JsonNode readTree(String resp) throws JsonProcessingException {
        return mapper.readTree(resp);
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param payload Object to serialize
     * @return JSON string
     * @throws JsonProcessingException If serialization fails
     */
    public static String writeValueAsString(Object payload) throws JsonProcessingException {
        return mapper.writeValueAsString(payload);
    }


    /**
     * Converts a TreeNode into a typed object.
     *
     * @param var1 Source tree node
     * @param var2 Target type
     * @param <T>  Target generic type
     * @return Converted object
     * @throws JsonProcessingException If conversion fails
     */
    public static <T> T treeToValue(TreeNode var1, Class<T> var2) throws JsonProcessingException {
        return mapper.treeToValue(var1, var2);
    }

    /**
     * Converts an object into a typed object.
     *
     * @param fromValue   Source value
     * @param toValueType Target type
     * @param <T>         Target generic type
     * @return Converted object
     */
    public static <T> T treeToValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }

    /**
     * Converts a JsonNode using a Jackson MapType definition.
     *
     * @param node    Source node
     * @param mapType Target map type
     * @param <T>     Target generic type
     * @return Converted object
     */
    public static <T> T convertValue(JsonNode node, MapType mapType) {
        return mapper.convertValue(node, mapType);
    }

    /**
     * Converts an object using a TypeReference.
     *
     * @param fromValue      Source value
     * @param toValueTypeRef Target type reference
     * @param <T>            Target generic type
     * @return Converted object
     * @throws IllegalArgumentException If conversion fails
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) throws IllegalArgumentException {
        return mapper.convertValue(fromValue, toValueTypeRef);
    }

    /**
     * Converts an object using a JavaType.
     *
     * @param fromValue   Source value
     * @param toValueType Target JavaType
     * @param <T>         Target generic type
     * @return Converted object
     * @throws IllegalArgumentException If conversion fails
     */
    public static <T> T convertValue(Object fromValue, JavaType toValueType) throws IllegalArgumentException {
        return mapper.convertValue(fromValue, toValueType);
    }

    /**
     * Converts an object using a target class.
     *
     * @param fromValue   Source value
     * @param toValueType Target class
     * @param <T>         Target generic type
     * @return Converted object
     * @throws IllegalArgumentException If conversion fails
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) throws IllegalArgumentException {
        return mapper.convertValue(fromValue, toValueType);
    }


    /**
     * Serializes an object to JSON bytes.
     *
     * @param action Object to serialize
     * @return JSON bytes
     * @throws JsonProcessingException If serialization fails
     */
    public static byte[] writeValueAsBytes(Object action) throws JsonProcessingException {
        return mapper.writeValueAsBytes(action);
    }

    /**
     * Deserializes JSON content from a parser.
     *
     * @param var1 Parser source
     * @param var2 Target type
     * @param <T>  Target generic type
     * @return Deserialized object
     * @throws IOException If deserialization fails
     */
    public static <T> T readValue(JsonParser var1, Class<T> var2) throws IOException {
        return mapper.readValue(var1, var2);
    }

    /**
     * Deserializes a JSON string into a typed object.
     *
     * @param content   JSON string
     * @param valueType Target type
     * @param <T>       Target generic type
     * @return Deserialized object
     * @throws JsonProcessingException If deserialization fails
     */
    public static <T> T readValue(String content, Class<T> valueType) throws JsonProcessingException {
        return mapper.readValue(content, valueType);
    }

    /**
     * Converts a JsonNode array into a typed list.
     *
     * @param jsonNode  Source JsonNode
     * @param valueType Element type
     * @param <T>       Element generic type
     * @return Converted list
     */
    public static <T> List<T> toList(JsonNode jsonNode, Class<T> valueType) {
        return mapper.convertValue(jsonNode, TypeFactory.defaultInstance().constructCollectionType(List.class, valueType));
    }

}
