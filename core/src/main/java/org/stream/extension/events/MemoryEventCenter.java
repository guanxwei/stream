package org.stream.extension.events;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.stream.core.helper.Jackson;
import org.stream.extension.clients.MessageClient;
import org.stream.extension.io.HessianIOSerializer;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link EventCenter}.
 *
 * All the events will be stored in Memory first, then assign back-end worker to push all the events to Kafka Queue.
 * @author guanxiong wei
 *
 */
@Slf4j
public class MemoryEventCenter implements EventCenter {

    private BlockingQueue<Event> pendingEvents = new LinkedBlockingQueue<>();

    @Setter
    private MessageClient kafkaClient;

    private Map<Class<? extends Event>, List<Listener>> listeners = new ConcurrentHashMap<>();

    private Object monitor = new Object();

    private ExecutorService service;

    @Setter
    private String topic;

    private volatile boolean shutingDown = false;

    @Setter
    private volatile boolean sendOnly = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireEvent(final Event event) {
        if (shutingDown) {
            throw new RuntimeException("JVM has been shuted down");
        }
        if ("Anony".equals(event.type())) {
            log.warn("Unsupported event type");
            return;
        }
        pendingEvents.offer(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(final Class<? extends Event> eventClass, final Listener listener) {
        synchronized (monitor) {
            listeners.computeIfAbsent(eventClass, k -> new LinkedList<>());
        }
        listeners.get(eventClass).add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(final Listener listener) {
        for (List<Listener> eventListeners : listeners.values()) {
            eventListeners.remove(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerMutilChannelListerner(final List<Class<? extends Event>> events, final Listener listener) {
        for (Class<? extends Event> clazz : events) {
            if (Event.class.isAssignableFrom(clazz)) {
                registerListener(clazz, listener);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Listener> getListenerListByEventType(final Class<?> type) {
        if (!type.getSuperclass().isAssignableFrom(Event.class)) {
            // Only event type can be key of the map.
            return Collections.emptyList();
        }
        return listeners.get(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireSyncEvent(final Event event) {
        if (shutingDown) {
            throw new RuntimeException("JVM has been shuted down");
        }
        try {
            String eventEntity = Jackson.json(event);
            log.trace("Receive request to deliver event synchronouly, will push it to the Kafka queue service immediately",
                    eventEntity, event.getClass().getSimpleName());
            kafkaClient.sendMessage(topic, event.getClass().getSimpleName(),
                    HessianIOSerializer.encode(event));
            log.trace("Event [{}] sent to Kafka cluster successfully", eventEntity);
        } catch (Exception e) {
            log.warn("Event dispatch error!", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Initiate new threads to process the events.
     * Our strategy is to send a message to Kafka cluster for each event,
     * let the Kafaka consumers to process the event asynchronously.
     */
    public void init() {
        // We just create a thread pool with threads up limit (1 + number of event types * 2).
        service = Executors.newFixedThreadPool(1 + listeners.keySet().size() * 2);
        log.info("Event center initiating...");
        log.info("Find kafka topic [{}]", topic);
        Runnable pipeline = createPipelineWorker();
        service.submit(pipeline);
        if (!sendOnly) {
            log.info("Register event listeners");
            for (Class<? extends Event> clazz : listeners.keySet()) {
                // Assign two threads to process one kind of event.
                service.submit(createListenerNotifiers(clazz));
                service.submit(createListenerNotifiers(clazz));
            }
            log.info("Event center initiated.");
            registerStopHook();
            return;
        }

        log.info("This machine is only used to send kafka messages! Please deploy consumer machine somewhere else in case"
                + " messages are over accumulated");
    }

    private Runnable createPipelineWorker() {
        Runnable producer = new Runnable() {
            @Override
            public void run() {
                log.info("Pipeline worker [{}] started working to transfer event message to Kafka queue service", Thread.currentThread().getName());
                while (true) {
                    retrieveAndProcess(false);
                }
            }
        };
        return producer;
    }

    private void retrieveAndProcess(final boolean discarded) {
        Event event = pendingEvents.poll();
        kafkaClient.sendMessage(topic, event.getClass().getSimpleName(), HessianIOSerializer.encode(event));
    }

    private Runnable createListenerNotifiers(final Class<? extends Event> clazz) {
        log.info("Create event type [{}] listener notify worker", clazz.getSimpleName());
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                log.info("Listener notify worker [{}] start working to pull event message from Kafka queue service for topic [{}] and key [{}]",
                        Thread.currentThread().getName(), topic, clazz.getSimpleName());
                while (true) {
                    try {
                        byte[] message = kafkaClient.pullMessage(clazz.getSimpleName());
                        log.info("Incoming event [{}]", message);
                        if (message == null) {
                            continue;
                        }
                        Event event = HessianIOSerializer.decode(message, clazz);
                        if (listeners.containsKey(clazz)) {
                            for (Listener listener : listeners.get(clazz)) {
                                try {
                                    listener.handle(event);
                                } catch (Exception e) {
                                    fireEvent(event);
                                    log.warn("Handler [{}] failed to handle the event message [{}]", listener.getClass().getSimpleName(), message);
                                }
                            }
                        }
                        kafkaClient.markAsConsumed();
                    } catch (Exception e) {
                        log.warn("Listener notification error", e);
                    }
                }
            }
        };
        return worker;
    }

    private void registerStopHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutingDown = true;
            while (!pendingEvents.isEmpty()) {
                retrieveAndProcess(true);
            }
        }));
    }
}
