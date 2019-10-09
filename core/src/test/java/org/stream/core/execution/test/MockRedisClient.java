package org.stream.core.execution.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.stream.extension.clients.RedisClient;

import lombok.Builder;
import lombok.Data;

public class MockRedisClient implements RedisClient {

    private static final ConcurrentHashMap<String, String> ITEMS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<String>> SETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<String>> LISTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Item>> ZLISTS = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean set(final String key, final String value) {
        return ITEMS.put(key, value) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long sadd(final String key, final String value) {
        Set<String> list = SETS.get(key);
        if (list == null) {
            SETS.putIfAbsent(key, new HashSet<>());
            list = SETS.get(key);
        }
        list.add(value);
        return 1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean del(final String key) {
        return ITEMS.remove(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long sremove(final String setKey, final String... memebers) {
        Set<String> set = SETS.get(setKey);
        long value = 0;
        if (set != null && memebers != null) {
            for (String member : memebers) {
                value += set.remove(member) ? 1l : 0l;
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get(final String key) {
        return ITEMS.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> lrange(final String key, final long start, final long end) {
        List<String> list = LISTS.get(key);
        if (!CollectionUtils.isEmpty(list)) {
            int first = Long.valueOf(start).intValue();
            int last = Long.valueOf(end).intValue();
            synchronized (list) {
                int size = list.size();
                last = size < end ? size : last;
                first = start > size ? size - 1 : first;
                return list.subList(first, last);
            }
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long setnx(String key, String value) {
        return ITEMS.putIfAbsent(key, value) == null ? 1l : 0l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long lpush(final String key, final String... value) {
        List<String> list = LISTS.get(key);
        if (list == null) {
            LISTS.putIfAbsent(key, new LinkedList<>());
            list = LISTS.get(key);
        }
        synchronized (list) {
            list.addAll(Arrays.asList(value));
        }
        return 1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean lrem(final String key, final int count, final String value) {
        List<String> list = LISTS.get(key);
        if (list != null) {
            synchronized (list) {
                list.remove(value);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setWithExpireTime(final String key, final String value, final int seconds) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long zadd(final String key, final String value, final double score) {
        List<Item> zlist = ZLISTS.get(key);
        if (zlist == null) {
            ZLISTS.putIfAbsent(key, new LinkedList<>());
            zlist = ZLISTS.get(key);
        }

        synchronized (zlist) {
            Item item = Item.builder()
                    .value(value)
                    .score(score)
                    .build();
            zlist.add(item);
        }
        return 1l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> zrange(final String key, final double begin, final double end) {
        List<Item> zlist = ZLISTS.get(key);
        if (zlist != null) {
            synchronized (zlist) {
                return zlist.stream()
                            .filter(item -> item.getScore() >= begin && item.getScore() <= end)
                            .map(Item::getValue)
                            .collect(Collectors.toSet());
            }
        }

        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean zdel(final String set, final String key) {
        List<Item> zlist = ZLISTS.get(set);
        if (zlist != null) {
            synchronized (zlist) {
                zlist.removeIf(item -> StringUtils.equals(key, item.getValue()));
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean rpush(final String set, final String key) {
        List<String> list = LISTS.get(set);
        if (list == null) {
            LISTS.putIfAbsent(key, new LinkedList<>());
            list = LISTS.get(key);
        }
        return list.add(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getListSize(final String list) {
        List<String> target = LISTS.get(list);
        return target == null ? 0 : target.size();
    }

    @Data
    @Builder
    public static class Item {
        private String value;
        private double score;
    }
}
