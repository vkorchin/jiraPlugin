package com.eventprocessing;

import com.eventprocessing.handler.impl.*;
import com.eventprocessing.model.Entity;
import com.eventprocessing.processor.EventProcessor;
import com.eventprocessing.rollback.RollbackService;
import com.eventprocessing.stream.InMemoryEventStream;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Базовые тесты для системы обработки событий
 */
public class EventProcessingTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Запуск тестов системы обработки событий ===\n");
        
        runBasicEventProcessingTest();
        runRollbackTest();
        runPerformanceTest();
        
        System.out.println("=== Все тесты пройдены успешно ===");
    }
    
    /**
     * Тест базовой обработки событий
     */
    private static void runBasicEventProcessingTest() throws Exception {
        System.out.println("1. Тест базовой обработки событий:");
        
        EventProcessor processor = new EventProcessor();
        InMemoryEventStream eventStream = new InMemoryEventStream();
        
        // Регистрируем обработчики
        processor.registerEventHandler(new CreateEventHandler());
        processor.registerEventHandler(new UpdateEventHandler());
        processor.registerEventHandler(new DeleteEventHandler());
        processor.registerEventHandler(new StatusChangeEventHandler());
        
        processor.start(eventStream);
        
        // Отправляем события
        eventStream.pushEvent("test1", "CREATE", Map.of("name", "Test Entity", "value", 100));
        Thread.sleep(50);
        
        eventStream.pushEvent("test1", "UPDATE", Map.of("value", 200, "updated", true));
        Thread.sleep(50);
        
        eventStream.pushEvent("test1", "STATUS_CHANGE", Map.of("status", "active"));
        Thread.sleep(50);
        
        // Проверяем результат
        Entity entity = processor.getCurrentState("test1");
        assert entity != null : "Entity должна существовать";
        assert entity.getStateValue("name").equals("Test Entity") : "Имя должно быть сохранено";
        assert entity.getStateValue("value").equals(200) : "Значение должно быть обновлено";
        assert entity.getStateValue("status").equals("active") : "Статус должен быть установлен";
        
        processor.stop();
        eventStream.stop();
        
        System.out.println("  ✓ Базовая обработка событий работает корректно\n");
    }
    
    /**
     * Тест откатов состояний
     */
    private static void runRollbackTest() throws Exception {
        System.out.println("2. Тест откатов состояний:");
        
        EventProcessor processor = new EventProcessor();
        InMemoryEventStream eventStream = new InMemoryEventStream();
        
        processor.registerEventHandler(new CreateEventHandler());
        processor.registerEventHandler(new UpdateEventHandler());
        processor.start(eventStream);
        
        Instant startTime = Instant.now();
        
        // Создаем последовательность событий с временными метками
        eventStream.pushEvent("rollback_test", "CREATE", Map.of("value", 1));
        Thread.sleep(100);
        
        Instant midTime = Instant.now();
        eventStream.pushEvent("rollback_test", "UPDATE", Map.of("value", 2));
        Thread.sleep(100);
        
        eventStream.pushEvent("rollback_test", "UPDATE", Map.of("value", 3));
        Thread.sleep(100);
        
        // Проверяем текущее состояние
        Entity currentEntity = processor.getCurrentState("rollback_test");
        assert currentEntity.getStateValue("value").equals(3) : "Текущее значение должно быть 3";
        
        // Тестируем откат
        RollbackService rollbackService = new RollbackService(
            processor.stateManager, processor.snapshotManager, processor.handlerRegistry
        );
        
        CompletableFuture<RollbackService.RollbackResult> future = 
            rollbackService.rollbackToTime("rollback_test", midTime, 
                RollbackService.RollbackStrategy.FULL_EVENT_REPLAY);
        
        RollbackService.RollbackResult result = future.get(5, TimeUnit.SECONDS);
        
        assert result.isSuccess() : "Откат должен быть успешным";
        assert result.getEntity() != null : "Результат отката не должен быть null";
        
        // Проверяем, что откат выполнен быстро (< 100 мс)
        assert result.getDurationMs() < 100 : "Откат должен выполняться быстро";
        
        processor.stop();
        eventStream.stop();
        rollbackService.shutdown();
        
        System.out.println("  ✓ Откаты состояний работают корректно");
        System.out.println("  ✓ Время отката: " + result.getDurationMs() + " мс\n");
    }
    
    /**
     * Тест производительности
     */
    private static void runPerformanceTest() throws Exception {
        System.out.println("3. Тест производительности:");
        
        EventProcessor processor = new EventProcessor();
        InMemoryEventStream eventStream = new InMemoryEventStream();
        
        processor.registerEventHandler(new CreateEventHandler());
        processor.registerEventHandler(new UpdateEventHandler());
        processor.start(eventStream);
        
        int eventCount = 10000;
        long startTime = System.currentTimeMillis();
        
        // Генерируем много событий
        for (int i = 0; i < eventCount; i++) {
            eventStream.pushEvent("perf_test_" + (i % 100), "UPDATE", 
                Map.of("iteration", i, "timestamp", System.currentTimeMillis()));
        }
        
        // Ждем обработки
        Thread.sleep(3000);
        
        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        double eventsPerSecond = eventCount / processingTime;
        
        EventProcessor.ProcessingStats stats = processor.getStats();
        
        System.out.println("  ✓ Обработано " + eventCount + " событий за " + processingTime + " сек");
        System.out.println("  ✓ Производительность: " + Math.round(eventsPerSecond) + " событий/сек");
        System.out.println("  ✓ Среднее время обработки: " + String.format("%.3f", stats.avgProcessingTimeMs) + " мс");
        
        // Проверяем, что производительность приемлемая (> 1000 событий/сек)
        assert eventsPerSecond > 1000 : "Производительность должна быть > 1000 событий/сек";
        
        processor.stop();
        eventStream.stop();
        
        System.out.println("  ✓ Тест производительности пройден\n");
    }
}