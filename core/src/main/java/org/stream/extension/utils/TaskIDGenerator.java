package org.stream.extension.utils;

import org.stream.core.resource.Resource;

/**
 * A generator to help generate unique task id.
 * @author weiguanxiong
 *
 */
public interface TaskIDGenerator {

    /**
     * Generate task id.
     * @param primary Primary resource.
     * @return Unique task id.
     */
    String generateTaskID(final Resource primary);
}
