package com.eventprocessing.state;

import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Менеджер состояний сущностей
 * Обеспечивает потокобезопасное управление текущими состояниями и историей событий
 */
public class StateManager {
    
    // Текущие состояния сущностей
    private final ConcurrentHashMap<String, Entity> currentStates = new ConcurrentHashMap<>();
    
    // История событий для каждой сущности, отсортированная по времени
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, Event>> eventHistory = new ConcurrentHashMap<>();
    
    // Счетчик последовательности событий
    private final AtomicLong eventSequenceNumber = new AtomicLong(0);
    
    // Блокировки для каждой сущности для обеспечения консистентности при откатах
    private final ConcurrentHashMap<String, ReadWriteLock> entityLocks = new ConcurrentHashMap<>();
    
    /**
     * Обновляет состояние сущности новым событием
     */
    public void updateEntityState(String entityId, Entity newState, Event event) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            // Обновляем текущее состояние
            currentStates.put(entityId, newState);
            
            // Добавляем событие в историю
            eventHistory.computeIfAbsent(entityId, k -> new ConcurrentSkipListMap<>())
                      .put(event.getTimestamp(), event);
            
            eventSequenceNumber.incrementAndGet();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Получает текущее состояние сущности
     */
    public Entity getCurrentState(String entityId) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            Entity entity = currentStates.get(entityId);
            return entity != null ? entity.copy() : null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Получает состояние сущности на определенный момент времени
     * Этот метод возвращает список событий, которые нужно применить для восстановления состояния
     */
    public List<Event> getEventsForStateAtTime(String entityId, Instant timestamp) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            ConcurrentSkipListMap<Instant, Event> entityEvents = eventHistory.get(entityId);
            if (entityEvents == null || entityEvents.isEmpty()) {
                return List.of();
            }
            
            // Получаем все события до указанного времени включительно
            return entityEvents.headMap(timestamp, true)
                             .values()
                             .stream()
                             .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Получает все события для сущности в указанном диапазоне времени
     */
    public List<Event> getEventsInRange(String entityId, Instant fromTime, Instant toTime) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            ConcurrentSkipListMap<Instant, Event> entityEvents = eventHistory.get(entityId);
            if (entityEvents == null) {
                return List.of();
            }
            
            return entityEvents.subMap(fromTime, true, toTime, true)
                             .values()
                             .stream()
                             .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Получает все события для сущности до указанного времени
     */
    public List<Event> getEventsUntil(String entityId, Instant timestamp) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            ConcurrentSkipListMap<Instant, Event> entityEvents = eventHistory.get(entityId);
            if (entityEvents == null) {
                return List.of();
            }
            
            return entityEvents.headMap(timestamp, true)
                             .values()
                             .stream()
                             .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Получает последнее событие для сущности
     */
    public Event getLastEvent(String entityId) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            ConcurrentSkipListMap<Instant, Event> entityEvents = eventHistory.get(entityId);
            if (entityEvents == null || entityEvents.isEmpty()) {
                return null;
            }
            return entityEvents.lastEntry().getValue();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Проверяет, существует ли сущность
     */
    public boolean entityExists(String entityId) {
        return currentStates.containsKey(entityId);
    }
    
    /**
     * Получает количество событий для сущности
     */
    public int getEventCount(String entityId) {
        ConcurrentSkipListMap<Instant, Event> entityEvents = eventHistory.get(entityId);
        return entityEvents != null ? entityEvents.size() : 0;
    }
    
    /**
     * Получает общее количество событий в системе
     */
    public long getTotalEventCount() {
        return eventSequenceNumber.get();
    }
    
    /**
     * Получает количество сущностей в системе
     */
    public int getEntityCount() {
        return currentStates.size();
    }
    
    /**
     * Очищает историю событий для сущности старше указанного времени
     * Используется для управления памятью
     */
    public void cleanupOldEvents(String entityId, Instant olderThan) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            ConcurrentSkipListMap<Instant, Event> entityEvents = eventHistory.get(entityId);
            if (entityEvents != null) {
                entityEvents.headMap(olderThan, false).clear();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}