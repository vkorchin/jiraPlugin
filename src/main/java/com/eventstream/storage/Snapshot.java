package com.eventstream.storage;

import com.eventstream.core.Entity;
import java.time.Instant;
import java.util.Objects;

/**
 * Снимок состояния сущности в определенный момент времени
 */
public class Snapshot {
    private final String entityId;
    private final Entity entity;
    private final Instant timestamp;
    private final long eventSequenceNumber;
    
    public Snapshot(String entityId, Entity entity, Instant timestamp, long eventSequenceNumber) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.entity = Objects.requireNonNull(entity, "entity cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.eventSequenceNumber = eventSequenceNumber;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public long getEventSequenceNumber() {
        return eventSequenceNumber;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snapshot snapshot = (Snapshot) o;
        return eventSequenceNumber == snapshot.eventSequenceNumber &&
                Objects.equals(entityId, snapshot.entityId) &&
                Objects.equals(timestamp, snapshot.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(entityId, timestamp, eventSequenceNumber);
    }
    
    @Override
    public String toString() {
        return "Snapshot{" +
                "entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                ", eventSequenceNumber=" + eventSequenceNumber +
                '}';
    }
}