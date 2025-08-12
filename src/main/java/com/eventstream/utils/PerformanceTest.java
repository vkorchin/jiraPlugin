package com.eventstream.utils;

import com.eventstream.core.*;
import com.eventstream.handlers.UserEventProcessor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Тест производительности системы
 * Демонстрирует способность обрабатывать миллионы событий в минуту
 */
public class PerformanceTest {
    
    public static void main(String[] args) {
        PerformanceTest test = new PerformanceTest();
        test.runPerformanceTest();
    }
    
    public void runPerformanceTest() {
        System.out.println("=== Тест производительности EventStreamProcessor ===");
        
        // Создаем процессор с оптимизированными настройками
        EventStreamProcessor processor = new EventStreamProcessor(50, 20);
        
        // Регистрируем специализированные обработчики
        processor.registerEventProcessor(new UserEventProcessor());
        
        // Создаем поток событий
        SimulatedEventStream eventStream = new SimulatedEventStream(1000); // 1000 событий/сек
        
        // Подписываемся на события
        eventStream.subscribe(processor::processEvent);
        
        // Запускаем поток событий
        eventStream.start();
        
        try {
            // Даем системе время на разогрев
            Thread.sleep(5000);
            
            // Тестируем производительность обработки
            testEventProcessingPerformance(processor, eventStream);
            
            // Тестируем производительность откатов
            testRollbackPerformance(processor);
            
            // Тестируем производительность под нагрузкой
            testUnderLoadPerformance(processor, eventStream);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            eventStream.stop();
            processor.shutdown();
        }
    }
    
    private void testEventProcessingPerformance(EventStreamProcessor processor, SimulatedEventStream eventStream) 
            throws InterruptedException {
        System.out.println("\n--- Тест производительности обработки событий ---");
        
        long startTime = System.currentTimeMillis();
        long initialEventCount = processor.getProcessedEventsCount();
        
        // Генерируем события в течение 10 секунд
        Thread.sleep(10000);
        
        long endTime = System.currentTimeMillis();
        long finalEventCount = processor.getProcessedEventsCount();
        long processedEvents = finalEventCount - initialEventCount;
        long durationSeconds = (endTime - startTime) / 1000;
        
        double eventsPerSecond = (double) processedEvents / durationSeconds;
        double eventsPerMinute = eventsPerSecond * 60;
        
        System.out.printf("Обработано событий: %d\n", processedEvents);
        System.out.printf("Время выполнения: %d секунд\n", durationSeconds);
        System.out.printf("Событий в секунду: %.2f\n", eventsPerSecond);
        System.out.printf("Событий в минуту: %.2f\n", eventsPerMinute);
        System.out.printf("Цель (1M/мин): %.2f%%\n", (eventsPerMinute / 1000000) * 100);
        
        if (eventsPerMinute >= 1000000) {
            System.out.println("✅ Цель достигнута: > 1M событий/мин");
        } else {
            System.out.println("❌ Цель не достигнута: < 1M событий/мин");
        }
    }
    
    private void testRollbackPerformance(EventStreamProcessor processor) {
        System.out.println("\n--- Тест производительности откатов ---");
        
        String testEntityId = "test-user-001";
        Instant now = Instant.now();
        
        // Тестируем откат на разные временные точки
        Instant[] testTimestamps = {
            now.minusSeconds(3600), // 1 час назад
            now.minusSeconds(1800), // 30 минут назад
            now.minusSeconds(900),  // 15 минут назад
            now.minusSeconds(300),  // 5 минут назад
            now.minusSeconds(60)    // 1 минуту назад
        };
        
        for (Instant timestamp : testTimestamps) {
            long startTime = System.nanoTime();
            
            Entity entity = processor.getEntityStateAt(testEntityId, timestamp);
            
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // конвертируем в миллисекунды
            
            System.out.printf("Откат на %s: %d мс %s\n", 
                timestamp, 
                durationMs,
                durationMs < 100 ? "✅" : "❌"
            );
            
            if (durationMs >= 100) {
                System.out.println("   ⚠️  Откат медленнее целевых 100 мс");
            }
        }
    }
    
    private void testUnderLoadPerformance(EventStreamProcessor processor, SimulatedEventStream eventStream) 
            throws InterruptedException {
        System.out.println("\n--- Тест производительности под нагрузкой ---");
        
        // Увеличиваем нагрузку
        eventStream.stop();
        SimulatedEventStream highLoadStream = new SimulatedEventStream(5000); // 5000 событий/сек
        highLoadStream.subscribe(processor::processEvent);
        highLoadStream.start();
        
        // Даем время на разогрев
        Thread.sleep(2000);
        
        // Тестируем откаты под нагрузкой
        String testEntityId = "load-test-user";
        Instant testTime = Instant.now().minusSeconds(300);
        
        long startTime = System.nanoTime();
        Entity entity = processor.getEntityStateAt(testEntityId, testTime);
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.printf("Откат под нагрузкой: %d мс %s\n", 
            durationMs,
            durationMs < 100 ? "✅" : "❌"
        );
        
        highLoadStream.stop();
    }
    
    /**
     * Тест многопоточности
     */
    public void testConcurrency() {
        System.out.println("\n--- Тест многопоточности ---");
        
        EventStreamProcessor processor = new EventStreamProcessor();
        int threadCount = 10;
        int eventsPerThread = 1000;
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong totalEvents = new AtomicLong(0);
        
        // Создаем потоки для генерации событий
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        Event event = new Event(
                            "concurrent-entity-" + threadId,
                            Instant.now(),
                            "concurrent.event",
                            Map.of("threadId", threadId, "eventId", j),
                            0
                        );
                        processor.processEvent(event);
                        totalEvents.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        try {
            // Ждем завершения всех потоков
            latch.await(30, TimeUnit.SECONDS);
            
            System.out.printf("Генерировано событий: %d\n", totalEvents.get());
            System.out.printf("Обработано событий: %d\n", processor.getProcessedEventsCount());
            
            if (totalEvents.get() == processor.getProcessedEventsCount()) {
                System.out.println("✅ Многопоточность работает корректно");
            } else {
                System.out.println("❌ Проблемы с многопоточностью");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Тест многопоточности прерван");
        }
        
        processor.shutdown();
    }
}