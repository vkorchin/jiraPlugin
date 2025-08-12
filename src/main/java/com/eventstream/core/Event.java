package com.eventstream.core;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Событие в потоке событий
 */
public class Event {
    private final String entityId;
    private final Instant timestamp;
    private final String eventType;
    private final Map<String, Object> payload;
    private final long sequenceNumber;

    public Event(String entityId, Instant timestamp, String eventType, Map<String, Object> payload, long sequenceNumber) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.payload = Objects.requireNonNull(payload, "payload cannot be null");
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
                Objects.equals(eventType, event.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, timestamp, eventType, sequenceNumber);
    }

    @Override
    public String toString() {
        return "Event{" +
                "entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}