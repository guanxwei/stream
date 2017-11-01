package org.stream.extension.clients;

import redis.clients.jedis.HostAndPort;

import java.util.List;

import lombok.Data;

/**
 * Redis master topology structure info holder.
 */
@Data
public class JedisShardTopoInfo {

    private String masterName;

    private HostAndPort master;

    private List<HostAndPort> slaves;

}
