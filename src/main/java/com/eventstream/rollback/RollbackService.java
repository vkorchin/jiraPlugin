package com.eventstream.rollback;

import com.eventstream.handler.EventHandler;
import com.eventstream.handler.EventHandlerRegistry;
import com.eventstream.model.EntityState;
import com.eventstream.model.Event;
import com.eventstream.model.Snapshot;
import com.eventstream.processor.EventProcessor;
import com.eventstream.snapshot.SnapshotManager;
import com.eventstream.state.StateManager;
import java.time.Instant;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling entity state rollback operations with optimization
 */
public class RollbackService {
    private static final Logger logger = Logger.getLogger(RollbackService.class.getName());
    
    private final StateManager stateManager;
    private final SnapshotManager snapshotManager;
    private final EventHandlerRegistry handlerRegistry;
    private final EventProcessor eventProcessor;
    private final ExecutorService rollbackExecutor;

    public RollbackService(StateManager stateManager, SnapshotManager snapshotManager, 
                          EventHandlerRegistry handlerRegistry, EventProcessor eventProcessor) {
        this.stateManager = stateManager;
        this.snapshotManager = snapshotManager;
        this.handlerRegistry = handlerRegistry;
        this.eventProcessor = eventProcessor;
        this.rollbackExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Rollback-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Rollback entity state to a specific timestamp (synchronous)
     * Target: < 100ms for most operations
     * 
     * @param entityId the entity ID
     * @param targetTimestamp the target timestamp
     * @return optional entity state at the target timestamp
     */
    public Optional<EntityState> rollbackEntityState(String entityId, Instant targetTimestamp) {
        long startTime = System.nanoTime();
        
        try {
            // Strategy 1: Try to find the best snapshot before target timestamp
            Optional<Snapshot> bestSnapshot = snapshotManager.findBestSnapshotForRollback(entityId, targetTimestamp);
            
            EntityState baseState = null;
            Instant replayFromTime = null;
            
            if (bestSnapshot.isPresent()) {
                // Use snapshot as base state
                Snapshot snapshot = bestSnapshot.get();
                baseState = snapshot.getState();
                replayFromTime = snapshot.getTimestamp();
                
                logger.fine("Using snapshot from " + replayFromTime + " for rollback to " + targetTimestamp);
            } else {
                // No snapshot available, start from scratch
                baseState = null;
                replayFromTime = Instant.EPOCH;
                
                logger.fine("No snapshot available, replaying all events for entity: " + entityId);
            }
            
            // Get events to replay from snapshot time to target time
            ConcurrentSkipListMap<Instant, Event> eventsToReplay = 
                eventProcessor.getEntityEventsInRange(entityId, replayFromTime, targetTimestamp);
            
            // Replay events on the base state
            EntityState rolledBackState = replayEvents(baseState, eventsToReplay);
            
            long endTime = System.nanoTime();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            
            logger.info("Rollback completed for entity: " + entityId + " in " + durationMs + "ms");
            
            return Optional.ofNullable(rolledBackState);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during rollback for entity: " + entityId, e);
            return Optional.empty();
        }
    }

    /**
     * Rollback entity state asynchronously
     * 
     * @param entityId the entity ID
     * @param targetTimestamp the target timestamp
     * @return CompletableFuture with the rollback result
     */
    public CompletableFuture<Optional<EntityState>> rollbackEntityStateAsync(String entityId, Instant targetTimestamp) {
        return CompletableFuture.supplyAsync(() -> rollbackEntityState(entityId, targetTimestamp), rollbackExecutor);
    }

    /**
     * Rollback multiple entities to the same timestamp
     * 
     * @param entityIds array of entity IDs
     * @param targetTimestamp the target timestamp
     * @return map of entity ID to rolled back state
     */
    public CompletableFuture<Map<String, Optional<EntityState>>> rollbackMultipleEntities(
            String[] entityIds, Instant targetTimestamp) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Optional<EntityState>> results = new ConcurrentSkipListMap<>();
            
            // Process entities in parallel
            CompletableFuture<?>[] futures = new CompletableFuture[entityIds.length];
            
            for (int i = 0; i < entityIds.length; i++) {
                String entityId = entityIds[i];
                futures[i] = CompletableFuture.runAsync(() -> {
                    Optional<EntityState> result = rollbackEntityState(entityId, targetTimestamp);
                    results.put(entityId, result);
                }, rollbackExecutor);
            }
            
            // Wait for all rollbacks to complete
            CompletableFuture.allOf(futures).join();
            
            return results;
        }, rollbackExecutor);
    }

    /**
     * Create a temporary rollback view without affecting current state
     * This is useful for queries without modifying the actual entity state
     * 
     * @param entityId the entity ID
     * @param targetTimestamp the target timestamp
     * @return rollback result with state and metadata
     */
    public RollbackResult createRollbackView(String entityId, Instant targetTimestamp) {
        long startTime = System.nanoTime();
        
        Optional<EntityState> rolledBackState = rollbackEntityState(entityId, targetTimestamp);
        Optional<EntityState> currentState = stateManager.getCurrentState(entityId);
        
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        return new RollbackResult(
            entityId,
            targetTimestamp,
            rolledBackState,
            currentState,
            durationMs,
            rolledBackState.isPresent()
        );
    }

    /**
     * Replay events on a base state
     */
    private EntityState replayEvents(EntityState baseState, ConcurrentSkipListMap<Instant, Event> events) {
        EntityState currentState = baseState;
        
        for (Event event : events.values()) {
            EventHandler handler = handlerRegistry.getHandler(event);
            if (handler == null) {
                logger.warning("No handler found for event type during replay: " + event.getEventType());
                continue;
            }
            
            try {
                currentState = handler.apply(currentState, event);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error replaying event: " + event, e);
                // Continue with next event
            }
        }
        
        return currentState;
    }

    /**
     * Get rollback performance statistics
     */
    public RollbackStatistics getStatistics() {
        return new RollbackStatistics(
            snapshotManager.getTotalSnapshotCount(),
            snapshotManager.getEntityCount(),
            stateManager.getEntityCount()
        );
    }

    /**
     * Shutdown the rollback service
     */
    public void shutdown() {
        rollbackExecutor.shutdown();
        try {
            if (!rollbackExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                rollbackExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            rollbackExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Result of a rollback operation
     */
    public static class RollbackResult {
        private final String entityId;
        private final Instant targetTimestamp;
        private final Optional<EntityState> rolledBackState;
        private final Optional<EntityState> currentState;
        private final long durationMs;
        private final boolean successful;

        public RollbackResult(String entityId, Instant targetTimestamp, 
                             Optional<EntityState> rolledBackState, Optional<EntityState> currentState,
                             long durationMs, boolean successful) {
            this.entityId = entityId;
            this.targetTimestamp = targetTimestamp;
            this.rolledBackState = rolledBackState;
            this.currentState = currentState;
            this.durationMs = durationMs;
            this.successful = successful;
        }

        public String getEntityId() {
            return entityId;
        }

        public Instant getTargetTimestamp() {
            return targetTimestamp;
        }

        public Optional<EntityState> getRolledBackState() {
            return rolledBackState;
        }

        public Optional<EntityState> getCurrentState() {
            return currentState;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public boolean isSuccessful() {
            return successful;
        }

        @Override
        public String toString() {
            return "RollbackResult{" +
                    "entityId='" + entityId + '\'' +
                    ", targetTimestamp=" + targetTimestamp +
                    ", successful=" + successful +
                    ", durationMs=" + durationMs +
                    ", hasRolledBackState=" + rolledBackState.isPresent() +
                    ", hasCurrentState=" + currentState.isPresent() +
                    '}';
        }
    }

    /**
     * Rollback performance statistics
     */
    public static class RollbackStatistics {
        private final long totalSnapshots;
        private final int entitiesWithSnapshots;
        private final int totalEntities;

        public RollbackStatistics(long totalSnapshots, int entitiesWithSnapshots, int totalEntities) {
            this.totalSnapshots = totalSnapshots;
            this.entitiesWithSnapshots = entitiesWithSnapshots;
            this.totalEntities = totalEntities;
        }

        public long getTotalSnapshots() {
            return totalSnapshots;
        }

        public int getEntitiesWithSnapshots() {
            return entitiesWithSnapshots;
        }

        public int getTotalEntities() {
            return totalEntities;
        }

        public double getSnapshotCoverage() {
            return totalEntities > 0 ? (double) entitiesWithSnapshots / totalEntities : 0.0;
        }

        public double getAverageSnapshotsPerEntity() {
            return entitiesWithSnapshots > 0 ? (double) totalSnapshots / entitiesWithSnapshots : 0.0;
        }

        @Override
        public String toString() {
            return "RollbackStatistics{" +
                    "totalSnapshots=" + totalSnapshots +
                    ", entitiesWithSnapshots=" + entitiesWithSnapshots +
                    ", totalEntities=" + totalEntities +
                    ", snapshotCoverage=" + String.format("%.2f%%", getSnapshotCoverage() * 100) +
                    ", avgSnapshotsPerEntity=" + String.format("%.2f", getAverageSnapshotsPerEntity()) +
                    '}';
        }
    }
}