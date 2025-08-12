package com.eventstream.storage;

import com.eventstream.core.Event;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Хранилище событий с поддержкой быстрого поиска по времени
 * Использует ConcurrentSkipListMap для эффективного поиска по временным меткам
 */
public class EventStore {
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, Event>> eventsByEntity;
    private final ConcurrentSkipListMap<Instant, Event> globalEventTimeline;
    private final AtomicLong globalSequenceCounter;
    
    public EventStore() {
        this.eventsByEntity = new ConcurrentHashMap<>();
        this.globalEventTimeline = new ConcurrentSkipListMap<>();
        this.globalSequenceCounter = new AtomicLong(0);
    }
    
    /**
     * Сохраняет событие в хранилище
     */
    public void storeEvent(Event event) {
        String entityId = event.getEntityId();
        
        // Сохраняем в хронологическом порядке для сущности
        ConcurrentSkipListMap<Instant, Event> entityEvents = 
            eventsByEntity.computeIfAbsent(entityId, k -> new ConcurrentSkipListMap<>());
        entityEvents.put(event.getTimestamp(), event);
        
        // Сохраняем в глобальной временной шкале
        globalEventTimeline.put(event.getTimestamp(), event);
    }
    
    /**
     * Возвращает события для сущности в указанном временном диапазоне
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
     * Возвращает события для сущности до указанного времени
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
     * Возвращает события для сущности после указанного времени
     */
    public List<Event> getEventsAfter(String entityId, Instant after) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null) {
            return List.of();
        }
        
        return entityEvents.tailMap(after, false)
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Возвращает все события для сущности
     */
    public List<Event> getAllEvents(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null) {
            return List.of();
        }
        
        return entityEvents.values().stream().collect(Collectors.toList());
    }
    
    /**
     * Возвращает последнее событие для сущности
     */
    public Event getLastEvent(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null || entityEvents.isEmpty()) {
            return null;
        }
        
        return entityEvents.lastEntry().getValue();
    }
    
    /**
     * Возвращает первое событие для сущности
     */
    public Event getFirstEvent(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        if (entityEvents == null || entityEvents.isEmpty()) {
            return null;
        }
        
        return entityEvents.firstEntry().getValue();
    }
    
    /**
     * Возвращает количество событий для сущности
     */
    public long getEventCount(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventsByEntity.get(entityId);
        return entityEvents != null ? entityEvents.size() : 0;
    }
    
    /**
     * Возвращает общее количество событий
     */
    public long getTotalEventCount() {
        return globalEventTimeline.size();
    }
    
    /**
     * Возвращает следующий глобальный номер последовательности
     */
    public long getNextSequenceNumber() {
        return globalSequenceCounter.incrementAndGet();
    }
    
    /**
     * Очищает старые события для экономии памяти
     */
    public void cleanupOldEvents(Instant cutoffTime) {
        eventsByEntity.forEach((entityId, events) -> {
            events.entrySet().removeIf(entry -> 
                entry.getKey().isBefore(cutoffTime));
        });
        
        globalEventTimeline.entrySet().removeIf(entry -> 
            entry.getKey().isBefore(cutoffTime));
    }
    
    /**
     * Возвращает статистику по хранилищу
     */
    public String getStatistics() {
        long totalEvents = getTotalEventCount();
        long totalEntities = eventsByEntity.size();
        
        return String.format("EventStore: %d entities, %d total events", 
                           totalEntities, totalEvents);
    }
}