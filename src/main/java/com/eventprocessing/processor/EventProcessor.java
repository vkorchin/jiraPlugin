package com.eventprocessing.processor;

import com.eventprocessing.handler.EventHandler;
import com.eventprocessing.handler.EventHandlerRegistry;
import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;
import com.eventprocessing.model.Snapshot;
import com.eventprocessing.snapshot.SnapshotManager;
import com.eventprocessing.state.StateManager;
import com.eventprocessing.stream.EventStream;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Основной процессор событий
 * Объединяет все компоненты системы для обработки событий и управления состояниями
 */
public class EventProcessor {
    
    public final StateManager stateManager;
    public final SnapshotManager snapshotManager;
    public final EventHandlerRegistry handlerRegistry;
    private final ExecutorService executorService;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong processedEventCount = new AtomicLong(0);
    
    // Метрики производительности
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong rollbackCount = new AtomicLong(0);
    private final AtomicLong rollbackTime = new AtomicLong(0);
    
    public EventProcessor() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public EventProcessor(int threadPoolSize) {
        this.stateManager = new StateManager();
        this.snapshotManager = new SnapshotManager();
        this.handlerRegistry = new EventHandlerRegistry();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }
    
    public EventProcessor(StateManager stateManager, SnapshotManager snapshotManager, 
                         EventHandlerRegistry handlerRegistry, int threadPoolSize) {
        this.stateManager = stateManager;
        this.snapshotManager = snapshotManager;
        this.handlerRegistry = handlerRegistry;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }
    
    /**
     * Запускает процессор и подключается к потоку событий
     */
    public void start(EventStream eventStream) {
        if (running.compareAndSet(false, true)) {
            eventStream.subscribe(this::processEvent);
        }
    }
    
    /**
     * Останавливает процессор
     */
    public void stop() {
        running.set(false);
        executorService.shutdown();
    }
    
    /**
     * Регистрирует обработчик события
     */
    public void registerEventHandler(EventHandler handler) {
        handlerRegistry.registerHandler(handler);
    }
    
    /**
     * Обрабатывает одно событие
     */
    private void processEvent(Event event) {
        if (!running.get()) {
            return;
        }
        
        long startTime = System.nanoTime();
        
        try {
            EventHandler handler = handlerRegistry.getHandler(event);
            if (handler == null) {
                System.err.println("No handler found for event type: " + event.getEventType());
                return;
            }
            
            // Получаем текущее состояние сущности
            Entity currentEntity = stateManager.getCurrentState(event.getEntityId());
            
            // Обрабатываем событие
            Entity newEntity = handler.handle(event, currentEntity);
            
            // Обновляем состояние
            stateManager.updateEntityState(event.getEntityId(), newEntity, event);
            
            // Проверяем, нужно ли создать snapshot
            if (snapshotManager.shouldCreateSnapshot(event.getEntityId())) {
                snapshotManager.createSnapshot(event.getEntityId(), newEntity, 
                                             stateManager.getTotalEventCount());
            }
            
            processedEventCount.incrementAndGet();
            
        } catch (Exception e) {
            System.err.println("Error processing event " + event + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            long processingTime = System.nanoTime() - startTime;
            totalProcessingTime.addAndGet(processingTime);
        }
    }
    
    /**
     * Получает текущее состояние сущности
     */
    public Entity getCurrentState(String entityId) {
        return stateManager.getCurrentState(entityId);
    }
    
    /**
     * Выполняет откат состояния сущности к указанному времени
     * Использует оптимизацию с snapshot'ами для быстрого отката
     */
    public CompletableFuture<Entity> rollbackToTime(String entityId, Instant timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                rollbackCount.incrementAndGet();
                
                // Ищем ближайший snapshot до указанного времени
                Snapshot nearestSnapshot = snapshotManager.findNearestSnapshotBefore(entityId, timestamp);
                
                Entity baseEntity;
                Instant startFrom;
                
                if (nearestSnapshot != null) {
                    // Начинаем с snapshot'а
                    baseEntity = nearestSnapshot.getEntityState();
                    startFrom = nearestSnapshot.getTimestamp();
                } else {
                    // Начинаем с пустой сущности
                    baseEntity = null;
                    startFrom = Instant.EPOCH;
                }
                
                // Получаем события от startFrom до timestamp
                List<Event> eventsToApply = stateManager.getEventsInRange(entityId, startFrom, timestamp);
                
                // Применяем события по порядку
                Entity resultEntity = baseEntity;
                for (Event event : eventsToApply) {
                    // Пропускаем события, которые уже учтены в snapshot
                    if (nearestSnapshot != null && !event.getTimestamp().isAfter(nearestSnapshot.getTimestamp())) {
                        continue;
                    }
                    
                    EventHandler handler = handlerRegistry.getHandler(event);
                    if (handler != null) {
                        resultEntity = handler.handle(event, resultEntity);
                    }
                }
                
                return resultEntity;
                
            } finally {
                long rollbackDuration = System.nanoTime() - startTime;
                rollbackTime.addAndGet(rollbackDuration);
            }
            
        }, executorService);
    }
    
    /**
     * Выполняет быстрый откат используя только snapshot'ы (менее точный, но очень быстрый)
     */
    public Entity fastRollbackToTime(String entityId, Instant timestamp) {
        Snapshot snapshot = snapshotManager.findNearestSnapshotBefore(entityId, timestamp);
        return snapshot != null ? snapshot.getEntityState() : null;
    }
    
    /**
     * Получает историю событий для сущности в указанном диапазоне
     */
    public List<Event> getEventHistory(String entityId, Instant fromTime, Instant toTime) {
        return stateManager.getEventsInRange(entityId, fromTime, toTime);
    }
    
    /**
     * Получает статистику производительности
     */
    public ProcessingStats getStats() {
        return new ProcessingStats(
            processedEventCount.get(),
            stateManager.getEntityCount(),
            snapshotManager.getTotalSnapshotCount(),
            rollbackCount.get(),
            getAverageProcessingTime(),
            getAverageRollbackTime()
        );
    }
    
    private double getAverageProcessingTime() {
        long count = processedEventCount.get();
        return count > 0 ? (double) totalProcessingTime.get() / count / 1_000_000.0 : 0.0; // в миллисекундах
    }
    
    private double getAverageRollbackTime() {
        long count = rollbackCount.get();
        return count > 0 ? (double) rollbackTime.get() / count / 1_000_000.0 : 0.0; // в миллисекундах
    }
    
    /**
     * Проверяет, запущен ли процессор
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Класс для хранения статистики производительности
     */
    public static class ProcessingStats {
        public final long processedEvents;
        public final int entityCount;
        public final int snapshotCount;
        public final long rollbackCount;
        public final double avgProcessingTimeMs;
        public final double avgRollbackTimeMs;
        
        public ProcessingStats(long processedEvents, int entityCount, int snapshotCount, 
                             long rollbackCount, double avgProcessingTimeMs, double avgRollbackTimeMs) {
            this.processedEvents = processedEvents;
            this.entityCount = entityCount;
            this.snapshotCount = snapshotCount;
            this.rollbackCount = rollbackCount;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
            this.avgRollbackTimeMs = avgRollbackTimeMs;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ProcessingStats{processedEvents=%d, entityCount=%d, snapshotCount=%d, " +
                "rollbackCount=%d, avgProcessingTime=%.2fms, avgRollbackTime=%.2fms}",
                processedEvents, entityCount, snapshotCount, rollbackCount, 
                avgProcessingTimeMs, avgRollbackTimeMs
            );
        }
    }
}