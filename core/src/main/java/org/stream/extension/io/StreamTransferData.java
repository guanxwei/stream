package org.stream.extension.io;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

/**
 * Common data holder to exchange between different services.
 * @author hzweiguanxiong
 *
 */
public class StreamTransferData implements Serializable {

    private static final long serialVersionUID = -3946269022445783584L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Getter @Setter
    private String activityResult;

    @Getter @Setter
    private Map<String, Object> objects = new HashMap<>();

    /**
     * Get object via key.
     * @param key Key.
     * @return Object if exists.
     */
    public Object get(final String key) {
        return objects.get(key);
    }

    /**
     * Store an object.
     * @param key Object's key.
     * @param object Object to be saved.
     */
    public void set(final String key, final Object object) {
        objects.put(key, object);
    }

    /**
     * Add an object and return this reference so that developers can invoke this method in chain pattern.
     * @param key Object's key.
     * @param object Object to be saved.
     * @return this reference.
     */
    public StreamTransferData add(final String key, final Object object) {
        set(key, object);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse {@link StreamTransferData} from the input Jsonfied string.
     * @param content Jsonfied string.
     * @return Parsed {@link StreamTransferData} entity.
     */
    public static StreamTransferData parse(final String content) {
        try {
            return OBJECT_MAPPER.readValue(content, StreamTransferData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Merge the source into the target object.
     * @param target Target object to accept the source's values.
     * @param source Source object provides values to the target object.
     */
    public static void merge(final StreamTransferData target, final StreamTransferData source) {
        target.objects.putAll(source.getObjects());
    }

    private static StreamTransferData status(final String status, final Map<String, Object> values) {
        StreamTransferData streamTransferData = new StreamTransferData();
        streamTransferData.setActivityResult(status);
        if (values != null) {
            streamTransferData.getObjects().putAll(values);
        }
        return streamTransferData;
    }

    public static StreamTransferData succeed(final Map<String, Object> values) {
        return status(Result.SUCCESS, values);
    }

    public static StreamTransferData succeed() {
        return status(Result.SUCCESS, null);
    }

    public static StreamTransferData failed(final Map<String, Object> values) {
        return status(Result.FAIL, values);
    }

    public static StreamTransferData failed() {
        return status(Result.FAIL, null);
    }

    public static StreamTransferData suspend(final Map<String, Object> values) {
        return status(Result.SUSPEND, values);
    }

    public static StreamTransferData suspend() {
        return status(Result.SUSPEND, null);
    }

    public static StreamTransferData retry(final Map<String, Object> values) {
        return status(Result.UNKNOWN, values);
    }

    public static StreamTransferData retry() {
        return status(Result.UNKNOWN, null);
    }

    public interface Result {
        String SUCCESS = "SUCCESS";
        String FAIL = "FAIL";
        String UNKNOWN = "UNKNOWN";
        String SUSPEND = "SUSPEND";
    }
}
