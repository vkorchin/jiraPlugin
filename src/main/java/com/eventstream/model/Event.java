package com.eventstream.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable event model for thread-safe processing
 */
public final class Event {
    private final String entityId;
    private final Instant timestamp;
    private final String eventType;
    private final Map<String, Object> payload;
    private final long sequenceNumber;

    public Event(String entityId, Instant timestamp, String eventType, 
                 Map<String, Object> payload, long sequenceNumber) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.payload = payload != null ? Collections.unmodifiableMap(payload) : Collections.emptyMap();
        this.sequenceNumber = sequenceNumber;
    }

    public String getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return sequenceNumber == event.sequenceNumber &&
                Objects.equals(entityId, event.entityId) &&
                Objects.equals(timestamp, event.timestamp) &&
                Objects.equals(eventType, event.eventType) &&
                Objects.equals(payload, event.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, timestamp, eventType, payload, sequenceNumber);
    }

    @Override
    public String toString() {
        return "Event{" +
                "entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", payload=" + payload +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}