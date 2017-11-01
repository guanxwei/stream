package org.stream.core.resource;

/**
 * Encapsulation of resource reader.
 * Stream provides an Resource-oriented-architecture abstraction to help read resource from the resource storage. Customers can
 * implements the interface to realize their own requirement. The framework does not care about the detail how customers
 * implements the readers.
 */
public interface ResourceReader {

    /**
     * Read and load the resource from the underline resource storage via its url.
     * @param resourceURL Resource url.
     * @return Resource entity.
     */
    Resource read(final ResourceURL resourceURL);

    /**
     * Resolve the reader's resource authority.
     * @return ResourceAuthority
     */
    ResourceAuthority resolve();

}
