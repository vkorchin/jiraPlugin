package com.eventprocessing.model;

import java.time.Instant;

/**
 * Снимок состояния сущности в определенный момент времени
 * Используется для оптимизации откатов состояния
 */
public class Snapshot {
    private final String entityId;
    private final Instant timestamp;
    private final Entity entityState;
    private final long eventSequenceNumber;

    public Snapshot(String entityId, Instant timestamp, Entity entityState, long eventSequenceNumber) {
        this.entityId = entityId;
        this.timestamp = timestamp;
        this.entityState = entityState.copy(); // Создаем копию для изоляции
        this.eventSequenceNumber = eventSequenceNumber;
    }

    public String getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Entity getEntityState() {
        return entityState.copy(); // Возвращаем копию для безопасности
    }

    public long getEventSequenceNumber() {
        return eventSequenceNumber;
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                ", eventSequenceNumber=" + eventSequenceNumber +
                ", entityState=" + entityState +
                '}';
    }
}