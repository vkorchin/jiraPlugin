package com.github.vkorchin.eventstream;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Event {
    private final String entityId;
    private final Instant timestamp;
    private final String eventType;
    private final Map<String, Object> payload;

    public Event(String entityId, Instant timestamp, String eventType, Map<String, Object> payload) {
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.payload = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(payload, "payload")));
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
    public String toString() {
        return "Event{" +
                "entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", payload=" + payload +
                '}';
    }
}