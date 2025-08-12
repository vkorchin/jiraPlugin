package com.eventprocessing.rollback;

import com.eventprocessing.handler.EventHandler;
import com.eventprocessing.handler.EventHandlerRegistry;
import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;
import com.eventprocessing.model.Snapshot;
import com.eventprocessing.snapshot.SnapshotManager;
import com.eventprocessing.state.StateManager;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Сервис для выполнения откатов состояний с различными стратегиями оптимизации
 */
public class RollbackService {
    
    private final StateManager stateManager;
    private final SnapshotManager snapshotManager;
    private final EventHandlerRegistry handlerRegistry;
    private final ExecutorService executorService;
    
    public RollbackService(StateManager stateManager, SnapshotManager snapshotManager, 
                          EventHandlerRegistry handlerRegistry) {
        this.stateManager = stateManager;
        this.snapshotManager = snapshotManager;
        this.handlerRegistry = handlerRegistry;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Стратегии отката
     */
    public enum RollbackStrategy {
        /**
         * Быстрый откат - использует только ближайший snapshot
         * Самый быстрый, но менее точный
         */
        FAST_SNAPSHOT_ONLY,
        
        /**
         * Оптимизированный откат - использует snapshot + события после него
         * Баланс между скоростью и точностью
         */
        SNAPSHOT_PLUS_EVENTS,
        
        /**
         * Полный откат - перепроигрывает все события с начала
         * Самый точный, но медленный
         */
        FULL_EVENT_REPLAY
    }
    
    /**
     * Выполняет откат с указанной стратегией
     */
    public CompletableFuture<RollbackResult> rollbackToTime(String entityId, Instant timestamp, 
                                                           RollbackStrategy strategy) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                Entity result = switch (strategy) {
                    case FAST_SNAPSHOT_ONLY -> fastSnapshotRollback(entityId, timestamp);
                    case SNAPSHOT_PLUS_EVENTS -> optimizedRollback(entityId, timestamp);
                    case FULL_EVENT_REPLAY -> fullEventReplay(entityId, timestamp);
                };
                
                long duration = System.nanoTime() - startTime;
                return new RollbackResult(result, strategy, duration / 1_000_000.0, true);
                
            } catch (Exception e) {
                long duration = System.nanoTime() - startTime;
                return new RollbackResult(null, strategy, duration / 1_000_000.0, false, e.getMessage());
            }
        }, executorService);
    }
    
    /**
     * Быстрый откат используя только snapshot'ы
     */
    private Entity fastSnapshotRollback(String entityId, Instant timestamp) {
        Snapshot snapshot = snapshotManager.findNearestSnapshotBefore(entityId, timestamp);
        return snapshot != null ? snapshot.getEntityState() : null;
    }
    
    /**
     * Оптимизированный откат с использованием snapshot'а + событий
     */
    private Entity optimizedRollback(String entityId, Instant timestamp) {
        // Находим ближайший snapshot
        Snapshot nearestSnapshot = snapshotManager.findNearestSnapshotBefore(entityId, timestamp);
        
        Entity baseEntity;
        Instant startFrom;
        
        if (nearestSnapshot != null) {
            baseEntity = nearestSnapshot.getEntityState();
            startFrom = nearestSnapshot.getTimestamp();
        } else {
            baseEntity = null;
            startFrom = Instant.EPOCH;
        }
        
        // Получаем события после snapshot'а до целевого времени
        List<Event> eventsToApply = stateManager.getEventsInRange(entityId, startFrom, timestamp);
        
        // Применяем события
        return applyEventsToEntity(baseEntity, eventsToApply, nearestSnapshot);
    }
    
    /**
     * Полный откат с перепроигрыванием всех событий
     */
    private Entity fullEventReplay(String entityId, Instant timestamp) {
        // Получаем все события до указанного времени
        List<Event> allEvents = stateManager.getEventsForStateAtTime(entityId, timestamp);
        
        // Применяем все события с начала
        return applyEventsToEntity(null, allEvents, null);
    }
    
    /**
     * Применяет список событий к базовой сущности
     */
    private Entity applyEventsToEntity(Entity baseEntity, List<Event> events, Snapshot referenceSnapshot) {
        Entity currentEntity = baseEntity;
        
        for (Event event : events) {
            // Пропускаем события, которые уже учтены в snapshot
            if (referenceSnapshot != null && 
                !event.getTimestamp().isAfter(referenceSnapshot.getTimestamp())) {
                continue;
            }
            
            EventHandler handler = handlerRegistry.getHandler(event);
            if (handler != null) {
                currentEntity = handler.handle(event, currentEntity);
            }
        }
        
        return currentEntity;
    }
    
    /**
     * Выполняет откат для множества сущностей параллельно
     */
    public CompletableFuture<List<RollbackResult>> rollbackMultipleEntities(
            List<String> entityIds, Instant timestamp, RollbackStrategy strategy) {
        
        List<CompletableFuture<RollbackResult>> futures = entityIds.stream()
            .map(entityId -> rollbackToTime(entityId, timestamp, strategy))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }
    
    /**
     * Получает рекомендуемую стратегию отката на основе анализа данных
     */
    public RollbackStrategy getRecommendedStrategy(String entityId, Instant timestamp) {
        int eventCount = stateManager.getEventCount(entityId);
        int snapshotCount = snapshotManager.getSnapshotCount(entityId);
        
        // Если событий мало, используем полный откат
        if (eventCount < 100) {
            return RollbackStrategy.FULL_EVENT_REPLAY;
        }
        
        // Если snapshot'ов нет, вынуждены использовать полный откат
        if (snapshotCount == 0) {
            return RollbackStrategy.FULL_EVENT_REPLAY;
        }
        
        // Проверяем, есть ли подходящий snapshot
        Snapshot nearestSnapshot = snapshotManager.findNearestSnapshotBefore(entityId, timestamp);
        if (nearestSnapshot == null) {
            return RollbackStrategy.FULL_EVENT_REPLAY;
        }
        
        // Считаем события после snapshot'а
        List<Event> eventsAfterSnapshot = stateManager.getEventsInRange(
            entityId, nearestSnapshot.getTimestamp(), timestamp);
        
        // Если событий после snapshot'а мало, используем быстрый откат
        if (eventsAfterSnapshot.size() < 10) {
            return RollbackStrategy.FAST_SNAPSHOT_ONLY;
        }
        
        // Иначе используем оптимизированный откат
        return RollbackStrategy.SNAPSHOT_PLUS_EVENTS;
    }
    
    /**
     * Закрывает сервис откатов
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Результат операции отката
     */
    public static class RollbackResult {
        private final Entity entity;
        private final RollbackStrategy strategy;
        private final double durationMs;
        private final boolean success;
        private final String errorMessage;
        
        public RollbackResult(Entity entity, RollbackStrategy strategy, double durationMs, boolean success) {
            this(entity, strategy, durationMs, success, null);
        }
        
        public RollbackResult(Entity entity, RollbackStrategy strategy, double durationMs, 
                            boolean success, String errorMessage) {
            this.entity = entity;
            this.strategy = strategy;
            this.durationMs = durationMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public Entity getEntity() { return entity; }
        public RollbackStrategy getStrategy() { return strategy; }
        public double getDurationMs() { return durationMs; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return String.format("RollbackResult{strategy=%s, duration=%.2fms, success=%s%s}",
                strategy, durationMs, success, 
                errorMessage != null ? ", error=" + errorMessage : "");
        }
    }
}