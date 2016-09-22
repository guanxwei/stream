package org.stream.core.resource;

/**
 * Encapsulation of resource reader.
 * Stream provides an Resource-oriented-architecture abstraction to help read resource from the resource storage. Customers can
 * implements the interface to realize their own sprtial requirement. The framework does not care about the detail how customers
 * implements the readers.
 */
public interface ResourceReader {

    Resource read(ResourceURL resourceURL);

    ResourceAuthority resolve();

}
