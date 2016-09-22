package org.stream.core.resource;

import lombok.Data;

@Data
public class Resource {

    private Object value;

    private ResourceType resourceType;

    private String resourceReference;

    private ResourceURL resourceURL;

    public static ResourceBuilder builder() {
        return new ResourceBuilder();
    }

    public static class ResourceBuilder {
        private Object value;
        private ResourceType resourceType;
        private String resourceReference;
        private ResourceURL resourceURL;

        public ResourceBuilder value(Object value) {
            this.value = value;
            return this;
        }

        public ResourceBuilder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public ResourceBuilder resourceReference(String resourceReference) {
            this.resourceReference = resourceReference;
            return this;
        }

        public ResourceBuilder resourceURL(ResourceURL resourceURL) {
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
}
