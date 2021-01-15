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
}
