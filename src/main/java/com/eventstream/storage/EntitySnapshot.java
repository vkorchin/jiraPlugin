package com.eventstream.storage;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Снимок состояния сущности в определенный момент времени
 * Используется для оптимизации откатов состояний
 */
public class EntitySnapshot {
    private final String entityId;
    private final Entity entity;
    private final Instant timestamp;
    private final long eventSequence;
    private final List<Event> eventsAfterSnapshot;
    
    public EntitySnapshot(String entityId, Entity entity, Instant timestamp, long eventSequence) {
        this.entityId = entityId;
        this.entity = entity.copy(); // Создаем копию для безопасности
        this.timestamp = timestamp;
        this.eventSequence = eventSequence;
        this.eventsAfterSnapshot = new CopyOnWriteArrayList<>();
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
    
    public long getEventSequence() {
        return eventSequence;
    }
    
    public List<Event> getEventsAfterSnapshot() {
        return new CopyOnWriteArrayList<>(eventsAfterSnapshot);
    }
    
    public void addEventAfterSnapshot(Event event) {
        eventsAfterSnapshot.add(event);
    }
    
    /**
     * Проверяет, содержит ли снимок события после указанного времени
     */
    public boolean hasEventsAfter(Instant timestamp) {
        return eventsAfterSnapshot.stream()
                .anyMatch(event -> event.getTimestamp().isAfter(timestamp));
    }
    
    /**
     * Возвращает события после указанного времени
     */
    public List<Event> getEventsAfter(Instant timestamp) {
        return eventsAfterSnapshot.stream()
                .filter(event -> event.getTimestamp().isAfter(timestamp))
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public String toString() {
        return "EntitySnapshot{" +
                "entityId='" + entityId + '\'' +
                ", timestamp=" + timestamp +
                ", eventSequence=" + eventSequence +
                ", eventsAfterCount=" + eventsAfterSnapshot.size() +
                '}';
    }
}