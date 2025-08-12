package com.eventstream.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of entity state at a specific point in time
 */
public final class Snapshot {
    private final String entityId;
    private final EntityState state;
    private final Instant timestamp;
    private final long eventSequenceNumber;

    public Snapshot(String entityId, EntityState state, Instant timestamp, long eventSequenceNumber) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.eventSequenceNumber = eventSequenceNumber;
    }

    public String getEntityId() {
        return entityId;
    }

    public EntityState getState() {
        return state;
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
                Objects.equals(state, snapshot.state) &&
                Objects.equals(timestamp, snapshot.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, state, timestamp, eventSequenceNumber);
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "entityId='" + entityId + '\'' +
                ", state=" + state +
                ", timestamp=" + timestamp +
                ", eventSequenceNumber=" + eventSequenceNumber +
                '}';
    }
}