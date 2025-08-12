package com.eventprocessing.demo;

import com.eventprocessing.handler.impl.*;
import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;
import com.eventprocessing.processor.EventProcessor;
import com.eventprocessing.rollback.RollbackService;
import com.eventprocessing.stream.InMemoryEventStream;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Демонстрационный пример системы потоковой обработки событий с откатом состояний
 */
public class EventProcessingDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Демонстрация системы потоковой обработки событий ===\n");
        
        // Создаем компоненты системы
        EventProcessor processor = new EventProcessor();
        InMemoryEventStream eventStream = new InMemoryEventStream();
        
        // Регистрируем обработчики событий
        processor.registerEventHandler(new CreateEventHandler());
        processor.registerEventHandler(new UpdateEventHandler());
        processor.registerEventHandler(new DeleteEventHandler());
        processor.registerEventHandler(new StatusChangeEventHandler());
        
        // Запускаем процессор
        processor.start(eventStream);
        
        System.out.println("1. Система запущена и готова к обработке событий\n");
        
        // Демонстрируем обработку событий
        demonstrateEventProcessing(eventStream, processor);
        
        // Демонстрируем откаты состояний
        demonstrateRollbacks(processor);
        
        // Демонстрируем производительность
        demonstratePerformance(eventStream, processor);
        
        // Останавливаем систему
        processor.stop();
        eventStream.stop();
        
        System.out.println("\n=== Демонстрация завершена ===");
    }
    
    private static void demonstrateEventProcessing(InMemoryEventStream eventStream, EventProcessor processor) 
            throws InterruptedException {
        
        System.out.println("2. Демонстрация обработки событий:");
        
        Instant baseTime = Instant.now();
        
        // Создаем сущность
        eventStream.pushEvent("user123", "CREATE", Map.of(
            "name", "John Doe",
            "email", "john@example.com",
            "status", "active"
        ));
        
        Thread.sleep(100); // Даем время на обработку
        
        // Обновляем сущность
        eventStream.pushEvent("user123", "UPDATE", Map.of(
            "email", "john.doe@example.com",
            "lastLogin", baseTime.plus(1, ChronoUnit.MINUTES).toString()
        ));
        
        Thread.sleep(100);
        
        // Изменяем статус
        eventStream.pushEvent("user123", "STATUS_CHANGE", Map.of(
            "status", "inactive",
            "reason", "User requested deactivation"
        ));
        
        Thread.sleep(100);
        
        // Проверяем текущее состояние
        Entity currentState = processor.getCurrentState("user123");
        System.out.println("  Текущее состояние user123: " + currentState);
        System.out.println();
    }
    
    private static void demonstrateRollbacks(EventProcessor processor) throws Exception {
        System.out.println("3. Демонстрация откатов состояний:");
        
        // Создаем RollbackService
        RollbackService rollbackService = new RollbackService(
            processor.stateManager, processor.snapshotManager, processor.handlerRegistry
        );
        
        // Получаем время для отката (5 минут назад)
        Instant rollbackTime = Instant.now().minus(5, ChronoUnit.MINUTES);
        
        // Тестируем разные стратегии отката
        System.out.println("  Тестирование различных стратегий отката:");
        
        for (RollbackService.RollbackStrategy strategy : RollbackService.RollbackStrategy.values()) {
            CompletableFuture<RollbackService.RollbackResult> future = 
                rollbackService.rollbackToTime("user123", rollbackTime, strategy);
            
            RollbackService.RollbackResult result = future.get(5, TimeUnit.SECONDS);
            System.out.println("    " + result);
        }
        
        // Рекомендуемая стратегия
        RollbackService.RollbackStrategy recommended = 
            rollbackService.getRecommendedStrategy("user123", rollbackTime);
        System.out.println("  Рекомендуемая стратегия для user123: " + recommended);
        
        rollbackService.shutdown();
        System.out.println();
    }
    
    private static void demonstratePerformance(InMemoryEventStream eventStream, EventProcessor processor) 
            throws InterruptedException {
        
        System.out.println("4. Демонстрация производительности:");
        
        // Генерируем большое количество событий
        int eventCount = 1000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < eventCount; i++) {
            String entityId = "entity" + (i % 10); // 10 разных сущностей
            String eventType = i % 4 == 0 ? "CREATE" : 
                              i % 4 == 1 ? "UPDATE" : 
                              i % 4 == 2 ? "STATUS_CHANGE" : "DELETE";
            
            eventStream.pushEvent(entityId, eventType, Map.of(
                "iteration", i,
                "timestamp", Instant.now().toString(),
                "data", "test_data_" + i
            ));
            
            // Небольшая задержка для имитации реального потока
            if (i % 100 == 0) {
                Thread.sleep(10);
            }
        }
        
        // Ждем обработки всех событий
        Thread.sleep(2000);
        
        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        
        // Получаем статистику
        EventProcessor.ProcessingStats stats = processor.getStats();
        
        System.out.println("  Обработано " + eventCount + " событий за " + processingTime + " секунд");
        System.out.println("  Производительность: " + (eventCount / processingTime) + " событий/сек");
        System.out.println("  Статистика: " + stats);
        
        // Тестируем производительность откатов
        try {
            testRollbackPerformance(processor);
        } catch (Exception e) {
            System.err.println("Ошибка при тестировании откатов: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testRollbackPerformance(EventProcessor processor) throws Exception {
        System.out.println("  Тестирование производительности откатов:");
        
        RollbackService rollbackService = new RollbackService(
            processor.stateManager, processor.snapshotManager, processor.handlerRegistry
        );
        
        Instant rollbackTime = Instant.now().minus(30, ChronoUnit.SECONDS);
        
        // Тестируем откат для нескольких сущностей
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            String entityId = "entity" + i;
            CompletableFuture<RollbackService.RollbackResult> future = 
                rollbackService.rollbackToTime(entityId, rollbackTime, 
                    RollbackService.RollbackStrategy.SNAPSHOT_PLUS_EVENTS);
            
            RollbackService.RollbackResult result = future.get(1, TimeUnit.SECONDS);
            if (i == 0) { // Показываем результат только для первой сущности
                System.out.println("    Пример отката: " + result);
            }
        }
        
        long endTime = System.currentTimeMillis();
        double rollbackDuration = (endTime - startTime) / 1000.0;
        
        System.out.println("    10 откатов выполнено за " + rollbackDuration + " секунд");
        System.out.println("    Средняя скорость отката: " + (rollbackDuration * 100) + " мс/откат");
        
        rollbackService.shutdown();
    }
}