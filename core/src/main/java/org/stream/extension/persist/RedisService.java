package org.stream.extension.persist;

import org.stream.extension.clients.RedisClientImpl;

/**
 * Encapsulation of Redis Service.
 * Provide some valuable APIs to manufacture data stored in Redis cluster.
 * @author hzweiguanxiong
 *
 */
public class RedisService extends RedisClientImpl {

    /**
     * Initiate method to initiate Redis clients pool.
     * @param nodes Cluster node list.
     * @param timeout Timeout.
     * @param maxRetryTimes Max retry times.
     */
    public RedisService(final String nodes, final int timeout, final int maxRetryTimes) {
        super(null, nodes, timeout, maxRetryTimes);
        super.init();
    }

}
