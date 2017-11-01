package org.stream.extension.clients;

import redis.clients.jedis.JedisPubSub;

/**
 * Default implementation of {@linkplain JedisPubSub}.
 * Default do nothing when receive message.
 */
public class JedisPubSubAdapter extends JedisPubSub {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(final String channel, final String message) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPMessage(final String pattern, final String channel, final String message) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPSubscribe(final String pattern, final int subscribedChannels) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPUnsubscribe(final String pattern, final int subscribedChannels) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSubscribe(final String channel, final int subscribedChannels) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUnsubscribe(final String channel, final int subscribedChannels) { }
}
