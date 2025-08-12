package com.eventstream;

import com.eventstream.core.*;
import com.eventstream.handlers.UserEventProcessor;
import com.eventstream.utils.PerformanceTest;

import java.time.Instant;
import java.util.Map;

/**
 * Демонстрация работы системы потоковой обработки событий
 */
public class Demo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Демонстрация EventStreamProcessor");
        System.out.println("=====================================\n");
        
        // Создаем процессор событий
        EventStreamProcessor processor = new EventStreamProcessor(100, 10);
        
        // Регистрируем специализированные обработчики
        processor.registerEventProcessor(new UserEventProcessor());
        
        // Демонстрируем базовую функциональность
        demonstrateBasicFunctionality(processor);
        
        // Демонстрируем откаты состояний
        demonstrateStateRollback(processor);
        
        // Демонстрируем расширяемость
        demonstrateExtensibility(processor);
        
        // Демонстрируем производительность
        demonstratePerformance(processor);
        
        // Показываем статистику
        System.out.println("\n📊 Финальная статистика:");
        System.out.println(processor.getStatistics());
        
        // Завершаем работу
        processor.shutdown();
        
        System.out.println("\n✅ Демонстрация завершена!");
    }
    
    private static void demonstrateBasicFunctionality(EventStreamProcessor processor) {
        System.out.println("🔧 Базовая функциональность:");
        System.out.println("----------------------------");
        
        // Создаем несколько событий для пользователя
        String userId = "demo-user-001";
        
        Event userCreated = new Event(
            userId,
            Instant.now().minusSeconds(300),
            "user.created",
            Map.of("name", "John Doe", "email", "john@example.com", "age", 30),
            0
        );
        
        Event userUpdated = new Event(
            userId,
            Instant.now().minusSeconds(200),
            "user.updated",
            Map.of("age", 31, "city", "New York"),
            0
        );
        
        Event orderCreated = new Event(
            userId,
            Instant.now().minusSeconds(100),
            "order.created",
            Map.of("orderId", "ORD-001", "amount", 99.99, "items", 3),
            0
        );
        
        // Обрабатываем события
        processor.processEvent(userCreated);
        processor.processEvent(userUpdated);
        processor.processEvent(orderCreated);
        
        // Даем время на обработку
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Показываем текущее состояние
        Entity currentState = processor.getCurrentEntityState(userId);
        System.out.println("Текущее состояние пользователя:");
        System.out.println("  ID: " + currentState.getId());
        System.out.println("  Статус: " + currentState.getStateValue("status"));
        System.out.println("  Имя: " + currentState.getStateValue("name"));
        System.out.println("  Возраст: " + currentState.getStateValue("age"));
        System.out.println("  Город: " + currentState.getStateValue("city"));
        System.out.println("  Последний заказ: " + currentState.getStateValue("orderId"));
        System.out.println();
    }
    
    private static void demonstrateStateRollback(EventStreamProcessor processor) {
        System.out.println("⏰ Откат состояний:");
        System.out.println("-------------------");
        
        String userId = "demo-user-001";
        Instant now = Instant.now();
        
        // Показываем состояние на разные моменты времени
        Instant[] timestamps = {
            now.minusSeconds(250), // После создания, до обновления
            now.minusSeconds(150), // После обновления, до заказа
            now.minusSeconds(50)   // После заказа
        };
        
        for (Instant timestamp : timestamps) {
            Entity historicalState = processor.getEntityStateAt(userId, timestamp);
            System.out.printf("Состояние на %s:\n", timestamp);
            System.out.printf("  Имя: %s\n", historicalState.getStateValue("name"));
            System.out.printf("  Возраст: %s\n", historicalState.getStateValue("age"));
            System.out.printf("  Город: %s\n", historicalState.getStateValue("city"));
            System.out.printf("  Заказ: %s\n", historicalState.getStateValue("orderId"));
            System.out.println();
        }
    }
    
    private static void demonstrateExtensibility(EventStreamProcessor processor) {
        System.out.println("🔌 Расширяемость системы:");
        System.out.println("-------------------------");
        
        // Создаем новый тип события
        String userId = "demo-user-002";
        
        Event userCreated = new Event(
            userId,
            Instant.now().minusSeconds(100),
            "user.created",
            Map.of("name", "Jane Smith", "email", "jane@example.com"),
            0
        );
        
        Event userVerified = new Event(
            userId,
            Instant.now().minusSeconds(50),
            "user.verified",
            Map.of("verificationMethod", "email", "verifiedAt", Instant.now()),
            0
        );
        
        // Обрабатываем события
        processor.processEvent(userCreated);
        processor.processEvent(userVerified);
        
        // Даем время на обработку
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Показываем, что система автоматически обработала новый тип события
        Entity currentState = processor.getCurrentEntityState(userId);
        System.out.println("Новый пользователь (с новым типом события):");
        System.out.println("  ID: " + currentState.getId());
        System.out.println("  Имя: " + currentState.getStateValue("name"));
        System.out.println("  Статус: " + currentState.getStateValue("status"));
        System.out.println("  Метод верификации: " + currentState.getStateValue("verificationMethod"));
        System.out.println();
        
        System.out.println("💡 Система автоматически обработала новый тип события 'user.verified'");
        System.out.println("   без изменения существующего кода!");
        System.out.println();
    }
    
    private static void demonstratePerformance(EventStreamProcessor processor) {
        System.out.println("⚡ Тест производительности:");
        System.out.println("---------------------------");
        
        // Создаем поток событий для тестирования
        SimulatedEventStream eventStream = new SimulatedEventStream(500); // 500 событий/сек
        
        // Подписываемся на события
        eventStream.subscribe(processor::processEvent);
        
        // Запускаем поток
        eventStream.start();
        
        try {
            // Даем время на генерацию событий
            Thread.sleep(5000);
            
            // Останавливаем поток
            eventStream.stop();
            
            // Показываем результаты
            long processedEvents = processor.getProcessedEventsCount();
            System.out.printf("Обработано событий: %d\n", processedEvents);
            System.out.printf("Событий в секунду: %.2f\n", (double) processedEvents / 5);
            
            // Тестируем скорость отката
            String testEntityId = "perf-test-user";
            Instant testTime = Instant.now().minusSeconds(100);
            
            long startTime = System.nanoTime();
            Entity entity = processor.getEntityStateAt(testEntityId, testTime);
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            
            System.out.printf("Время отката: %d мс %s\n", 
                durationMs,
                durationMs < 100 ? "✅" : "❌"
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println();
    }
}