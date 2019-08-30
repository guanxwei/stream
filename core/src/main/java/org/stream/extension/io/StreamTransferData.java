package org.stream.extension.io;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Common data holder to exchange between different services.
 * @author hzweiguanxiong
 *
 */
public class StreamTransferData implements Serializable {

    private static final long serialVersionUID = -3946269022445783584L;

    @Getter @Setter
    private String activityResult;

    @Getter @Setter
    private Map<String, Serializable> objects = new HashMap<>();

    /**
     * Get object via key.
     * @param key Key.
     * @return Object if exists.
     */
    public Serializable get(final String key) {
        return objects.get(key);
    }

    /**
     * Store an object.
     * @param key Object's key.
     * @param object Object to be saved.
     */
    public void set(final String key, final Serializable object) {
        objects.put(key, object);
    }

    /**
     * Add an object and return this reference so that developers can invoke this method in chain pattern.
     * @param key Object's key.
     * @param object Object to be saved.
     * @return this reference.
     */
    public StreamTransferData add(final String key, final Serializable object) {
        set(key, object);
        return this;
    }

    /**
     * Get the value as cast it the target class.
     * @param <T> Target class type.
     * @param key Key reference to the value.
     * @param clazz Valuable class.
     * @return The value mapping the key.
     */
    public <T> T as(final String key, final Class<T> clazz) {
        assert Serializable.class.isAssignableFrom(clazz);

        return clazz.cast(get(key));
    }

    public <T> T getPrimary() {
        String className = as("primaryClass", String.class);
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) Class.forName(className);
            return as("primary", clazz);
        } catch (ClassNotFoundException e) {
            return null;
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

    private static StreamTransferData status(final String status, final Map<String, Serializable> values) {
        StreamTransferData streamTransferData = new StreamTransferData();
        streamTransferData.setActivityResult(status);
        if (values != null) {
            streamTransferData.getObjects().putAll(values);
        }
        return streamTransferData;
    }

    public static StreamTransferData succeed(final Map<String, Serializable> values) {
        return status(Result.SUCCESS, values);
    }

    public static StreamTransferData succeed() {
        return status(Result.SUCCESS, null);
    }

    public static StreamTransferData failed(final Map<String, Serializable> values) {
        return status(Result.FAIL, values);
    }

    public static StreamTransferData failed() {
        return status(Result.FAIL, null);
    }

    public static StreamTransferData suspend(final Map<String, Serializable> values) {
        return status(Result.SUSPEND, values);
    }

    public static StreamTransferData suspend() {
        return status(Result.SUSPEND, null);
    }

    public static StreamTransferData retry(final Map<String, Serializable> values) {
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
