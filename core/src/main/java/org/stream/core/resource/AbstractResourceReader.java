package org.stream.core.resource;

/**
 * Abstract implement of {@link ResourceReader}
 */
public abstract class AbstractResourceReader<T> implements ResourceReader {

    /**
     * {@inheritDoc}
     */
    public Resource read(final ResourceURL resourceURL) {
        return Resource.builder()
                .resourceReference(constructResourceReference(resourceURL))
                .resourceURL(resourceURL)
                .value(doRead(resourceURL))
                .build();
    }

    /**
     * Read a object from the resource specific storage.
     * @param resourceURL A reference to the object.
     * @return An object read from the storage.
     */
    protected abstract T doRead(final ResourceURL resourceURL);

    /**
     * Typically a resource url is used to link a resource stored in external systems, like a KV database,
     * while a resource reference is used by the workflow engine to link a memory object.In most times
     * the resource url and resource reference will have their own naming convension, so define a method here
     * letting different resource admins implement their name convertion, which is convert a resource url to
     * a resource reference. Once a resource is loaded through the resource url,  a unique resource reference will
     * also be binded to the resource in the memory, then all avtivity can also visit the resource through
     * the resource reference if they want.
     * @param resourceURL A url linked to the a external resource.
     * @return Resource reference used in the memory.
     */
    protected abstract String constructResourceReference(final ResourceURL resourceURL);
}
