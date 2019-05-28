package org.stream.extension.utils;

import java.util.UUID;

import org.stream.core.resource.Resource;

/**
 * A {@link TaskIDGenerator} implementation to generate task id
 * using UUID facility. Ignore the input primary resource.
 * @author weiguanxiong
 *
 */
public class UUIDTaskIDGenerator implements TaskIDGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateTaskID(final Resource primary) {
         return UUID.randomUUID().toString();
    }

}
