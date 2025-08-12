package com.eventstream;

import com.eventstream.core.EventStreamProcessor;
import com.eventstream.core.Event;
import com.eventstream.core.Entity;
import com.eventstream.handlers.UserEventHandler;
import com.eventstream.handlers.OrderEventHandler;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Расширенный пример использования системы с несколькими типами сущностей
 */
public class AdvancedExample {
    
    public static void main(String[] args) {
        System.out.println("🚀 Расширенный пример EventStreamProcessor\n");
        
        // Создаем процессор с частотой снимков каждые 5 событий и 4 потоками
        EventStreamProcessor processor = new EventStreamProcessor(5, 4);
        
        // Регистрируем обработчики событий
        processor.registerEventHandler(new UserEventHandler());
        processor.registerEventHandler(new OrderEventHandler());
        
        // Тестируем работу с пользователями
        testUserWorkflow(processor);
        
        // Тестируем работу с заказами
        testOrderWorkflow(processor);
        
        // Тестируем параллельную обработку
        testParallelProcessing(processor);
        
        // Тестируем откат состояний
        testStateRollback(processor);
        
        // Выводим статистику
        System.out.println("\n📊 Финальная статистика:");
        System.out.println(processor.getSystemStats());
        
        // Завершаем работу
        processor.shutdown();
        System.out.println("\n🏁 Расширенный пример завершен!");
    }
    
    /**
     * Тестирует workflow пользователей
     */
    private static void testUserWorkflow(EventStreamProcessor processor) {
        System.out.println("👥 Тестирование workflow пользователей...");
        
        String userId = "user_advanced_001";
        
        // Создание пользователя
        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("name", "Анна Петрова");
        createPayload.put("email", "anna@example.com");
        createPayload.put("age", 25);
        createPayload.put("city", "Санкт-Петербург");
        
        Event createEvent = processor.createEvent(userId, Instant.now(), "USER_CREATED", createPayload);
        processor.processEvent(createEvent).join();
        
        // Обновление профиля
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("age", 26);
        updatePayload.put("job", "Дизайнер");
        updatePayload.put("interests", "Искусство, путешествия");
        
        Event updateEvent = processor.createEvent(userId, Instant.now(), "USER_UPDATED", updatePayload);
        processor.processEvent(updateEvent).join();
        
        // Проверяем текущее состояние
        Entity currentState = processor.getCurrentEntityState(userId);
        System.out.println("✅ Пользователь создан и обновлен: " + currentState.getState());
    }
    
    /**
     * Тестирует workflow заказов
     */
    private static void testOrderWorkflow(EventStreamProcessor processor) {
        System.out.println("\n🛒 Тестирование workflow заказов...");
        
        String orderId = "order_001";
        
        // Создание заказа
        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("customerId", "user_advanced_001");
        createPayload.put("items", "Книга 'Java Concurrency in Practice'");
        createPayload.put("amount", 2500.0);
        createPayload.put("currency", "RUB");
        
        Event createEvent = processor.createEvent(orderId, Instant.now(), "ORDER_CREATED", createPayload);
        processor.processEvent(createEvent).join();
        
        // Обновление заказа
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("amount", 2400.0); // Скидка
        updatePayload.put("discount", "10%");
        
        Event updateEvent = processor.createEvent(orderId, Instant.now(), "ORDER_UPDATED", updatePayload);
        processor.processEvent(updateEvent).join();
        
        // Завершение заказа
        Map<String, Object> completePayload = new HashMap<>();
        completePayload.put("notes", "Заказ успешно доставлен");
        completePayload.put("deliveryDate", Instant.now());
        
        Event completeEvent = processor.createEvent(orderId, Instant.now(), "ORDER_COMPLETED", completePayload);
        processor.processEvent(completeEvent).join();
        
        // Проверяем текущее состояние
        Entity currentState = processor.getCurrentEntityState(orderId);
        System.out.println("✅ Заказ создан, обновлен и завершен: " + currentState.getState());
    }
    
    /**
     * Тестирует параллельную обработку
     */
    private static void testParallelProcessing(EventStreamProcessor processor) {
        System.out.println("\n⚡ Тестирование параллельной обработки...");
        
        long startTime = System.currentTimeMillis();
        
        // Создаем множество событий для разных пользователей
        CompletableFuture<Void>[] futures = new CompletableFuture[100];
        
        for (int i = 0; i < 100; i++) {
            String userId = "parallel_user_" + i;
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "Пользователь " + i);
            payload.put("value", i);
            
            Event event = processor.createEvent(userId, Instant.now(), "USER_CREATED", payload);
            futures[i] = processor.processEvent(event);
        }
        
        // Ждем завершения всех операций
        CompletableFuture.allOf(futures).join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("✅ Параллельно обработано 100 событий за " + duration + " мс");
        System.out.println("⚡ Средняя производительность: " + String.format("%.2f", 100.0 * 1000 / duration) + " событий/сек");
    }
    
    /**
     * Тестирует откат состояний
     */
    private static void testStateRollback(EventStreamProcessor processor) {
        System.out.println("\n⏪ Тестирование отката состояний...");
        
        String userId = "rollback_user_001";
        
        // Создаем пользователя
        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("name", "Тест Откат");
        createPayload.put("status", "ACTIVE");
        
        Event createEvent = processor.createEvent(userId, Instant.now(), "USER_CREATED", createPayload);
        processor.processEvent(createEvent).join();
        
        Instant createTime = createEvent.getTimestamp();
        
        // Делаем несколько обновлений
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("step", i);
            updatePayload.put("value", "update_" + i);
            
            Event updateEvent = processor.createEvent(userId, Instant.now(), "USER_UPDATED", updatePayload);
            processor.processEvent(updateEvent).join();
            
            // Небольшая задержка между событиями
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Тестируем откат на разные моменты времени
        System.out.println("🔄 Тестирование откатов...");
        
        // Откат к моменту создания
        CompletableFuture<Entity> createTimeState = processor.getEntityStateAt(userId, createTime);
        createTimeState.thenAccept(state -> {
            System.out.println("⏪ Состояние на момент создания: " + state.getState());
        }).join();
        
        // Откат к промежуточному состоянию
        Instant intermediateTime = createTime.plusMillis(50);
        CompletableFuture<Entity> intermediateState = processor.getEntityStateAt(userId, intermediateTime);
        intermediateState.thenAccept(state -> {
            System.out.println("⏪ Промежуточное состояние: " + state.getState());
        }).join();
        
        // Текущее состояние
        Entity currentState = processor.getCurrentEntityState(userId);
        System.out.println("✅ Текущее состояние: " + currentState.getState());
    }
}