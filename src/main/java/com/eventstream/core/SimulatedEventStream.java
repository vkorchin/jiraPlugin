package com.eventstream.core;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Симуляция потока событий для тестирования
 * Генерирует события с заданной частотой
 */
public class SimulatedEventStream implements EventStream {
    private final CopyOnWriteArrayList<Consumer<Event>> subscribers;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean active;
    private final AtomicLong eventCounter;
    private final ConcurrentHashMap<String, Long> entityEventCounters;
    private final int eventsPerSecond;
    
    public SimulatedEventStream(int eventsPerSecond) {
        this.subscribers = new CopyOnWriteArrayList<>();
        this.executor = Executors.newScheduledThreadPool(1);
        this.active = new AtomicBoolean(false);
        this.eventCounter = new AtomicLong(0);
        this.entityEventCounters = new ConcurrentHashMap<>();
        this.eventsPerSecond = eventsPerSecond;
    }
    
    public SimulatedEventStream() {
        this(100); // По умолчанию 100 событий в секунду
    }
    
    @Override
    public void subscribe(Consumer<Event> eventHandler) {
        subscribers.add(eventHandler);
    }
    
    @Override
    public void unsubscribe(Consumer<Event> eventHandler) {
        subscribers.remove(eventHandler);
    }
    
    @Override
    public void start() {
        if (active.compareAndSet(false, true)) {
            long intervalMs = 1000L / eventsPerSecond;
            executor.scheduleAtFixedRate(this::generateEvent, 0, intervalMs, TimeUnit.MILLISECONDS);
        }
    }
    
    @Override
    public void stop() {
        if (active.compareAndSet(true, false)) {
            executor.shutdown();
        }
    }
    
    @Override
    public boolean isActive() {
        return active.get();
    }
    
    /**
     * Генерирует событие и отправляет всем подписчикам
     */
    private void generateEvent() {
        if (!active.get()) {
            return;
        }
        
        // Генерируем случайное событие
        Event event = generateRandomEvent();
        
        // Отправляем всем подписчикам
        for (Consumer<Event> subscriber : subscribers) {
            try {
                subscriber.accept(event);
            } catch (Exception e) {
                System.err.println("Error in event subscriber: " + e.getMessage());
            }
        }
    }
    
    /**
     * Генерирует случайное событие
     */
    private Event generateRandomEvent() {
        String[] entityIds = {"user-001", "user-002", "user-003", "order-001", "order-002"};
        String[] eventTypes = {"user.created", "user.updated", "order.created", "order.paid", "order.shipped"};
        
        String entityId = entityIds[(int) (Math.random() * entityIds.length)];
        String eventType = eventTypes[(int) (Math.random() * eventTypes.length)];
        
        // Увеличиваем счетчик событий для сущности
        long entityEventCount = entityEventCounters.compute(entityId, (k, v) -> v == null ? 1 : v + 1);
        
        // Создаем payload
        Map<String, Object> payload = Map.of(
            "entityId", entityId,
            "eventType", eventType,
            "timestamp", Instant.now(),
            "sequence", entityEventCount
        );
        
        return new Event(
            entityId,
            Instant.now(),
            eventType,
            payload,
            eventCounter.incrementAndGet()
        );
    }
    
    /**
     * Генерирует событие с заданными параметрами
     */
    public void generateCustomEvent(String entityId, String eventType, Map<String, Object> payload) {
        Event event = new Event(
            entityId,
            Instant.now(),
            eventType,
            payload,
            eventCounter.incrementAndGet()
        );
        
        // Отправляем всем подписчикам
        for (Consumer<Event> subscriber : subscribers) {
            try {
                subscriber.accept(event);
            } catch (Exception e) {
                System.err.println("Error in event subscriber: " + e.getMessage());
            }
        }
    }
    
    /**
     * Возвращает количество сгенерированных событий
     */
    public long getGeneratedEventCount() {
        return eventCounter.get();
    }
}