package com.eventstream.storage;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Optional;

/**
 * Менеджер снимков состояний для оптимизации откатов
 */
public class SnapshotManager {
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, Snapshot>> snapshotsByEntity;
    private final AtomicLong snapshotCounter;
    private final int snapshotFrequency;
    
    public SnapshotManager(int snapshotFrequency) {
        this.snapshotsByEntity = new ConcurrentHashMap<>();
        this.snapshotCounter = new AtomicLong(0);
        this.snapshotFrequency = snapshotFrequency;
    }
    
    /**
     * Создает снимок состояния сущности
     */
    public void createSnapshot(String entityId, Entity entity, Event event) {
        if (shouldCreateSnapshot(entityId, event)) {
            Snapshot snapshot = new Snapshot(
                entityId, 
                entity.copy(), 
                event.getTimestamp(), 
                event.getSequenceNumber()
            );
            
            snapshotsByEntity
                .computeIfAbsent(entityId, k -> new ConcurrentSkipListMap<>())
                .put(event.getTimestamp(), snapshot);
                
            snapshotCounter.incrementAndGet();
        }
    }
    
    /**
     * Проверяет, нужно ли создать снимок
     */
    private boolean shouldCreateSnapshot(String entityId, Event event) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        if (entitySnapshots == null) {
            return true;
        }
        
        // Создаем снимок каждые N событий
        return event.getSequenceNumber() % snapshotFrequency == 0;
    }
    
    /**
     * Находит ближайший снимок для указанной временной точки
     */
    public Optional<Snapshot> findNearestSnapshot(String entityId, Instant targetTime) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        if (entitySnapshots == null || entitySnapshots.isEmpty()) {
            return Optional.empty();
        }
        
        // Находим снимок с временной меткой <= targetTime
        Map.Entry<Instant, Snapshot> entry = entitySnapshots.floorEntry(targetTime);
        return Optional.ofNullable(entry).map(Map.Entry::getValue);
    }
    
    /**
     * Получает снимок по точному времени
     */
    public Optional<Snapshot> getSnapshot(String entityId, Instant timestamp) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        if (entitySnapshots == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(entitySnapshots.get(timestamp));
    }
    
    /**
     * Удаляет старые снимки для освобождения памяти
     */
    public void cleanupOldSnapshots(String entityId, Instant cutoffTime) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        if (entitySnapshots != null) {
            entitySnapshots.headMap(cutoffTime).clear();
        }
    }
    
    /**
     * Получает статистику по снимкам
     */
    public long getSnapshotCount() {
        return snapshotCounter.get();
    }
    
    /**
     * Получает количество снимков для конкретной сущности
     */
    public int getSnapshotCountForEntity(String entityId) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        return entitySnapshots != null ? entitySnapshots.size() : 0;
    }
}