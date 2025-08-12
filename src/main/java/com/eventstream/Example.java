package com.eventstream;

import com.eventstream.core.*;
import com.eventstream.handlers.UserEventProcessor;
import com.eventstream.utils.PerformanceMonitor;

import java.time.Instant;
import java.util.Map;

/**
 * Простой пример использования EventStreamProcessor
 */
public class Example {
    
    public static void main(String[] args) {
        System.out.println("🎯 Простой пример EventStreamProcessor");
        System.out.println("=====================================\n");
        
        // 1. Создаем процессор
        EventStreamProcessor processor = new EventStreamProcessor(50, 10);
        
        // 2. Регистрируем обработчики
        processor.registerEventProcessor(new UserEventProcessor());
        
        // 3. Создаем мониторинг производительности
        PerformanceMonitor monitor = new PerformanceMonitor(processor);
        
        // 4. Обрабатываем несколько событий
        processSampleEvents(processor, monitor);
        
        // 5. Демонстрируем откаты
        demonstrateRollbacks(processor, monitor);
        
        // 6. Показываем статистику
        monitor.printStats();
        
        // 7. Завершаем работу
        processor.shutdown();
        
        System.out.println("\n✅ Пример завершен!");
    }
    
    private static void processSampleEvents(EventStreamProcessor processor, PerformanceMonitor monitor) {
        System.out.println("📝 Обработка событий:");
        System.out.println("----------------------");
        
        String userId = "example-user";
        
        // Событие 1: Создание пользователя
        Event userCreated = new Event(
            userId,
            Instant.now().minusSeconds(300),
            "user.created",
            Map.of("name", "Alice Johnson", "email", "alice@example.com", "age", 25),
            0
        );
        
        processor.processEvent(userCreated);
        monitor.trackEventProcessing();
        
        // Событие 2: Обновление пользователя
        Event userUpdated = new Event(
            userId,
            Instant.now().minusSeconds(200),
            "user.updated",
            Map.of("age", 26, "city", "London"),
            0
        );
        
        processor.processEvent(userUpdated);
        monitor.trackEventProcessing();
        
        // Событие 3: Создание заказа
        Event orderCreated = new Event(
            userId,
            Instant.now().minusSeconds(100),
            "order.created",
            Map.of("orderId", "ORD-123", "amount", 149.99, "items", 2),
            0
        );
        
        processor.processEvent(orderCreated);
        monitor.trackEventProcessing();
        
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
        System.out.println("  Имя: " + currentState.getStateValue("name"));
        System.out.println("  Возраст: " + currentState.getStateValue("age"));
        System.out.println("  Город: " + currentState.getStateValue("city"));
        System.out.println("  Заказ: " + currentState.getStateValue("orderId"));
        System.out.println();
    }
    
    private static void demonstrateRollbacks(EventStreamProcessor processor, PerformanceMonitor monitor) {
        System.out.println("⏰ Демонстрация откатов:");
        System.out.println("------------------------");
        
        String userId = "example-user";
        Instant now = Instant.now();
        
        // Тестируем откат на разные моменты времени
        Instant[] timestamps = {
            now.minusSeconds(250), // После создания, до обновления
            now.minusSeconds(150), // После обновления, до заказа
            now.minusSeconds(50)   // После заказа
        };
        
        for (Instant timestamp : timestamps) {
            monitor.startMetric("rollback_" + timestamp.getEpochSecond());
            
            long startTime = System.nanoTime();
            Entity historicalState = processor.getEntityStateAt(userId, timestamp);
            long endTime = System.nanoTime();
            
            long durationMs = (endTime - startTime) / 1_000_000;
            monitor.endMetric("rollback_" + timestamp.getEpochSecond());
            monitor.trackRollback(durationMs);
            
            System.out.printf("Состояние на %s (%d мс):\n", timestamp, durationMs);
            System.out.printf("  Имя: %s\n", historicalState.getStateValue("name"));
            System.out.printf("  Возраст: %s\n", historicalState.getStateValue("age"));
            System.out.printf("  Город: %s\n", historicalState.getStateValue("city"));
            System.out.printf("  Заказ: %s\n", historicalState.getStateValue("orderId"));
            System.out.println();
        }
    }
}