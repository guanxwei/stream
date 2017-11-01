package org.stream.extension.clients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;

/**
 * Enhance implementation of Redis pool client.
 */
public class AssembledShardedJedisSentinelPool extends Pool<AssembledShardedJedis> {

    // CHECKSTYLE:OFF
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AssembledShardedJedisSentinelPool.class);

    public static final int MAX_RETRY_SENTINEL = 10;

    protected GenericObjectPoolConfig poolConfig;

    protected int timeout = Protocol.DEFAULT_TIMEOUT;

    private int sentinelRetry = 0;

    protected String password;

    protected int database = Protocol.DEFAULT_DATABASE;

    private List<String> masters;

    private List<String> sentinels;

    protected Set<MasterListener> masterListeners = new HashSet<MasterListener>();

    private volatile List<HostAndPort> currentHostMasters;

    private List<JedisShardTopoInfo> topoInfoList;

    private ScheduledThreadPoolExecutor timer;

    public AssembledShardedJedisSentinelPool(final List<String> masters, final Set<String> sentinels) {
        this(masters, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, null,
                Protocol.DEFAULT_DATABASE);
    }

    public AssembledShardedJedisSentinelPool(final List<String> masters, final Set<String> sentinels,
            final String password) {
        this(masters, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, password);
    }

    public AssembledShardedJedisSentinelPool(final GenericObjectPoolConfig poolConfig, final List<String> masters,
            final Set<String> sentinels) {
        this(masters, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
    }

    public AssembledShardedJedisSentinelPool(final List<String> masters, final Set<String> sentinels,
            final GenericObjectPoolConfig poolConfig, final int timeout, final String password) {
        this(masters, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
    }

    public AssembledShardedJedisSentinelPool(final List<String> masters, final Set<String> sentinels,
            final GenericObjectPoolConfig poolConfig, final int timeout) {
        this(masters, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
    }

    public AssembledShardedJedisSentinelPool(final List<String> masters, final Set<String> sentinels,
            final GenericObjectPoolConfig poolConfig, final String password) {
        this(masters, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
    }

    public AssembledShardedJedisSentinelPool(final List<String> masters, final Set<String> sentinels,
            final GenericObjectPoolConfig poolConfig, final int timeout, final String password, final int database) {
        this.poolConfig = poolConfig;
        this.timeout = timeout;
        this.password = password;
        this.database = database;
        this.masters = masters;
        this.sentinels = new ArrayList<>(sentinels);

        topoInfoList = initSentinels(sentinels, masters);
        initPool(topoInfoList);
        timer = new ScheduledThreadPoolExecutor(1);
        timer.scheduleAtFixedRate(new SlaveFetcher(), 5, 5, TimeUnit.SECONDS);
    }

    public void destroy() {
        for (MasterListener m : masterListeners) {
            m.shutdown();
        }

        super.destroy();
    }

    public List<HostAndPort> getCurrentHostMaster() {
        return currentHostMasters;
    }

    private synchronized void initPool(List<JedisShardTopoInfo> topoInfoList) {

        List<HostAndPort> masters = new ArrayList<>();
        for (JedisShardTopoInfo topoInfo : topoInfoList) {
            masters.add(topoInfo.getMaster());
        }
        List<HostAndPort> slaves = new ArrayList<>();
        for (JedisShardTopoInfo topoInfo : topoInfoList) {
            if (topoInfo.getSlaves() != null && !topoInfo.getSlaves().isEmpty()) {
                slaves.addAll(topoInfo.getSlaves());
            }
        }

        if (!equals(currentHostMasters, masters)) {
            doInitiate(masters, slaves);
        }
    }

    private boolean equals(List<HostAndPort> currentShardMasters, List<HostAndPort> shardMasters) {
        if (currentShardMasters != null && shardMasters != null) {
            if (currentShardMasters.size() == shardMasters.size()) {
                for (int i = 0; i < currentShardMasters.size(); i++) {
                    if (!currentShardMasters.get(i).equals(shardMasters.get(i)))
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    private void doInitiate(final List<HostAndPort> masters, final List<HostAndPort> slaves) {
        StringBuffer sb = new StringBuffer();
        for (HostAndPort master : masters) {
            sb.append(master.toString());
            sb.append(" ");
        }
        logger.info("Created ShardedJedisPool to master at [" + sb.toString() + "]");
        List<JedisShardInfo> shardMasters = makeShardInfoList(masters);
        List<JedisShardInfo> shardSlaves = makeShardInfoList(slaves);
        AssembledJedisShardInfo assembledJedisShardInfo = new AssembledJedisShardInfo();
        assembledJedisShardInfo.setMasters(shardMasters);
        assembledJedisShardInfo.setSlaves(shardSlaves);
        initPool(poolConfig, new ShardedJedisFactory(assembledJedisShardInfo, Hashing.MURMUR_HASH, null));
        currentHostMasters = masters;
    }

    private List<JedisShardInfo> makeShardInfoList(List<HostAndPort> shards) {
        List<JedisShardInfo> shardMasters = new ArrayList<JedisShardInfo>();
        for (HostAndPort shard : shards) {
            if (shard == null) {
                shardMasters.add(null);
            } else {
                JedisShardInfo jedisShardInfo = new JedisShardInfo(shard.getHost(), shard.getPort(), timeout);
                jedisShardInfo.setPassword(password);
                shardMasters.add(jedisShardInfo);
            }
        }
        return shardMasters;
    }

    private List<JedisShardTopoInfo> initSentinels(final Set<String> sentinels, final List<String> masters) {

        List<JedisShardTopoInfo> topoInfoList = new CopyOnWriteArrayList<>();

        Map<String, JedisShardTopoInfo> masterMap = new HashMap<>();
        List<HostAndPort> shardMasters = new ArrayList<>();

        logger.info("Trying to find all master from available Sentinels...");

        for (String masterName : masters) {
            boolean fetched = false;
            sentinelRetry = 0;
            while (!fetched && sentinelRetry < MAX_RETRY_SENTINEL) {
                fetched = detectSentinels(topoInfoList, masterMap, masterName, shardMasters);
            }
            // Try MAX_RETRY_SENTINEL times.
            if (!fetched && sentinelRetry >= MAX_RETRY_SENTINEL) {
                logger.warn("All sentinels down and try " + MAX_RETRY_SENTINEL + " times, Abort.");
                throw new JedisConnectionException("Cannot connect all sentinels, Abort.");
            }
        }

        // All shards master must been accessed.
        if (masters.size() != 0 && masters.size() == shardMasters.size()) {
            startSentinelListeners();
        }

        return topoInfoList;
    }

    private boolean detectSentinels(final List<JedisShardTopoInfo> topoInfoList, final Map<String, JedisShardTopoInfo> masterMap,
            final String masterName, final List<HostAndPort> shardMasters) {
        boolean succeed = false;
        JedisShardTopoInfo topoInfo = masterMap.get(masterName);
        if (topoInfo == null) {
            for (String sentinel : sentinels) {
                final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel.split(":")));
                logger.info("Connecting to Sentinel " + hap);
                try (Jedis jedis = new Jedis(hap.getHost(), hap.getPort())) {
                    succeed = loadMasterMetaInfo(topoInfoList, jedis, masterName, shardMasters, masterMap);
                    if (succeed) {
                        topoInfo = masterMap.get(masterName);
                        break;
                    }
                } catch (JedisConnectionException e) {
                    logger.warn("Cannot connect to sentinel running @ " + hap + ". Trying next one.");
                }
            }
        }

        if (null == topoInfo) {
            sleepOnMaster(masterName);
        }
        return succeed;
    }

    private boolean loadMasterMetaInfo(final List<JedisShardTopoInfo> topoInfoList, final Jedis jedis, final String masterName, final List<HostAndPort> shardMasters,
            final Map<String, JedisShardTopoInfo> masterMap) {
        List<String> hostAndPort = jedis.sentinelGetMasterAddrByName(masterName);
        List<Map<String, String>> slaveMapList = jedis.sentinelSlaves(masterName);
        boolean succeed = false;

        if (hostAndPort != null && hostAndPort.size() > 0) {
            HostAndPort masterHAP = toHostAndPort(hostAndPort);
            shardMasters.add(masterHAP);
            JedisShardTopoInfo topoInfo = new JedisShardTopoInfo();
            topoInfo.setMasterName(masterName);
            topoInfo.setMaster(masterHAP);

            if (slaveMapList != null && !slaveMapList.isEmpty()) {
                List<HostAndPort> slaveHAPs = convertSlaveInfo(slaveMapList);
                topoInfo.setSlaves(slaveHAPs);
            } else {
                topoInfo.setSlaves(Collections.emptyList());
            }

            logger.info("Found Redis master at " + topoInfo);
            topoInfoList.add(topoInfo);
            masterMap.put(masterName, topoInfo);
            succeed = true;
            jedis.disconnect();
        }
        return succeed;
    }

    private HostAndPort toHostAndPort(final List<String> getMasterAddrByNameResult) {
        String host = getMasterAddrByNameResult.get(0);
        int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

        return new HostAndPort(host, port);
    }

    private void sleepOnMaster(final String masterName) {
        try {
            logger.info("All sentinels down, cannot determine where is "
                    + masterName + " master is running... sleeping 1000ms, Will try again.");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sentinelRetry++;
    }

    private List<HostAndPort> convertSlaveInfo(List<Map<String, String>> infoMapList) {

        List<HostAndPort> hostAndPortList = new CopyOnWriteArrayList<>();
        for (Map<String, String> infoMap : infoMapList) {
            // 取活着的slave;
            if (infoMap.containsKey("runid") && StringUtils.isNotEmpty(infoMap.get("runid"))) {
                String host = infoMap.get("ip");
                int port = Integer.parseInt(infoMap.get("port"));
                HostAndPort hostAndPort = new HostAndPort(host, port);
                hostAndPortList.add(hostAndPort);
            }
        }

        return hostAndPortList;
    }

    private void startSentinelListeners() {
        logger.info("Starting Sentinel listeners...");
        for (String sentinel : sentinels) {
            final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel.split(":")));
            MasterListener masterListener = new MasterListener(masters, hap.getHost(), hap.getPort());
            masterListeners.add(masterListener);
            masterListener.start();
        }
    }

    /**
     * PoolableObjectFactory custom impl.
     */
    protected static class ShardedJedisFactory implements PooledObjectFactory<AssembledShardedJedis> {
        private AssembledJedisShardInfo shards;
        private Hashing algo;
        private Pattern keyTagPattern;

        public ShardedJedisFactory(AssembledJedisShardInfo shards, Hashing algo, Pattern keyTagPattern) {
            this.shards = shards;
            this.algo = algo;
            this.keyTagPattern = keyTagPattern;
        }

        public PooledObject<AssembledShardedJedis> makeObject() throws Exception {
            AssembledShardedJedis assembledShardedJedis = new AssembledShardedJedis();

            ShardedJedis masterJedis = new ShardedJedis(shards.getMasters(), algo, keyTagPattern);
            assembledShardedJedis.setMasterShardedRedis(masterJedis);

            if (shards.getSlaves() == null || shards.getSlaves().size() == 0 || shards.getSlaves().contains(null)) {
                assembledShardedJedis.setSlaveUseable(false);
            }
            if (assembledShardedJedis.isSlaveUseable()) {
                ShardedJedis slaveJedis = new ShardedJedis(shards.getSlaves(), algo, keyTagPattern);
                assembledShardedJedis.setSlaveShardedRedis(slaveJedis);
            }

            return new DefaultPooledObject<AssembledShardedJedis>(assembledShardedJedis);
        }

        public void destroyObject(PooledObject<AssembledShardedJedis> pooledShardedJedis) throws Exception {
            final ShardedJedis shardedJedis = pooledShardedJedis.getObject().getMasterShardedRedis();
            for (Jedis jedis : shardedJedis.getAllShards()) {
                try {
                    jedis.quit();
                    jedis.disconnect();
                } catch (Throwable e) {
                    AssembledShardedJedisSentinelPool.logger.error("pool destroyObject master error:" + e);
                }
            }

            final ShardedJedis slavesJedis = pooledShardedJedis.getObject().getSlaveShardedRedis();
            for (Jedis jedis : slavesJedis.getAllShards()) {
                try {
                    jedis.quit();
                    jedis.disconnect();
                } catch (Throwable e) {
                    AssembledShardedJedisSentinelPool.logger.error("pool destroyObject slave error:" + e);
                }
            }
        }

        public boolean validateObject(PooledObject<AssembledShardedJedis> pooledShardedJedis) {
            try {
                ShardedJedis jedis = pooledShardedJedis.getObject().getMasterShardedRedis();
                for (Jedis shard : jedis.getAllShards()) {
                    try {
                        if (!shard.ping().equals("PONG")) {
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }

                }

                ShardedJedis slave = pooledShardedJedis.getObject().getSlaveShardedRedis();
                for (Jedis shard : slave.getAllShards()) {
                    try {
                        if (!shard.ping().equals("PONG")) {
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        public void activateObject(PooledObject<AssembledShardedJedis> p) throws Exception {

        }

        public void passivateObject(PooledObject<AssembledShardedJedis> p) throws Exception {

        }
    }

    /**
     * 定时去load下slave,发现新的slave,作为监听的补充,监听不允许同时获取;
     */
    private class SlaveFetcher implements Runnable {

        private static final int SLAVE_MAX_RETRY_SENTINEL = 3;

        @Override
        public void run() {

            for (String masterName : masters) {
                List<Map<String, String>> slaveMapList = null;
                boolean fetched = false;
                sentinelRetry = 0;
                while (!fetched && sentinelRetry < SLAVE_MAX_RETRY_SENTINEL) {
                    for (String sentinel : sentinels) {
                        final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel.split(":")));

                        // logger.info("Connecting to Sentinel " + hap);
                        Jedis jedis = null;
                        try {
                            jedis = new Jedis(hap.getHost(), hap.getPort());
                            if (slaveMapList == null) {
                                slaveMapList = jedis.sentinelSlaves(masterName);

                                JedisShardTopoInfo topoInfo = topoInfoList.get(masters.indexOf(masterName));
                                if (slaveMapList != null && !slaveMapList.isEmpty()) {
                                    List<HostAndPort> slaveHAPs = convertSlaveInfo(slaveMapList);
                                    topoInfo.setSlaves(slaveHAPs);
                                } else {
                                    topoInfo.setSlaves(Collections.emptyList());
                                }
                                fetched = true;
                                jedis.disconnect();
                                break;
                            }
                        } catch (JedisConnectionException e) {
                            logger.warn("Cannot connect to sentinel running @ " + hap + ". Trying next one.");
                        } finally {
                            if (jedis != null) {
                                jedis.close();
                            }
                        }
                    }

                    if (null == slaveMapList) {
                        try {
                            logger.info("All sentinels down, cannot determine where is " + masterName
                                    + " master is running... sleeping 1000ms, Will try again.");
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        fetched = false;
                        sentinelRetry++;
                    }
                }

                // Try SLAVE_MAX_RETRY_SENTINEL times.
                if (!fetched && sentinelRetry >= SLAVE_MAX_RETRY_SENTINEL) {
                    logger.warn("All sentinels down and try " + SLAVE_MAX_RETRY_SENTINEL + " times, Abort.");
                    throw new JedisConnectionException("Cannot connect all sentinels, Abort.");
                }
            }
            for (JedisShardTopoInfo topoInfo : topoInfoList) {
                System.out.println(topoInfo.getMaster() + ">>>" + topoInfo.getSlaves());
            }
        }
    }

    protected class MasterListener extends Thread {

        protected List<String> masters;
        protected String host;
        protected int port;
        protected long subscribeRetryWaitTimeMillis = 5000;
        protected Jedis jedis;
        protected AtomicBoolean running = new AtomicBoolean(false);

        protected MasterListener() { }

        public MasterListener(List<String> masters, String host, int port) {
            this.masters = masters;
            this.host = host;
            this.port = port;
        }

        public MasterListener(List<String> masters, String host, int port, long subscribeRetryWaitTimeMillis) {
            this(masters, host, port);
            this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
        }

        public void run() {
            running.set(true);
            while (running.get()) {
                jedis = new Jedis(host, port);
                try {
                    jedis.subscribe(new JedisPubSubAdapter() {
                        @Override
                        public void onMessage(final String channel, final String message) {
                            processMessage(message);
                        }
                    }, "+switch-master");
                } catch (Exception e) {
                    sleep();
                }
            }
        }

        private void processMessage(final String message) {
         // mesg format : <master name> <oldip> <oldport>
            // <newip> <newport>
            logger.info("Sentinel " + host + ":" + port + " published: " + message + ".");
            String[] switchMasterMsg = message.split(" ");
            try {
                if (switchMasterMsg.length > 3) {
                    int index = masters.indexOf(switchMasterMsg[0]);
                    if (index >= 0) {
                        HostAndPort newHostMaster = toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4]));
                        JedisShardTopoInfo topoInfo = topoInfoList.get(index);
                        topoInfo.setMaster(newHostMaster);
                        // 直接remove掉,没办法在这里获取最新的slave列表;
                        topoInfo.getSlaves().remove(newHostMaster);
                        initPool(topoInfoList);
                    } else {
                        StringBuffer sb = new StringBuffer();
                        for (String masterName : masters) {
                            sb.append(masterName);
                            sb.append(",");
                        }
                        logger.info("Ignoring message on + switch-master for master name "
                                + switchMasterMsg[0] + ", our monitor master name are [" + sb + "]");
                    }
                } else {
                    logger.error("Invalid message received on Sentinel " + host + ":" + port
                            + " on channel +switch-master: " + message);
                }
            } catch (Exception e) {
                logger.error("onMessage error:", e);
            }
        }

        private void sleep() {
            if (running.get()) {
                logger.error("Lost connection to Sentinel at " + host + ":" + port
                        + ". Sleeping 5000ms and retrying.");
                try {
                    Thread.sleep(subscribeRetryWaitTimeMillis);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } else {
                logger.error("Unsubscribing from Sentinel at " + host + ":" + port);
            }
        }

        public void shutdown() {
            try {
                logger.info("Shutting down listener on " + host + ":" + port);
                running.set(false);
                // This isn't good, the Jedis object is not thread safe
                jedis.disconnect();
                jedis.close();
            } catch (Exception e) {
                logger.error("Caught exception while shutting down: " + e.getMessage());
            }
        }
    }
    // CHECKSTYLE:ON
}
