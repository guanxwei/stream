package org.stream.extension.clients;

import java.util.List;

import lombok.Data;
import redis.clients.jedis.JedisShardInfo;

/**
 * Jedis config holder.
 */
@Data
public class AssembledJedisShardInfo {

    // Master nodes.
    private List<JedisShardInfo> masters;

    // Slave nodes.
    private List<JedisShardInfo> slaves;
}
