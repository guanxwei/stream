package org.stream.extension.utils;

/**
 * A generator to help generate unique task id.
 * @author 魏冠雄
 *
 */
public interface TaskIDGenerator {

    /**
     * Generate task id.
     * @return Unique task id.
     */
    String generateTaskID();
}
