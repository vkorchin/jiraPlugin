package com.eventstream.snapshot;

import com.eventstream.model.EntityState;
import com.eventstream.model.Snapshot;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;

/**
 * Manager for entity snapshots with optimized rollback capabilities
 */
public class SnapshotManager {
    private static final Logger logger = Logger.getLogger(SnapshotManager.class.getName());
    
    // Map: entityId -> NavigableMap(timestamp -> Snapshot)
    private final ConcurrentMap<String, ConcurrentSkipListMap<Instant, Snapshot>> entitySnapshots = new ConcurrentHashMap<>();
    
    private final int maxSnapshotsPerEntity;
    private final AtomicLong totalSnapshots = new AtomicLong(0);

    public SnapshotManager(int maxSnapshotsPerEntity) {
        this.maxSnapshotsPerEntity = maxSnapshotsPerEntity;
    }

    public SnapshotManager() {
        this(100); // Default max snapshots per entity
    }

    /**
     * Create and store a snapshot for an entity
     * @param entityId the entity ID
     * @param state the entity state
     * @param timestamp the snapshot timestamp
     * @param eventSequenceNumber the sequence number of the last event applied
     */
    public void createSnapshot(String entityId, EntityState state, Instant timestamp, long eventSequenceNumber) {
        Snapshot snapshot = new Snapshot(entityId, state.deepCopy(), timestamp, eventSequenceNumber);
        
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshotMap = 
            entitySnapshots.computeIfAbsent(entityId, k -> new ConcurrentSkipListMap<>());
        
        entitySnapshotMap.put(timestamp, snapshot);
        totalSnapshots.incrementAndGet();
        
        // Clean up old snapshots if we exceed the limit
        cleanupOldSnapshots(entityId, entitySnapshotMap);
        
        logger.fine("Created snapshot for entity: " + entityId + " at " + timestamp);
    }

    /**
     * Find the best snapshot for rollback (closest to but not after the target timestamp)
     * @param entityId the entity ID
     * @param targetTimestamp the target timestamp for rollback
     * @return optional snapshot
     */
    public Optional<Snapshot> findBestSnapshotForRollback(String entityId, Instant targetTimestamp) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshotMap = entitySnapshots.get(entityId);
        if (entitySnapshotMap == null || entitySnapshotMap.isEmpty()) {
            return Optional.empty();
        }
        
        // Find the latest snapshot that is not after the target timestamp
        Map.Entry<Instant, Snapshot> entry = entitySnapshotMap.floorEntry(targetTimestamp);
        
        if (entry != null) {
            logger.fine("Found snapshot for entity: " + entityId + " at " + entry.getKey() + " for target: " + targetTimestamp);
            return Optional.of(entry.getValue());
        }
        
        return Optional.empty();
    }

    /**
     * Get all snapshots for an entity (for debugging/monitoring)
     * @param entityId the entity ID
     * @return navigable map of snapshots
     */
    public NavigableMap<Instant, Snapshot> getEntitySnapshots(String entityId) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshotMap = entitySnapshots.get(entityId);
        return entitySnapshotMap != null ? entitySnapshotMap.clone() : new ConcurrentSkipListMap<>();
    }

    /**
     * Get the latest snapshot for an entity
     * @param entityId the entity ID
     * @return optional latest snapshot
     */
    public Optional<Snapshot> getLatestSnapshot(String entityId) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshotMap = entitySnapshots.get(entityId);
        if (entitySnapshotMap == null || entitySnapshotMap.isEmpty()) {
            return Optional.empty();
        }
        
        Map.Entry<Instant, Snapshot> lastEntry = entitySnapshotMap.lastEntry();
        return lastEntry != null ? Optional.of(lastEntry.getValue()) : Optional.empty();
    }

    /**
     * Get the number of snapshots for an entity
     * @param entityId the entity ID
     * @return number of snapshots
     */
    public int getSnapshotCount(String entityId) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshotMap = entitySnapshots.get(entityId);
        return entitySnapshotMap != null ? entitySnapshotMap.size() : 0;
    }

    /**
     * Get the total number of snapshots across all entities
     * @return total snapshot count
     */
    public long getTotalSnapshotCount() {
        return totalSnapshots.get();
    }

    /**
     * Get the number of entities with snapshots
     * @return number of entities
     */
    public int getEntityCount() {
        return entitySnapshots.size();
    }

    /**
     * Remove all snapshots for an entity
     * @param entityId the entity ID
     * @return number of snapshots removed
     */
    public int removeEntitySnapshots(String entityId) {
        ConcurrentSkipListMap<Instant, Snapshot> removed = entitySnapshots.remove(entityId);
        if (removed != null) {
            int count = removed.size();
            totalSnapshots.addAndGet(-count);
            logger.info("Removed " + count + " snapshots for entity: " + entityId);
            return count;
        }
        return 0;
    }

    /**
     * Remove snapshots older than the specified timestamp for an entity
     * @param entityId the entity ID
     * @param cutoffTimestamp the cutoff timestamp
     * @return number of snapshots removed
     */
    public int removeOldSnapshots(String entityId, Instant cutoffTimestamp) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshotMap = entitySnapshots.get(entityId);
        if (entitySnapshotMap == null) {
            return 0;
        }
        
        int removedCount = 0;
        NavigableMap<Instant, Snapshot> oldSnapshots = entitySnapshotMap.headMap(cutoffTimestamp, false);
        
        for (Instant timestamp : oldSnapshots.keySet()) {
            if (entitySnapshotMap.remove(timestamp) != null) {
                removedCount++;
                totalSnapshots.decrementAndGet();
            }
        }
        
        if (removedCount > 0) {
            logger.info("Removed " + removedCount + " old snapshots for entity: " + entityId);
        }
        
        return removedCount;
    }

    /**
     * Clear all snapshots
     */
    public void clear() {
        entitySnapshots.clear();
        totalSnapshots.set(0);
        logger.info("Cleared all snapshots");
    }

    /**
     * Get memory usage statistics
     * @return snapshot statistics
     */
    public SnapshotStatistics getStatistics() {
        return new SnapshotStatistics(
            getTotalSnapshotCount(),
            getEntityCount(),
            maxSnapshotsPerEntity
        );
    }

    /**
     * Clean up old snapshots for an entity if we exceed the limit
     */
    private void cleanupOldSnapshots(String entityId, ConcurrentSkipListMap<Instant, Snapshot> entitySnapshotMap) {
        while (entitySnapshotMap.size() > maxSnapshotsPerEntity) {
            Map.Entry<Instant, Snapshot> oldest = entitySnapshotMap.firstEntry();
            if (oldest != null && entitySnapshotMap.remove(oldest.getKey()) != null) {
                totalSnapshots.decrementAndGet();
                logger.fine("Removed old snapshot for entity: " + entityId + " at " + oldest.getKey());
            }
        }
    }

    /**
     * Statistics about snapshots
     */
    public static class SnapshotStatistics {
        private final long totalSnapshots;
        private final int entityCount;
        private final int maxSnapshotsPerEntity;

        public SnapshotStatistics(long totalSnapshots, int entityCount, int maxSnapshotsPerEntity) {
            this.totalSnapshots = totalSnapshots;
            this.entityCount = entityCount;
            this.maxSnapshotsPerEntity = maxSnapshotsPerEntity;
        }

        public long getTotalSnapshots() {
            return totalSnapshots;
        }

        public int getEntityCount() {
            return entityCount;
        }

        public int getMaxSnapshotsPerEntity() {
            return maxSnapshotsPerEntity;
        }

        public double getAverageSnapshotsPerEntity() {
            return entityCount > 0 ? (double) totalSnapshots / entityCount : 0.0;
        }

        @Override
        public String toString() {
            return "SnapshotStatistics{" +
                    "totalSnapshots=" + totalSnapshots +
                    ", entityCount=" + entityCount +
                    ", maxSnapshotsPerEntity=" + maxSnapshotsPerEntity +
                    ", averageSnapshotsPerEntity=" + String.format("%.2f", getAverageSnapshotsPerEntity()) +
                    '}';
        }
    }
}