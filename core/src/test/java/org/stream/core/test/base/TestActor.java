package org.stream.core.test.base;

import org.stream.extension.io.Actor;
import org.stream.extension.io.StreamTransferData;

public class TestActor implements Actor<String> {

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamTransferData call(final String request) {
        return StreamTransferData.succeed();
    }

}
