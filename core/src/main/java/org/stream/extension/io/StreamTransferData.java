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
    private int status;

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
    public static Object parse(final String content) {
        try {
            return OBJECT_MAPPER.readValue(content, StreamTransferData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
