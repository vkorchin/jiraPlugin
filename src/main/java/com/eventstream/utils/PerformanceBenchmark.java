package com.eventstream.utils;

import com.eventstream.core.EventStreamProcessor;
import com.eventstream.core.Event;
import com.eventstream.handlers.UserEventHandler;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;

/**
 * Бенчмарк для тестирования производительности системы
 */
public class PerformanceBenchmark {
    
    public static void main(String[] args) {
        // Создаем процессор с частотой снимков каждые 100 событий и 4 потоками
        EventStreamProcessor processor = new EventStreamProcessor(100, 4);
        
        // Регистрируем обработчики
        processor.registerEventHandler(new UserEventHandler());
        
        System.out.println("🚀 Запуск бенчмарка производительности...");
        
        // Тест 1: Обработка событий
        benchmarkEventProcessing(processor, 10000);
        
        // Тест 2: Откат состояний
        benchmarkStateRollback(processor, 1000);
        
        // Тест 3: Нагрузочное тестирование
        benchmarkLoadTest(processor, 100000);
        
        // Выводим статистику
        System.out.println("\n📊 Финальная статистика:");
        System.out.println(processor.getSystemStats());
        
        processor.shutdown();
    }
    
    /**
     * Бенчмарк обработки событий
     */
    private static void benchmarkEventProcessing(EventStreamProcessor processor, int eventCount) {
        System.out.println("\n📝 Тест обработки " + eventCount + " событий...");
        
        long startTime = System.currentTimeMillis();
        AtomicLong processedEvents = new AtomicLong(0);
        
        // Создаем события для нескольких пользователей
        for (int i = 0; i < eventCount; i++) {
            String entityId = "user_" + (i % 100); // 100 пользователей
            String eventType = getEventType(i);
            Map<String, Object> payload = createPayload(i);
            
            Event event = processor.createEvent(entityId, Instant.now(), eventType, payload);
            
            processor.processEvent(event).thenRun(() -> {
                processedEvents.incrementAndGet();
            });
        }
        
        // Ждем завершения обработки
        waitForCompletion(processedEvents, eventCount);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double eventsPerSecond = (eventCount * 1000.0) / duration;
        
        System.out.println("✅ Обработано " + eventCount + " событий за " + duration + " мс");
        System.out.println("⚡ Производительность: " + String.format("%.2f", eventsPerSecond) + " событий/сек");
    }
    
    /**
     * Бенчмарк отката состояний
     */
    private static void benchmarkStateRollback(EventStreamProcessor processor, int rollbackCount) {
        System.out.println("\n⏪ Тест отката " + rollbackCount + " состояний...");
        
        long startTime = System.currentTimeMillis();
        AtomicLong completedRollbacks = new AtomicLong(0);
        
        // Создаем события для одного пользователя
        String entityId = "test_user";
        for (int i = 0; i < rollbackCount; i++) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("value", i);
            
            Event event = processor.createEvent(entityId, Instant.now(), "USER_UPDATED", payload);
            processor.processEvent(event);
        }
        
        // Тестируем откаты на разные временные точки
        for (int i = 0; i < rollbackCount; i++) {
            Instant targetTime = Instant.now().minusSeconds(i);
            
            processor.getEntityStateAt(entityId, targetTime).thenRun(() -> {
                completedRollbacks.incrementAndGet();
            });
        }
        
        // Ждем завершения откатов
        waitForCompletion(completedRollbacks, rollbackCount);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double rollbacksPerSecond = (rollbackCount * 1000.0) / duration;
        
        System.out.println("✅ Выполнено " + rollbackCount + " откатов за " + duration + " мс");
        System.out.println("⚡ Производительность откатов: " + String.format("%.2f", rollbacksPerSecond) + " откатов/сек");
    }
    
    /**
     * Нагрузочное тестирование
     */
    private static void benchmarkLoadTest(EventStreamProcessor processor, int eventCount) {
        System.out.println("\n🔥 Нагрузочное тестирование " + eventCount + " событий...");
        
        long startTime = System.currentTimeMillis();
        AtomicLong processedEvents = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(eventCount);
        
        // Создаем события в нескольких потоках
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < eventCount; i++) {
            String entityId = "load_user_" + (i % 1000); // 1000 пользователей
            String eventType = getEventType(i);
            Map<String, Object> payload = createPayload(i);
            
            Event event = processor.createEvent(entityId, Instant.now(), eventType, payload);
            
            CompletableFuture<Void> future = processor.processEvent(event)
                .thenRun(() -> {
                    processedEvents.incrementAndGet();
                    latch.countDown();
                });
            
            futures.add(future);
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double eventsPerSecond = (eventCount * 1000.0) / duration;
        
        System.out.println("✅ Нагрузочный тест завершен: " + eventCount + " событий за " + duration + " мс");
        System.out.println("⚡ Общая производительность: " + String.format("%.2f", eventsPerSecond) + " событий/сек");
    }
    
    /**
     * Ждет завершения обработки всех событий
     */
    private static void waitForCompletion(AtomicLong counter, int expectedCount) {
        long startWait = System.currentTimeMillis();
        long timeout = 30000; // 30 секунд таймаут
        
        while (counter.get() < expectedCount && (System.currentTimeMillis() - startWait) < timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (counter.get() < expectedCount) {
            System.out.println("⚠️ Таймаут ожидания: обработано " + counter.get() + " из " + expectedCount + " событий");
        }
    }
    
    /**
     * Определяет тип события на основе индекса
     */
    private static String getEventType(int index) {
        if (index % 3 == 0) return "USER_CREATED";
        if (index % 3 == 1) return "USER_UPDATED";
        return "USER_DELETED";
    }
    
    /**
     * Создает payload для события
     */
    private static Map<String, Object> createPayload(int index) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("index", index);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("data", "test_data_" + index);
        return payload;
    }
}