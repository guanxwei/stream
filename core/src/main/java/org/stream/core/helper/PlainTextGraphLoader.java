package org.stream.core.helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.stream.core.exception.GraphLoadException;

import lombok.extern.slf4j.Slf4j;

/**
 * Plain text based graph loader. The graph definition content will be plain text input directly from
 * the user interface.
 * @author weiguanxiong.
 *
 */
@Slf4j
public class PlainTextGraphLoader extends AbstractGraphLoader {

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream loadInputStream(final String sourcePath) throws GraphLoadException {
        log.info("Graph context [{}]", sourcePath);
        return new ByteArrayInputStream(sourcePath.getBytes());
    }

}
