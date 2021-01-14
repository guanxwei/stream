package org.stream.core.helper;

import java.io.IOException;
import java.util.StringTokenizer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class providing some useful methods to process Json text.
 * Uses the jackson framework to parse and jsonfy the inputs.
 *
 */

public class Jackson {

    private static final ObjectMapper OBJECT_MANAGER = new ObjectMapper();

    static {
        OBJECT_MANAGER.setSerializationInclusion(Include.NON_NULL);
        OBJECT_MANAGER.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        OBJECT_MANAGER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MANAGER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        OBJECT_MANAGER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    /**
     * Generate jsonfied string for the input object.
     * @param obj Incoming object.
     * @return Jsonfied string.
     */
    public static String json(final Object obj) {
        try {
            return OBJECT_MANAGER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("toJson出错：" + e.getMessage(), e);
        }
    }

    /**
     * Parse an object from the incoming jsonfied string with the corresponding type reference.
     * @param json Jsonfied string.
     * @param typeref Type reference.
     * @param <T> Value.
     * @return Parsed object with the specific type reference.
     */
    public static <T> T parse(final String json, final TypeReference<T> typeref) {
        try {
            return OBJECT_MANAGER.readValue(json, typeref);
        } catch (Exception e) {
            throw new RuntimeException("readObject TypeReference出错：" + e.getMessage(), e);
        }
    }

    /**
     * Parse an object from the incoming jsonfied string with the corresponding class. Null non-tolerant.
     * @param json Jsonfied string.
     * @param clz Object type.
     * @param <T> Value.
     * @return Parsed object with the specific class.
     */
    public static <T> T parse(final String json, final Class<T> clz) {
        try {
            return OBJECT_MANAGER.readValue(json, clz);
        } catch (Exception e) {
            throw new RuntimeException("readObject出错：" + e.getMessage(), e);
        }
    }

    /**
     * Parse an object from the incoming jsonfied string with the corresponding class. Null tolerant.
     * @param json Jsonfied string.
     * @param clz Object type.
     * @param <T> Value
     * @return Parsed object with the specific class.
     */
    public static <T> T parseWithNull(final String json, final Class<T> clz) {
        try {
            if (json == null || json.length() == 0) {
                return null;
            }
            return OBJECT_MANAGER.readValue(json, clz);
        } catch (Exception e) {
            throw new RuntimeException("readObject出错：" + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseNodeValue(final JsonNode node, final Class<T> clazz) throws IOException {
        if (node == null) {
            return null;
        }
        if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(node.asLong());
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(node.asInt());
        }
        if (clazz == Float.class || clazz == float.class) {
            return (T) Float.valueOf(node.asText());
        }
        if (clazz == Double.class || clazz == double.class) {
            return (T) Double.valueOf(node.asDouble());
        }
        if (clazz.isAssignableFrom(String.class)) {
            return (T) node.asText();
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) Boolean.valueOf(node.asBoolean());
        }
        return OBJECT_MANAGER.readValue(node.asText(), clazz);
    }

    private static JsonNode findNode(final JsonNode root, final String path) {
        StringTokenizer tokenizer = new StringTokenizer(path, ".");
        JsonNode current = root;
        while (tokenizer.hasMoreElements()) {
            String key = tokenizer.nextToken();
            current = current.get(key);
            if (current == null) {
               break;
            }
        }
        return current;
    }

    /**
     * Get the property value with the specific path from the incoming json string.
     * @param json Jsonfied string of the instance of Class clazz.
     * @param path Property path.
     * @param clazz Corresponding object's concrete class.
     * @param <T> Value.
     * @return Property value if exists.
     */
    public static <T> T getProperty(final String json, final String path, final Class<T> clazz) {
        try {
            JsonNode root = OBJECT_MANAGER.readTree(json);
            return parseNodeValue(findNode(root, path), clazz);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage(), ioe);
        }
    }

    private JsonNode jsonNode;

    /**
     * Constructor.
     * @param json Jsonfied string.
     * @throws IOException IOException.
     */
    public Jackson(final String json) throws IOException {
        this.jsonNode = OBJECT_MANAGER.readTree(json);
    }

    /**
     * Get the property value with the specific path from the incoming json string.
     * Instance of this class should be initiated before invoking this method.
     * @param path Property path.
     * @param clazz Corresponding object's concrete class.
     * @param <T> Value.
     * @return Property value if exists.
     */
    public <T> T getProperty(final String path, final Class<T> clazz) {
        try {
            return parseNodeValue(findNode(this.jsonNode, path), clazz);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage(), ioe);
        }
    }

}
