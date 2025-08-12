package com.eventstream.processor;

import com.eventstream.handler.EventHandler;
import com.eventstream.handler.EventHandlerRegistry;
import com.eventstream.model.Event;
import com.eventstream.model.EntityState;
import com.eventstream.snapshot.SnapshotManager;
import com.eventstream.state.StateManager;
import com.eventstream.stream.EventStream;
import java.time.Instant;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

/**
 * Main event processing engine that coordinates all components
 */
public class EventProcessor {
    private static final Logger logger = Logger.getLogger(EventProcessor.class.getName());
    
    private final StateManager stateManager;
    private final SnapshotManager snapshotManager;
    private final EventHandlerRegistry handlerRegistry;
    private final EventStream eventStream;
    
    // Event storage for rollback purposes - sorted by timestamp
    private final ConcurrentSkipListMap<String, ConcurrentSkipListMap<Instant, Event>> entityEvents = new ConcurrentSkipListMap<>();
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong processedEventCount = new AtomicLong(0);
    private final AtomicLong failedEventCount = new AtomicLong(0);
    
    // Configuration
    private final int snapshotFrequency; // Create snapshot every N events per entity
    private final int maxEventsPerEntity; // Max events to keep in memory per entity

    public EventProcessor(EventStream eventStream, StateManager stateManager, 
                         SnapshotManager snapshotManager, EventHandlerRegistry handlerRegistry) {
        this(eventStream, stateManager, snapshotManager, handlerRegistry, 100, 10000);
    }

    public EventProcessor(EventStream eventStream, StateManager stateManager, 
                         SnapshotManager snapshotManager, EventHandlerRegistry handlerRegistry,
                         int snapshotFrequency, int maxEventsPerEntity) {
        this.eventStream = eventStream;
        this.stateManager = stateManager;
        this.snapshotManager = snapshotManager;
        this.handlerRegistry = handlerRegistry;
        this.snapshotFrequency = snapshotFrequency;
        this.maxEventsPerEntity = maxEventsPerEntity;
    }

    /**
     * Start the event processor
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            eventStream.subscribe(this::processEvent);
            eventStream.start();
            logger.info("Event processor started");
        }
    }

    /**
     * Stop the event processor
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            eventStream.stop();
            logger.info("Event processor stopped");
        }
    }

    /**
     * Check if the processor is running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Get current state of an entity
     */
    public Optional<EntityState> getCurrentState(String entityId) {
        return stateManager.getCurrentState(entityId);
    }

    /**
     * Get processing statistics
     */
    public ProcessingStatistics getStatistics() {
        return new ProcessingStatistics(
            processedEventCount.get(),
            failedEventCount.get(),
            stateManager.getEntityCount(),
            snapshotManager.getTotalSnapshotCount(),
            entityEvents.size()
        );
    }

    /**
     * Process a single event
     */
    private void processEvent(Event event) {
        try {
            String entityId = event.getEntityId();
            
            // Store event for rollback purposes
            storeEvent(event);
            
            // Get handler for this event type
            EventHandler handler = handlerRegistry.getHandler(event);
            if (handler == null) {
                logger.warning("No handler found for event type: " + event.getEventType());
                failedEventCount.incrementAndGet();
                return;
            }

            // Get current state
            Optional<EntityState> currentStateOpt = stateManager.getCurrentState(entityId);
            EntityState currentState = currentStateOpt.orElse(null);

            // Apply event to state
            EntityState newState = handler.apply(currentState, event);
            
            // Update state atomically
            stateManager.updateState(entityId, newState);
            
            // Check if we should create a snapshot
            long entityEventCount = getEntityEventCount(entityId);
            if (entityEventCount % snapshotFrequency == 0) {
                snapshotManager.createSnapshot(entityId, newState, event.getTimestamp(), event.getSequenceNumber());
            }
            
            processedEventCount.incrementAndGet();
            
            logger.fine("Processed event: " + event.getEventType() + " for entity: " + entityId);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing event: " + event, e);
            failedEventCount.incrementAndGet();
        }
    }

    /**
     * Store event for rollback purposes
     */
    private void storeEvent(Event event) {
        String entityId = event.getEntityId();
        ConcurrentSkipListMap<Instant, Event> entityEventMap = 
            entityEvents.computeIfAbsent(entityId, k -> new ConcurrentSkipListMap<>());
        
        entityEventMap.put(event.getTimestamp(), event);
        
        // Clean up old events if we exceed the limit
        cleanupOldEvents(entityId, entityEventMap);
    }

    /**
     * Clean up old events for an entity if we exceed the limit
     */
    private void cleanupOldEvents(String entityId, ConcurrentSkipListMap<Instant, Event> entityEventMap) {
        while (entityEventMap.size() > maxEventsPerEntity) {
            // Remove oldest event, but keep at least one for each snapshot
            var oldest = entityEventMap.firstEntry();
            if (oldest != null) {
                entityEventMap.remove(oldest.getKey());
                logger.fine("Removed old event for entity: " + entityId + " at " + oldest.getKey());
            }
        }
    }

    /**
     * Get the number of events stored for an entity
     */
    private long getEntityEventCount(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEventMap = entityEvents.get(entityId);
        return entityEventMap != null ? entityEventMap.size() : 0;
    }

    /**
     * Get events for an entity within a time range (for rollback)
     */
    public ConcurrentSkipListMap<Instant, Event> getEntityEventsInRange(String entityId, Instant fromTime, Instant toTime) {
        ConcurrentSkipListMap<Instant, Event> entityEventMap = entityEvents.get(entityId);
        if (entityEventMap == null) {
            return new ConcurrentSkipListMap<>();
        }
        
        return new ConcurrentSkipListMap<>(entityEventMap.subMap(fromTime, true, toTime, true));
    }

    /**
     * Get all events for an entity after a specific timestamp (for rollback)
     */
    public ConcurrentSkipListMap<Instant, Event> getEntityEventsAfter(String entityId, Instant afterTime) {
        ConcurrentSkipListMap<Instant, Event> entityEventMap = entityEvents.get(entityId);
        if (entityEventMap == null) {
            return new ConcurrentSkipListMap<>();
        }
        
        return new ConcurrentSkipListMap<>(entityEventMap.tailMap(afterTime, false));
    }

    /**
     * Processing statistics
     */
    public static class ProcessingStatistics {
        private final long processedEvents;
        private final long failedEvents;
        private final int entityCount;
        private final long snapshotCount;
        private final int entitiesWithEvents;

        public ProcessingStatistics(long processedEvents, long failedEvents, int entityCount, 
                                  long snapshotCount, int entitiesWithEvents) {
            this.processedEvents = processedEvents;
            this.failedEvents = failedEvents;
            this.entityCount = entityCount;
            this.snapshotCount = snapshotCount;
            this.entitiesWithEvents = entitiesWithEvents;
        }

        public long getProcessedEvents() {
            return processedEvents;
        }

        public long getFailedEvents() {
            return failedEvents;
        }

        public int getEntityCount() {
            return entityCount;
        }

        public long getSnapshotCount() {
            return snapshotCount;
        }

        public int getEntitiesWithEvents() {
            return entitiesWithEvents;
        }

        public double getSuccessRate() {
            long totalEvents = processedEvents + failedEvents;
            return totalEvents > 0 ? (double) processedEvents / totalEvents : 0.0;
        }

        @Override
        public String toString() {
            return "ProcessingStatistics{" +
                    "processedEvents=" + processedEvents +
                    ", failedEvents=" + failedEvents +
                    ", entityCount=" + entityCount +
                    ", snapshotCount=" + snapshotCount +
                    ", entitiesWithEvents=" + entitiesWithEvents +
                    ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) +
                    '}';
        }
    }
}