package org.stream.extension.utils;

import java.util.UUID;

/**
 * A {@link TaskIDGenerator} implementation to generate task id
 * using UUID facility.
 * @author 魏冠雄
 *
 */
public class UUIDTaskIDGenerator implements TaskIDGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateTaskID() {
         return UUID.randomUUID().toString();
    }

}
