package org.stream.core.resource.test.infrastructure;

import java.util.UUID;

import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceAuthority;
import org.stream.core.resource.ResourceReader;
import org.stream.core.resource.ResourceType;
import org.stream.core.resource.ResourceURL;

public class TestObjectReader implements ResourceReader {

    private final static ResourceAuthority resourceAuthority = new ResourceAuthority("testObject", TestObject.class);

    @Override
    public Resource read(ResourceURL resourceURL) {
        return Resource.builder()
                .resourceReference(UUID.randomUUID().toString())
                .resourceType(ResourceType.OBJECT)
                .resourceURL(resourceURL)
                .value(new TestObject(UUID.randomUUID().toString()))
                .build();
    }

    @Override
    public ResourceAuthority resolve() {
        return resourceAuthority;
    }

}
