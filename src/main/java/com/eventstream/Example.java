package com.eventstream;

import com.eventstream.core.EventStreamProcessor;
import com.eventstream.core.Event;
import com.eventstream.core.Entity;
import com.eventstream.handlers.UserEventHandler;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Простой пример использования системы потоковой обработки событий
 */
public class Example {
    
    public static void main(String[] args) {
        System.out.println("🎯 Пример использования EventStreamProcessor\n");
        
        // Создаем процессор с частотой снимков каждые 10 событий и 2 потоками
        EventStreamProcessor processor = new EventStreamProcessor(10, 2);
        
        // Регистрируем обработчик событий пользователей
        processor.registerEventHandler(new UserEventHandler());
        
        // Создаем пользователя
        String userId = "user_123";
        
        System.out.println("1️⃣ Создание пользователя...");
        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("name", "Иван Иванов");
        createPayload.put("email", "ivan@example.com");
        createPayload.put("age", 30);
        
        Event createEvent = processor.createEvent(userId, Instant.now(), "USER_CREATED", createPayload);
        processor.processEvent(createEvent).join();
        
        // Проверяем текущее состояние
        Entity currentState = processor.getCurrentEntityState(userId);
        System.out.println("✅ Пользователь создан: " + currentState.getState());
        
        // Обновляем пользователя
        System.out.println("\n2️⃣ Обновление пользователя...");
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("age", 31);
        updatePayload.put("city", "Москва");
        
        Event updateEvent = processor.createEvent(userId, Instant.now(), "USER_UPDATED", updatePayload);
        processor.processEvent(updateEvent).join();
        
        currentState = processor.getCurrentEntityState(userId);
        System.out.println("✅ Пользователь обновлен: " + currentState.getState());
        
        // Еще одно обновление
        System.out.println("\n3️⃣ Еще одно обновление...");
        Map<String, Object> updatePayload2 = new HashMap<>();
        updatePayload2.put("age", 32);
        updatePayload2.put("job", "Разработчик");
        
        Event updateEvent2 = processor.createEvent(userId, Instant.now(), "USER_UPDATED", updatePayload2);
        processor.processEvent(updateEvent2).join();
        
        currentState = processor.getCurrentEntityState(userId);
        System.out.println("✅ Пользователь обновлен: " + currentState.getState());
        
        // Тестируем откат состояния
        System.out.println("\n4️⃣ Тестирование отката состояния...");
        
        // Получаем состояние на момент первого обновления
        Instant firstUpdateTime = updateEvent.getTimestamp();
        CompletableFuture<Entity> rollbackFuture = processor.getEntityStateAt(userId, firstUpdateTime);
        
        rollbackFuture.thenAccept(rolledBackState -> {
            System.out.println("⏪ Состояние откачено к первому обновлению: " + rolledBackState.getState());
        }).join();
        
        // Получаем состояние на момент создания
        Instant createTime = createEvent.getTimestamp();
        CompletableFuture<Entity> createTimeStateFuture = processor.getEntityStateAt(userId, createTime);
        
        createTimeStateFuture.thenAccept(createTimeState -> {
            System.out.println("⏪ Состояние откачено к моменту создания: " + createTimeState.getState());
        }).join();
        
        // Выводим статистику
        System.out.println("\n📊 Статистика системы:");
        System.out.println(processor.getSystemStats());
        
        // Завершаем работу
        processor.shutdown();
        System.out.println("\n🏁 Пример завершен!");
    }
}