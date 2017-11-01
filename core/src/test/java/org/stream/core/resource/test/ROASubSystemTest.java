package org.stream.core.resource.test;

import java.util.UUID;

import org.stream.core.resource.Cache;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceAuthority;
import org.stream.core.resource.ResourceCatalog;
import org.stream.core.resource.ResourceType;
import org.stream.core.resource.ResourceURL;
import org.stream.core.resource.sample.MemoryCache;
import org.stream.core.resource.test.infrastructure.TestObject;
import org.stream.core.resource.test.infrastructure.TestObjectReader;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ROASubSystemTest {

    private static final String PATH = "ROS::test::1.0::" + UUID.randomUUID().toString();
    private static final String RESOURCE_AUTHORITY_VALUE = "testObject";

    private Cache cache;
    private Resource resource;
    private ResourceURL resourceURL;
    private ResourceAuthority resourceAuthority;
    private ResourceCatalog resourceCatalog;

    @org.testng.annotations.BeforeMethod
    public void BeforeMethod() {
        this.resourceCatalog = new ResourceCatalog();
        this.resourceCatalog.registerReader(new TestObjectReader());
        this.resourceAuthority = new ResourceAuthority(RESOURCE_AUTHORITY_VALUE, TestObject.class);
        this.resourceURL = new ResourceURL(PATH, resourceAuthority);
    }

    @Test
    public void testCache() {
        this.cache = new MemoryCache();
        this.resource = Resource.builder()
                .resourceType(ResourceType.OBJECT)
                .resourceReference("test")
                .resourceURL(resourceURL)
                .build();
        cache.put(resourceURL, resource);
        Resource r = cache.get(resourceURL);
        Assert.assertNotNull(r);
    }

    @Test
    public void testResourceReader() throws Exception {
        Resource retrievedResource = resourceCatalog.readResource(resourceURL);
        Assert.assertNotNull(retrievedResource);
        Assert.assertEquals(retrievedResource.getValue().getClass().getName(), TestObject.class.getName());
    }

}
