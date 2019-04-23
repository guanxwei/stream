package org.stream.core.test.base;

import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.Tower;

public class TestTower implements Tower<String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamTransferData call(final String request) {
        return StreamTransferData.succeed();
    }

}
