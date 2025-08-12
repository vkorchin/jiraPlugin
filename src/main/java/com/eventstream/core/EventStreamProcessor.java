package com.eventstream.core;

import com.eventstream.handlers.EventHandler;
import com.eventstream.storage.EventStore;
import com.eventstream.storage.SnapshotManager;
import com.eventstream.storage.Snapshot;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Основной процессор для обработки потока событий с поддержкой отката состояний
 */
public class EventStreamProcessor {
    private final EventStore eventStore;
    private final SnapshotManager snapshotManager;
    private final ConcurrentHashMap<String, Entity> currentStates;
    private final ConcurrentHashMap<String, ReadWriteLock> entityLocks;
    private final ConcurrentLinkedQueue<EventHandler> eventHandlers;
    private final ExecutorService processingExecutor;
    private final AtomicLong sequenceNumber;
    
    public EventStreamProcessor(int snapshotFrequency, int threadPoolSize) {
        this.eventStore = new EventStore();
        this.snapshotManager = new SnapshotManager(snapshotFrequency);
        this.currentStates = new ConcurrentHashMap<>();
        this.entityLocks = new ConcurrentHashMap<>();
        this.eventHandlers = new ConcurrentLinkedQueue<>();
        this.processingExecutor = Executors.newFixedThreadPool(threadPoolSize);
        this.sequenceNumber = new AtomicLong(0);
    }
    
    /**
     * Регистрирует обработчик событий
     */
    public void registerEventHandler(EventHandler handler) {
        eventHandlers.add(handler);
    }
    
    /**
     * Обрабатывает входящее событие
     */
    public CompletableFuture<Void> processEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            String entityId = event.getEntityId();
            
            // Получаем или создаем блокировку для сущности
            ReadWriteLock entityLock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
            
            // Блокируем на запись для обработки события
            entityLock.writeLock().lock();
            try {
                // Получаем текущее состояние сущности
                Entity currentEntity = currentStates.get(entityId);
                
                // Обрабатываем событие через подходящий обработчик
                Entity updatedEntity = processEventWithHandler(event, currentEntity);
                
                // Обновляем состояние
                currentStates.put(entityId, updatedEntity);
                
                // Сохраняем событие
                eventStore.addEvent(event);
                
                // Создаем снимок при необходимости
                snapshotManager.createSnapshot(entityId, updatedEntity, event);
                
            } finally {
                entityLock.writeLock().unlock();
            }
        }, processingExecutor);
    }
    
    /**
     * Обрабатывает событие через подходящий обработчик
     */
    private Entity processEventWithHandler(Event event, Entity currentEntity) {
        for (EventHandler handler : eventHandlers) {
            if (handler.canHandle(event.getEventType())) {
                return handler.handle(event, currentEntity);
            }
        }
        
        // Если не найден подходящий обработчик, используем обработчик по умолчанию
        EventHandler defaultHandler = new DefaultEventHandler(event.getEventType());
        return defaultHandler.handle(event, currentEntity);
    }
    
    /**
     * Получает состояние сущности на указанный момент времени
     */
    public CompletableFuture<Entity> getEntityStateAt(String entityId, Instant timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            ReadWriteLock entityLock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
            
            // Блокируем на чтение
            entityLock.readLock().lock();
            try {
                return reconstructEntityState(entityId, timestamp);
            } finally {
                entityLock.readLock().unlock();
            }
        }, processingExecutor);
    }
    
    /**
     * Восстанавливает состояние сущности на указанный момент времени
     */
    private Entity reconstructEntityState(String entityId, Instant timestamp) {
        // Сначала пытаемся найти ближайший снимок
        Optional<Snapshot> snapshotOpt = snapshotManager.findNearestSnapshot(entityId, timestamp);
        
        if (snapshotOpt.isPresent()) {
            Snapshot snapshot = snapshotOpt.get();
            Entity baseEntity = snapshot.getEntity();
            
            // Применяем события после снимка до указанного времени
            List<Event> eventsAfterSnapshot = eventStore.getEventsInRange(
                entityId, 
                snapshot.getTimestamp(), 
                timestamp
            );
            
            return applyEventsToEntity(baseEntity, eventsAfterSnapshot);
        } else {
            // Если снимков нет, применяем все события с начала
            List<Event> allEvents = eventStore.getEventsUntil(entityId, timestamp);
            Entity initialEntity = new Entity(entityId);
            return applyEventsToEntity(initialEntity, allEvents);
        }
    }
    
    /**
     * Применяет список событий к сущности
     */
    private Entity applyEventsToEntity(Entity entity, List<Event> events) {
        Entity currentEntity = entity;
        for (Event event : events) {
            currentEntity = processEventWithHandler(event, currentEntity);
        }
        return currentEntity;
    }
    
    /**
     * Получает текущее состояние сущности
     */
    public Entity getCurrentEntityState(String entityId) {
        return currentStates.get(entityId);
    }
    
    /**
     * Создает событие с автоматически генерируемым sequence number
     */
    public Event createEvent(String entityId, Instant timestamp, String eventType, java.util.Map<String, Object> payload) {
        return new Event(entityId, timestamp, eventType, payload, sequenceNumber.incrementAndGet());
    }
    
    /**
     * Очищает старые данные для освобождения памяти
     */
    public void cleanupOldData(Instant cutoffTime) {
        currentStates.keySet().forEach(entityId -> {
            eventStore.cleanupOldEvents(entityId, cutoffTime);
            snapshotManager.cleanupOldSnapshots(entityId, cutoffTime);
        });
    }
    
    /**
     * Получает статистику системы
     */
    public SystemStats getSystemStats() {
        return new SystemStats(
            currentStates.size(),
            eventStore.getTotalEventCount(),
            snapshotManager.getSnapshotCount()
        );
    }
    
    /**
     * Завершает работу процессора
     */
    public void shutdown() {
        processingExecutor.shutdown();
    }
    
    /**
     * Статистика системы
     */
    public static class SystemStats {
        private final int activeEntities;
        private final long totalEvents;
        private final long totalSnapshots;
        
        public SystemStats(int activeEntities, long totalEvents, long totalSnapshots) {
            this.activeEntities = activeEntities;
            this.totalEvents = totalEvents;
            this.totalSnapshots = totalSnapshots;
        }
        
        public int getActiveEntities() { return activeEntities; }
        public long getTotalEvents() { return totalEvents; }
        public long getTotalSnapshots() { return totalSnapshots; }
        
        @Override
        public String toString() {
            return String.format("SystemStats{activeEntities=%d, totalEvents=%d, totalSnapshots=%d}", 
                activeEntities, totalEvents, totalSnapshots);
        }
    }
}