package org.stream.core.resource;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

/**
 * Encapsulation of work-flow resources.
 * Each resource instance will contain one valuable "resource" that can be found by a reference in the
 * work-flow context.
 * @author hzweiguanxiong
 *
 */
@Data
public class Resource {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Object value;

    private ResourceType resourceType;

    /**
     * Work-flow internal used reference to concrete resource.
     */
    private String resourceReference;

    /**
     * Work-flow external used reference to a potential concrete resource. Typically used to get resource
     * through {@link ResourceReader}. {@link ResourceReader} will use it to find the resource from their
     * own storage.
     */
    private ResourceURL resourceURL;

    /**
     * Return resource builder.
     * @return Resource builder.
     */
    public static ResourceBuilder builder() {
        return new ResourceBuilder();
    }

    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse resource from string.
     * @param content Content to be parsed.
     * @return Parse object.
     */
    public static Resource parse(final String content) {
        try {
            return MAPPER.readValue(content, Resource.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read json node value from the input Jsonfied content.
     * @param content Jsonfied resource string.
     * @return Jsonfied value string.
     */
    public static String readValue(final String content) {
        try {
            JsonNode node = MAPPER.readTree(content);
            return node.get("value").textValue();
        } catch (Exception e) {
            return null;
        }

    }

    // CHECKSTYLE:OFF
    /**
     * Resource builder.
     * @author hzweiguanxiong
     *
     */
    public static class ResourceBuilder {
        private Object value;
        private ResourceType resourceType;
        private String resourceReference;
        private ResourceURL resourceURL;

        public ResourceBuilder value(final Object value) {
            this.value = value;
            return this;
        }

        public ResourceBuilder resourceType(final ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public ResourceBuilder resourceReference(final String resourceReference) {
            this.resourceReference = resourceReference;
            return this;
        }

        public ResourceBuilder resourceURL(final ResourceURL resourceURL) {
            this.resourceURL = resourceURL;
            return this;
        }

        public Resource build() {
            Resource resource = new Resource();
            resource.setResourceReference(this.resourceReference);
            resource.setResourceType(this.resourceType);
            resource.setValue(this.value);
            resource.setResourceURL(this.resourceURL);
            return resource;
        }
    }
    // CHECKSTYLE:ON
}
