package com.eventprocessing.stream;

import com.eventprocessing.model.Event;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * In-memory реализация EventStream для тестирования и демонстрации
 * Поддерживает как внешнюю подачу событий, так и автогенерацию
 */
public class InMemoryEventStream implements EventStream {
    
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Consumer<Event> eventConsumer;
    
    // Для автогенерации событий
    private final boolean autoGenerate;
    private final Random random = new Random();
    private final String[] eventTypes = {"CREATE", "UPDATE", "DELETE", "STATUS_CHANGE"};
    private final String[] entityIds = {"entity1", "entity2", "entity3", "entity4", "entity5"};
    
    public InMemoryEventStream() {
        this(false);
    }
    
    public InMemoryEventStream(boolean autoGenerate) {
        this.autoGenerate = autoGenerate;
    }
    
    @Override
    public void subscribe(Consumer<Event> eventConsumer) {
        this.eventConsumer = eventConsumer;
        if (running.compareAndSet(false, true)) {
            executor.submit(this::processEvents);
            if (autoGenerate) {
                executor.submit(this::generateEvents);
            }
        }
    }
    
    @Override
    public void stop() {
        running.set(false);
        executor.shutdown();
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Добавляет событие в поток извне
     */
    public void pushEvent(Event event) {
        if (running.get()) {
            eventQueue.offer(event);
        }
    }
    
    /**
     * Добавляет событие в поток с указанными параметрами
     */
    public void pushEvent(String entityId, String eventType, Map<String, Object> payload) {
        pushEvent(new Event(entityId, Instant.now(), eventType, payload));
    }
    
    private void processEvents() {
        while (running.get()) {
            try {
                Event event = eventQueue.take();
                if (eventConsumer != null) {
                    eventConsumer.accept(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Логируем ошибку и продолжаем работу
                System.err.println("Error processing event: " + e.getMessage());
            }
        }
    }
    
    private void generateEvents() {
        while (running.get()) {
            try {
                // Генерируем событие каждые 10-100 мс
                Thread.sleep(10 + random.nextInt(90));
                
                String entityId = entityIds[random.nextInt(entityIds.length)];
                String eventType = eventTypes[random.nextInt(eventTypes.length)];
                
                Map<String, Object> payload = Map.of(
                    "value", random.nextInt(1000),
                    "description", "Auto-generated event",
                    "randomField", "data_" + random.nextInt(100)
                );
                
                Event event = new Event(entityId, Instant.now(), eventType, payload);
                eventQueue.offer(event);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}