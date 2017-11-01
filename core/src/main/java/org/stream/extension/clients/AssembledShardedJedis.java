package org.stream.extension.clients;

import lombok.Data;
import redis.clients.jedis.ShardedJedis;

/**
 * Jedis client holder.
 */
@Data
public class AssembledShardedJedis {

    private boolean slaveUseable = true;

    private ShardedJedis masterShardedRedis;

    private ShardedJedis slaveShardedRedis;

}
