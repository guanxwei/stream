/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.extension.io;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Common data holder to exchange between different services.
 * @author guanxiong wei
 *
 */
@Setter
@Getter
public class StreamTransferData implements Serializable {

    @Serial
    private static final long serialVersionUID = -3946269022445783584L;

    private String activityResult;

    private Map<String, Serializable> objects = new HashMap<>();

    /**
     * Get an object via a key.
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
     * Add an object and return this reference so that developers can invoke this method in a chain pattern.
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
        target.setActivityResult(source.getActivityResult());
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
        return status(StreamTransferDataStatus.SUCCESS, values);
    }

    public static StreamTransferData succeed() {
        return status(StreamTransferDataStatus.SUCCESS, null);
    }

    public static StreamTransferData failed(final Map<String, Serializable> values) {
        return status(StreamTransferDataStatus.FAIL, values);
    }

    public static StreamTransferData failed() {
        return status(StreamTransferDataStatus.FAIL, null);
    }

    public static StreamTransferData suspend(final Map<String, Serializable> values) {
        return status(StreamTransferDataStatus.SUSPEND, values);
    }

    public static StreamTransferData suspend() {
        return status(StreamTransferDataStatus.SUSPEND, null);
    }

    public static StreamTransferData retry(final Map<String, Serializable> values) {
        return status(StreamTransferDataStatus.UNKNOWN, values);
    }

    public static StreamTransferData retry() {
        return status(StreamTransferDataStatus.UNKNOWN, null);
    }
}
