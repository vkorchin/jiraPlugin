package com.eventstream.storage;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Менеджер снимков состояний сущностей
 * Автоматически создает снимки каждые N событий для оптимизации откатов
 */
public class SnapshotManager {
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, EntitySnapshot>> snapshotsByEntity;
    private final ConcurrentHashMap<String, AtomicLong> eventCounters;
    private final int snapshotFrequency; // Каждые N событий создаем снимок
    private final int maxSnapshotsPerEntity; // Максимальное количество снимков на сущность
    
    public SnapshotManager(int snapshotFrequency, int maxSnapshotsPerEntity) {
        this.snapshotsByEntity = new ConcurrentHashMap<>();
        this.eventCounters = new ConcurrentHashMap<>();
        this.snapshotFrequency = snapshotFrequency;
        this.maxSnapshotsPerEntity = maxSnapshotsPerEntity;
    }
    
    public SnapshotManager() {
        this(100, 10); // По умолчанию: снимок каждые 100 событий, максимум 10 снимков
    }
    
    /**
     * Проверяет, нужно ли создать снимок для сущности
     */
    public boolean shouldCreateSnapshot(String entityId) {
        AtomicLong counter = eventCounters.get(entityId);
        return counter != null && counter.get() % snapshotFrequency == 0;
    }
    
    /**
     * Создает снимок состояния сущности
     */
    public void createSnapshot(String entityId, Entity entity, Event event) {
        ConcurrentSkipListMap<Instant, EntitySnapshot> entitySnapshots = 
            snapshotsByEntity.computeIfAbsent(entityId, k -> new ConcurrentSkipListMap<>());
        
        EntitySnapshot snapshot = new EntitySnapshot(
            entityId, entity, event.getTimestamp(), event.getSequenceNumber()
        );
        
        entitySnapshots.put(event.getTimestamp(), snapshot);
        
        // Ограничиваем количество снимков
        if (entitySnapshots.size() > maxSnapshotsPerEntity) {
            Instant oldestSnapshot = entitySnapshots.firstKey();
            entitySnapshots.remove(oldestSnapshot);
        }
    }
    
    /**
     * Добавляет событие к существующему снимку
     */
    public void addEventToSnapshot(String entityId, Event event) {
        ConcurrentSkipListMap<Instant, EntitySnapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        if (entitySnapshots != null) {
            // Добавляем событие к последнему снимку
            EntitySnapshot lastSnapshot = entitySnapshots.lastEntry().getValue();
            lastSnapshot.addEventAfterSnapshot(event);
        }
    }
    
    /**
     * Увеличивает счетчик событий для сущности
     */
    public void incrementEventCounter(String entityId) {
        eventCounters.computeIfAbsent(entityId, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Находит ближайший снимок к указанному времени
     */
    public EntitySnapshot findNearestSnapshot(String entityId, Instant targetTime) {
        ConcurrentSkipListMap<Instant, EntitySnapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        if (entitySnapshots == null || entitySnapshots.isEmpty()) {
            return null;
        }
        
        // Ищем снимок, который был сделан до или в указанное время
        java.util.Map.Entry<Instant, EntitySnapshot> entry = entitySnapshots.floorEntry(targetTime);
        return entry != null ? entry.getValue() : null;
    }
    
    /**
     * Возвращает все снимки для сущности
     */
    public List<EntitySnapshot> getSnapshots(String entityId) {
        ConcurrentSkipListMap<Instant, EntitySnapshot> entitySnapshots = snapshotsByEntity.get(entityId);
        if (entitySnapshots == null) {
            return List.of();
        }
        return entitySnapshots.values().stream().collect(Collectors.toList());
    }
    
    /**
     * Очищает старые снимки для экономии памяти
     */
    public void cleanupOldSnapshots(Instant cutoffTime) {
        snapshotsByEntity.forEach((entityId, snapshots) -> {
            snapshots.entrySet().removeIf(entry -> 
                entry.getKey().isBefore(cutoffTime));
        });
    }
    
    /**
     * Возвращает статистику по снимкам
     */
    public String getStatistics() {
        long totalSnapshots = snapshotsByEntity.values().stream()
            .mapToLong(map -> map.size())
            .sum();
        
        long totalEntities = snapshotsByEntity.size();
        
        return String.format("SnapshotManager: %d entities, %d total snapshots", 
                           totalEntities, totalSnapshots);
    }
}