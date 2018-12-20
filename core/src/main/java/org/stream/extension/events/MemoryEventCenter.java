package org.stream.extension.events;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.stream.core.helper.Jackson;
import org.stream.extension.clients.KafkaClient;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link EventCenter}.
 *
 * All the events will be stored in Memory first, then assign back-end worker to push all the events to Kafka Queue.
 * @author hzweiguanxiong
 *
 */
@Slf4j
public class MemoryEventCenter implements EventCenter {

    private BlockingQueue<Event<?, ?>> pendingEvents = new LinkedBlockingQueue<>();

    @Setter
    private KafkaClient kafkaClient;

    private Map<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();

    private Object monitor = new Object();

    private ExecutorService service;

    @Setter
    private String topic;

    private volatile boolean shutingDown = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireEvent(final Event<?, ?> event) {
        if (shutingDown) {
            throw new RuntimeException("JVM has been shuted down");
        }
        pendingEvents.offer(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(final Class<?> eventClass, final Listener listener) {
        if (!listeners.containsKey(eventClass)) {
            synchronized (monitor) {
                List<Listener> eventListeners = new LinkedList<>();
                eventListeners.add(listener);
                listeners.put(eventClass, eventListeners);
            }
        } else {
            List<Listener> eventListeners = listeners.get(eventClass);
            if (!eventListeners.contains(listener)) {
                eventListeners.add(listener);
            }
        }
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
    public void registerMutilChannelListerner(final List<Class<?>> events, final Listener listener) {
        for (Class<?> clazz : events) {
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
            return null;
        }
        return listeners.get(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireSyncEvent(final Event<?, ?> event) throws Exception {
        if (shutingDown) {
            throw new RuntimeException("JVM has been shuted down");
        }
        try {
            String eventEntity = Jackson.json(event);
            log.info("Receive request to deliver event synchronouly, will push it to the Kafka queue service immediately",
                    eventEntity, event.getClass().getSimpleName());
            Future<RecordMetadata> result = kafkaClient.sendMessage(topic, event.getClass().getSimpleName(), eventEntity);
            result.get();
            log.info("Event [{}] sent to Kafka cluster successfully", eventEntity);
        } catch (Exception e) {
            log.warn("Event dispatch error!", e);
            throw e;
        }
    }

    /**
     * Initiate new threads to process the events.
     * Our strategy is to send a message to Kafaka cluster for each event,
     * let the Kafaka consumers to process the event asynchronously.
     */
    public void init() {
        // We just create a thread pool with threads up limit (1 + number of event types * 2).
        service = Executors.newFixedThreadPool(1 + listeners.keySet().size() * 2);
        log.info("Event center initiating...");
        log.info("Find kafka topic [{}]", topic);
        Runnable pipeline = createPipelineWorker();
        service.submit(pipeline);
        for (Class<?> clazz : listeners.keySet()) {
            //Assign two threads to process one kind of event.
            service.submit(createListenerNotifiers(clazz));
            service.submit(createListenerNotifiers(clazz));
        }
        log.info("Event center initiated.");
        registerStopHook();
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
        Event<?, ?> event = null;
        try {
            String eventEntity;
            event = pendingEvents.take();
            eventEntity = Jackson.json(event);
            log.info("Retrieve event entity [{}] with type [{}] from the buffered queue, will push it to the Kafka queue service",
                    eventEntity, event.getClass().getSimpleName());
            kafkaClient.sendMessage(topic, event.getClass().getSimpleName(), eventEntity);
            log.info("Event [{}] sent to Kafka cluster", eventEntity);
        } catch (Exception e) {
            log.warn("Event dispatch error!", e);
            if (!discarded) {
                try {
                    log.info("Take a little break! Will continue to work after one second.");
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    log.warn("Thread interrupted", e1);
                }
                pendingEvents.offer(event);
            }
        }
    }

    private Runnable createListenerNotifiers(final Class<?> clazz) {
        log.info("Create event type [{}] listener notify worker", clazz.getSimpleName());
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                log.info("Listener notify worker [{}] start working to pull event message from Kafka queue service for topic [{}] and key [{}]",
                        Thread.currentThread().getName(), topic, clazz.getSimpleName());
                while (true) {
                    try {
                        String message = kafkaClient.pullMessageAsString(clazz.getSimpleName());
                        log.info("Incoming event [{}]", message);
                        if (message == null) {
                            continue;
                        }
                        Event<?, ?> event = (Event<?, ?>) Jackson.parse(message, clazz);
                        for (Listener listener : listeners.get(clazz)) {
                            try {
                                listener.handle(event);
                            } catch (Exception e) {
                                fireEvent(event);
                                log.warn("Handler [{}] failed to handle the event message [{}]", listener.getClass().getSimpleName(), message);
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
