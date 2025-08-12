package com.eventprocessing.snapshot;

import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Snapshot;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Менеджер снимков состояний сущностей
 * Обеспечивает оптимизацию откатов через создание и управление snapshot'ами
 */
public class SnapshotManager {
    
    // Снимки для каждой сущности, отсортированные по времени
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, Snapshot>> snapshots = new ConcurrentHashMap<>();
    
    // Счетчик событий для каждой сущности для определения когда создавать snapshot
    private final ConcurrentHashMap<String, AtomicLong> entityEventCounters = new ConcurrentHashMap<>();
    
    // Блокировки для каждой сущности
    private final ConcurrentHashMap<String, ReadWriteLock> entityLocks = new ConcurrentHashMap<>();
    
    // Конфигурация
    private final int snapshotInterval; // Создавать snapshot каждые N событий
    private final int maxSnapshotsPerEntity; // Максимальное количество snapshot'ов на сущность
    
    public SnapshotManager() {
        this(100, 10); // По умолчанию: snapshot каждые 100 событий, максимум 10 snapshot'ов
    }
    
    public SnapshotManager(int snapshotInterval, int maxSnapshotsPerEntity) {
        this.snapshotInterval = snapshotInterval;
        this.maxSnapshotsPerEntity = maxSnapshotsPerEntity;
    }
    
    /**
     * Проверяет, нужно ли создать snapshot для сущности
     */
    public boolean shouldCreateSnapshot(String entityId) {
        AtomicLong counter = entityEventCounters.computeIfAbsent(entityId, k -> new AtomicLong(0));
        long currentCount = counter.incrementAndGet();
        return currentCount % snapshotInterval == 0;
    }
    
    /**
     * Создает snapshot состояния сущности
     */
    public void createSnapshot(String entityId, Entity entityState, long eventSequenceNumber) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            Instant now = Instant.now();
            Snapshot snapshot = new Snapshot(entityId, now, entityState, eventSequenceNumber);
            
            ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = 
                snapshots.computeIfAbsent(entityId, k -> new ConcurrentSkipListMap<>());
            
            entitySnapshots.put(now, snapshot);
            
            // Очищаем старые snapshot'ы если их слишком много
            cleanupOldSnapshots(entityId, entitySnapshots);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Находит ближайший snapshot до указанного времени
     */
    public Snapshot findNearestSnapshotBefore(String entityId, Instant timestamp) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshots.get(entityId);
            if (entitySnapshots == null || entitySnapshots.isEmpty()) {
                return null;
            }
            
            // Находим ближайший snapshot до указанного времени
            var entry = entitySnapshots.floorEntry(timestamp);
            return entry != null ? entry.getValue() : null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Получает последний snapshot для сущности
     */
    public Snapshot getLatestSnapshot(String entityId) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshots.get(entityId);
            if (entitySnapshots == null || entitySnapshots.isEmpty()) {
                return null;
            }
            return entitySnapshots.lastEntry().getValue();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Получает количество snapshot'ов для сущности
     */
    public int getSnapshotCount(String entityId) {
        ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots = snapshots.get(entityId);
        return entitySnapshots != null ? entitySnapshots.size() : 0;
    }
    
    /**
     * Получает общее количество snapshot'ов в системе
     */
    public int getTotalSnapshotCount() {
        return snapshots.values().stream()
                       .mapToInt(ConcurrentSkipListMap::size)
                       .sum();
    }
    
    /**
     * Очищает старые snapshot'ы для сущности
     */
    private void cleanupOldSnapshots(String entityId, ConcurrentSkipListMap<Instant, Snapshot> entitySnapshots) {
        while (entitySnapshots.size() > maxSnapshotsPerEntity) {
            entitySnapshots.pollFirstEntry(); // Удаляем самый старый snapshot
        }
    }
    
    /**
     * Очищает все snapshot'ы старше указанного времени для всех сущностей
     */
    public void cleanupSnapshotsOlderThan(Instant timestamp) {
        snapshots.forEach((entityId, entitySnapshots) -> {
            ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
            lock.writeLock().lock();
            try {
                entitySnapshots.headMap(timestamp, false).clear();
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Очищает все snapshot'ы для указанной сущности
     */
    public void clearSnapshotsForEntity(String entityId) {
        ReadWriteLock lock = entityLocks.computeIfAbsent(entityId, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            snapshots.remove(entityId);
            entityEventCounters.remove(entityId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Получает настройки менеджера snapshot'ов
     */
    public int getSnapshotInterval() {
        return snapshotInterval;
    }
    
    public int getMaxSnapshotsPerEntity() {
        return maxSnapshotsPerEntity;
    }
}