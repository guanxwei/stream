package org.stream.extension.persist;

import java.util.Collection;

import lombok.Setter;

/**
 * A redis based implementation of distributed delay queue.
 * @author weigu
 *
 */
public class DelayQueue {

    @Setter
    private RedisService redisService;

    public Collection<String> getItems(final String queueName, final double from, final double end) {
        return redisService.zrange(queueName, from, end);
    }
}
