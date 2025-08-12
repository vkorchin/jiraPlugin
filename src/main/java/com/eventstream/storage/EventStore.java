package com.eventstream.storage;

import com.eventstream.core.Event;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Хранилище событий для каждой сущности
 */
public class EventStore {
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, Event>> eventsByEntity;
    private final AtomicLong totalEventCount;
    
    public EventStore() {
        this.eventsByEntity = new ConcurrentHashMap<>();
        this.totalEventCount = new AtomicLong(0);
    }
    
    /**
     * Добавляет событие в хранилище
     */
    public void addEvent(Event event) {
        eventsByEntity
            .computeIfAbsent(event.getEntityId(), k -> new ConcurrentSkipListMap<>())
            .put(event.getTimestamp(), event);
            
        totalEventCount.incrementAndGet();
    }
    
    /**
     * Получает все события для сущности в указанном временном диапазоне
     */
    public List<Event> getEventsInRange(String entityId, Instant from, Instant to) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null) {
            return List.of();
        }
        
        return entityEvents.subMap(from, true, to, true)
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Получает все события для сущности до указанного времени
     */
    public List<Event> getEventsUntil(String entityId, Instant until) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null) {
            return List.of();
        }
        
        return entityEvents.headMap(until, true)
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Получает событие по точному времени
     */
    public Event getEventAt(String entityId, Instant timestamp) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null) {
            return null;
        }
        
        return entityEvents.get(timestamp);
    }
    
    /**
     * Получает последнее событие для сущности
     */
    public Event getLastEvent(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null || entityEvents.isEmpty()) {
            return null;
        }
        
        return entityEvents.lastEntry().getValue();
    }
    
    /**
     * Получает количество событий для сущности
     */
    public int getEventCountForEntity(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        return entityEvents != null ? entityEvents.size() : 0;
    }
    
    /**
     * Получает общее количество событий
     */
    public long getTotalEventCount() {
        return totalEventCount.get();
    }
    
    /**
     * Удаляет старые события для освобождения памяти
     */
    public void cleanupOldEvents(String entityId, Instant cutoffTime) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents != null) {
            entityEvents.headMap(cutoffTime).clear();
        }
    }
}