package org.stream.core.test.base;

import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.Tower;

public class TestTower implements Tower {

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamTransferData call(final StreamTransferData request) {
        return StreamTransferData.succeed();
    }

}
