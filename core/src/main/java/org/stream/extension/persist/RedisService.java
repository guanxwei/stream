package org.stream.extension.persist;

import org.stream.extension.clients.RedisClientImpl;

import lombok.Setter;

/**
 * Encapsulation of Redis Service.
 * Provide some valuable APIs to manufacture data stored in Redis cluster.
 * @author guanxiong wei
 *
 */
@Setter
public class RedisService extends RedisClientImpl {

    private int maxActive = 50;

    private int maxIdle = 30;

    private int minIdle = 5;

    /**
     * Initiate method to initiate Redis clients pool.
     * @param nodes Cluster node list.
     * @param timeout Timeout.
     * @param maxRetryTimes Max retry times.
     */
    public RedisService(final String nodes, final int timeout, final int maxRetryTimes) {
        super.setNodes(nodes);
        super.setTimeout(timeout);
        super.setMaxActive(maxActive);
        super.setMaxIdle(maxIdle);
        super.setMinIdle(minIdle);
        super.setMaxRetryTimes(maxRetryTimes);
        super.init();
    }

}
