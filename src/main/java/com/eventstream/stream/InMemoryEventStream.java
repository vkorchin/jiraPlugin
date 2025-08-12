package com.eventstream.stream;

import com.eventstream.model.Event;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory implementation of EventStream for testing and demonstration
 */
public class InMemoryEventStream implements EventStream {
    private static final Logger logger = Logger.getLogger(InMemoryEventStream.class.getName());
    
    private final BlockingQueue<Event> eventQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong sequenceGenerator = new AtomicLong(0);
    
    private Consumer<Event> eventConsumer;
    private ExecutorService executorService;

    public InMemoryEventStream() {
        this.eventQueue = new LinkedBlockingQueue<>();
    }

    public InMemoryEventStream(int queueCapacity) {
        this.eventQueue = new LinkedBlockingQueue<>(queueCapacity);
    }

    @Override
    public void subscribe(Consumer<Event> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            if (eventConsumer == null) {
                throw new IllegalStateException("Event consumer must be set before starting");
            }
            
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EventStream-Consumer");
                t.setDaemon(true);
                return t;
            });
            
            executorService.submit(this::processEvents);
            logger.info("Event stream started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (executorService != null) {
                executorService.shutdown();
            }
            logger.info("Event stream stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public long getProcessedEventCount() {
        return processedCount.get();
    }

    /**
     * Add event to the stream (for testing purposes)
     */
    public void addEvent(String entityId, String eventType, Map<String, Object> payload) {
        if (!running.get()) {
            throw new IllegalStateException("Stream is not running");
        }
        
        Event event = new Event(entityId, Instant.now(), eventType, payload, sequenceGenerator.incrementAndGet());
        
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while adding event", e);
        }
    }

    /**
     * Add event with specific timestamp (for testing purposes)
     */
    public void addEvent(String entityId, Instant timestamp, String eventType, Map<String, Object> payload) {
        if (!running.get()) {
            throw new IllegalStateException("Stream is not running");
        }
        
        Event event = new Event(entityId, timestamp, eventType, payload, sequenceGenerator.incrementAndGet());
        
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while adding event", e);
        }
    }

    /**
     * Get current queue size (for monitoring)
     */
    public int getQueueSize() {
        return eventQueue.size();
    }

    private void processEvents() {
        while (running.get()) {
            try {
                Event event = eventQueue.take();
                eventConsumer.accept(event);
                processedCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Event processing interrupted");
                break;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error processing event", e);
            }
        }
    }

    @Override
    public void close() {
        stop();
    }
}