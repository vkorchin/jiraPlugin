package com.eventprocessing.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Базовая модель события в системе
 */
public class Event {
    private final String entityId;
    private final Instant timestamp;
    private final String eventType;
    private final Map<String, Object> payload;

    public Event(String entityId, Instant timestamp, String eventType, Map<String, Object> payload) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.payload = Map.copyOf(payload != null ? payload : Map.of());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(entityId, event.entityId) &&
                Objects.equals(timestamp, event.timestamp) &&
                Objects.equals(eventType, event.eventType) &&
                Objects.equals(payload, event.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, timestamp, eventType, payload);
    }

    @Override
    public String toString() {
        return "Event{" +
                "entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", payload=" + payload +
                '}';
    }
}