package org.stream.core.resource;

public abstract class AbstractResourceReader implements ResourceReader {

    /**
     * {@inheritDoc}
     */
    public Resource read(final ResourceURL resourceURL) {
        Resource taskResource = Resource.builder()
                .resourceReference(resourceURL.getPath())
                .resourceURL(resourceURL)
                .value(doRead(resourceURL))
                .build();
        return taskResource;
    }

    /**
     * Read a object from the resource specific storage.
     * @param resourceURL A reference to the object.
     * @return An object read from the storage.
     */
    protected abstract Object doRead(final ResourceURL resourceURL);
}
