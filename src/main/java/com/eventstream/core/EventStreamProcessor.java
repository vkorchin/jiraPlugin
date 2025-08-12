package com.eventstream.core;

import com.eventstream.handlers.EventProcessor;
import com.eventstream.storage.EventStore;
import com.eventstream.storage.SnapshotManager;
import com.eventstream.storage.EntitySnapshot;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Основной процессор потоковой обработки событий с поддержкой откатов
 * Обеспечивает потокобезопасность и высокую производительность
 */
public class EventStreamProcessor {
    private final EventStore eventStore;
    private final SnapshotManager snapshotManager;
    private final ConcurrentHashMap<String, Entity> currentEntities;
    private final CopyOnWriteArrayList<EventProcessor> eventProcessors;
    private final ExecutorService eventProcessingExecutor;
    private final AtomicLong processedEventsCount;
    private final ReadWriteLock entityLock;
    
    public EventStreamProcessor() {
        this.eventStore = new EventStore();
        this.snapshotManager = new SnapshotManager();
        this.currentEntities = new ConcurrentHashMap<>();
        this.eventProcessors = new CopyOnWriteArrayList<>();
        this.eventProcessingExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        this.processedEventsCount = new AtomicLong(0);
        this.entityLock = new ReentrantReadWriteLock();
        
        // Регистрируем обработчик по умолчанию
        registerEventProcessor(new com.eventstream.handlers.DefaultEventProcessor("default"));
    }
    
    public EventStreamProcessor(int snapshotFrequency, int maxSnapshotsPerEntity) {
        this.eventStore = new EventStore();
        this.snapshotManager = new SnapshotManager(snapshotFrequency, maxSnapshotsPerEntity);
        this.currentEntities = new ConcurrentHashMap<>();
        this.eventProcessors = new CopyOnWriteArrayList<>();
        this.eventProcessingExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        this.processedEventsCount = new AtomicLong(0);
        this.entityLock = new ReentrantReadWriteLock();
        
        // Регистрируем обработчик по умолчанию
        registerEventProcessor(new com.eventstream.handlers.DefaultEventProcessor("default"));
    }
    
    /**
     * Регистрирует новый обработчик событий
     */
    public void registerEventProcessor(EventProcessor processor) {
        eventProcessors.add(processor);
        // Сортируем по приоритету
        eventProcessors.sort((p1, p2) -> Integer.compare(p1.getPriority(), p2.getPriority()));
    }
    
    /**
     * Обрабатывает входящее событие
     */
    public void processEvent(Event event) {
        // Добавляем номер последовательности, если его нет
        if (event.getSequenceNumber() == 0) {
            event = new Event(
                event.getEntityId(),
                event.getTimestamp(),
                event.getEventType(),
                event.getPayload(),
                eventStore.getNextSequenceNumber()
            );
        }
        
        // Сохраняем событие
        eventStore.storeEvent(event);
        
        // Обрабатываем событие асинхронно
        eventProcessingExecutor.submit(() -> {
            try {
                processEventInternal(event);
                processedEventsCount.incrementAndGet();
            } catch (Exception e) {
                System.err.println("Error processing event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Внутренняя обработка события
     */
    private void processEventInternal(Event event) {
        String entityId = event.getEntityId();
        
        // Получаем или создаем сущность
        Entity entity = currentEntities.computeIfAbsent(entityId, Entity::new);
        
        // Находим подходящий обработчик
        EventProcessor processor = findProcessor(event.getEventType());
        if (processor != null) {
            processor.handle(event, entity);
        }
        
        // Обновляем счетчик событий для снимков
        snapshotManager.incrementEventCounter(entityId);
        
        // Проверяем, нужно ли создать снимок
        if (snapshotManager.shouldCreateSnapshot(entityId)) {
            snapshotManager.createSnapshot(entityId, entity, event);
        } else {
            // Добавляем событие к существующему снимку
            snapshotManager.addEventToSnapshot(entityId, event);
        }
    }
    
    /**
     * Находит подходящий обработчик для типа события
     */
    private EventProcessor findProcessor(String eventType) {
        return eventProcessors.stream()
            .filter(processor -> processor.canHandle(eventType))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Возвращает состояние сущности на указанное время
     */
    public Entity getEntityStateAt(String entityId, Instant timestamp) {
        entityLock.readLock().lock();
        try {
            // Сначала пытаемся найти ближайший снимок
            EntitySnapshot snapshot = snapshotManager.findNearestSnapshot(entityId, timestamp);
            
            if (snapshot != null) {
                // Восстанавливаем состояние из снимка
                Entity entity = snapshot.getEntity().copy();
                
                // Применяем события после снимка до указанного времени
                List<Event> eventsToApply = snapshot.getEventsAfter(timestamp);
                for (Event event : eventsToApply) {
                    if (event.getTimestamp().isBefore(timestamp) || 
                        event.getTimestamp().equals(timestamp)) {
                        EventProcessor processor = findProcessor(event.getEventType());
                        if (processor != null) {
                            processor.handle(event, entity);
                        }
                    }
                }
                
                return entity;
            } else {
                // Снимков нет, применяем все события с начала
                return reconstructEntityFromEvents(entityId, timestamp);
            }
        } finally {
            entityLock.readLock().unlock();
        }
    }
    
    /**
     * Восстанавливает состояние сущности из событий
     */
    private Entity reconstructEntityFromEvents(String entityId, Instant timestamp) {
        Entity entity = new Entity(entityId);
        List<Event> events = eventStore.getEventsUntil(entityId, timestamp);
        
        for (Event event : events) {
            EventProcessor processor = findProcessor(event.getEventType());
            if (processor != null) {
                processor.handle(event, entity);
            }
        }
        
        return entity;
    }
    
    /**
     * Возвращает текущее состояние сущности
     */
    public Entity getCurrentEntityState(String entityId) {
        return currentEntities.get(entityId);
    }
    
    /**
     * Возвращает количество обработанных событий
     */
    public long getProcessedEventsCount() {
        return processedEventsCount.get();
    }
    
    /**
     * Возвращает статистику системы
     */
    public String getStatistics() {
        return String.format("EventStreamProcessor: %d processed events, %d entities, %s, %s",
                           processedEventsCount.get(),
                           currentEntities.size(),
                           eventStore.getStatistics(),
                           snapshotManager.getStatistics());
    }
    
    /**
     * Очищает старые данные для экономии памяти
     */
    public void cleanupOldData(Instant cutoffTime) {
        eventStore.cleanupOldEvents(cutoffTime);
        snapshotManager.cleanupOldSnapshots(cutoffTime);
    }
    
    /**
     * Завершает работу процессора
     */
    public void shutdown() {
        eventProcessingExecutor.shutdown();
    }
}