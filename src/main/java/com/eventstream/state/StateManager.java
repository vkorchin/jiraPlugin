package com.eventstream.state;

import com.eventstream.model.EntityState;
import com.eventstream.model.Event;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Thread-safe manager for entity states with copy-on-write semantics
 */
public class StateManager {
    private static final Logger logger = Logger.getLogger(StateManager.class.getName());
    
    private final ConcurrentMap<String, EntityState> currentStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ReadWriteLock> entityLocks = new ConcurrentHashMap<>();

    /**
     * Get current state of an entity
     * @param entityId the entity ID
     * @return optional entity state
     */
    public Optional<EntityState> getCurrentState(String entityId) {
        return Optional.ofNullable(currentStates.get(entityId));
    }

    /**
     * Update entity state atomically
     * @param entityId the entity ID
     * @param newState the new state
     * @return the previous state (if any)
     */
    public Optional<EntityState> updateState(String entityId, EntityState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("New state cannot be null");
        }
        
        ReadWriteLock lock = getEntityLock(entityId);
        lock.writeLock().lock();
        try {
            EntityState previousState = currentStates.put(entityId, newState);
            logger.fine("Updated state for entity: " + entityId + " to version: " + newState.getVersion());
            return Optional.ofNullable(previousState);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Update entity state only if current state matches expected state (CAS operation)
     * @param entityId the entity ID
     * @param expectedState the expected current state (null for non-existent)
     * @param newState the new state
     * @return true if update was successful, false otherwise
     */
    public boolean compareAndSwapState(String entityId, EntityState expectedState, EntityState newState) {
        ReadWriteLock lock = getEntityLock(entityId);
        lock.writeLock().lock();
        try {
            EntityState currentState = currentStates.get(entityId);
            
            // Check if current state matches expected state
            if ((currentState == null && expectedState == null) ||
                (currentState != null && currentState.equals(expectedState))) {
                
                if (newState != null) {
                    currentStates.put(entityId, newState);
                } else {
                    currentStates.remove(entityId);
                }
                
                logger.fine("CAS successful for entity: " + entityId);
                return true;
            }
            
            logger.fine("CAS failed for entity: " + entityId + " (state mismatch)");
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove entity state
     * @param entityId the entity ID
     * @return the removed state (if any)
     */
    public Optional<EntityState> removeState(String entityId) {
        ReadWriteLock lock = getEntityLock(entityId);
        lock.writeLock().lock();
        try {
            EntityState removedState = currentStates.remove(entityId);
            if (removedState != null) {
                logger.fine("Removed state for entity: " + entityId);
            }
            return Optional.ofNullable(removedState);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if entity exists
     * @param entityId the entity ID
     * @return true if entity exists
     */
    public boolean hasEntity(String entityId) {
        return currentStates.containsKey(entityId);
    }

    /**
     * Get the number of entities being tracked
     * @return number of entities
     */
    public int getEntityCount() {
        return currentStates.size();
    }

    /**
     * Create a snapshot of all current states
     * @param timestamp the snapshot timestamp
     * @return map of entity states
     */
    public ConcurrentMap<String, EntityState> createSnapshot(Instant timestamp) {
        ConcurrentMap<String, EntityState> snapshot = new ConcurrentHashMap<>();
        
        // We need to lock all entities to create a consistent snapshot
        // For performance, we'll create a best-effort snapshot without global locking
        // This is acceptable for most use cases where perfect consistency isn't required
        for (String entityId : currentStates.keySet()) {
            ReadWriteLock lock = getEntityLock(entityId);
            lock.readLock().lock();
            try {
                EntityState state = currentStates.get(entityId);
                if (state != null) {
                    snapshot.put(entityId, state.deepCopy());
                }
            } finally {
                lock.readLock().unlock();
            }
        }
        
        logger.info("Created snapshot with " + snapshot.size() + " entities at " + timestamp);
        return snapshot;
    }

    /**
     * Restore states from a snapshot
     * @param snapshot the snapshot to restore from
     */
    public void restoreFromSnapshot(ConcurrentMap<String, EntityState> snapshot) {
        // Clear current states and restore from snapshot
        currentStates.clear();
        currentStates.putAll(snapshot);
        
        logger.info("Restored " + snapshot.size() + " entities from snapshot");
    }

    /**
     * Clear all states
     */
    public void clear() {
        currentStates.clear();
        entityLocks.clear();
        logger.info("Cleared all entity states");
    }

    /**
     * Get or create a lock for the specific entity
     */
    private ReadWriteLock getEntityLock(String entityId) {
        return entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
    }
}