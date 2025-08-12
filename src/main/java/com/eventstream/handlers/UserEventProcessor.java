package com.eventstream.handlers;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;

import java.util.Map;

/**
 * Обработчик событий пользователей
 * Демонстрирует расширяемость системы без изменения базового кода
 */
public class UserEventProcessor implements EventProcessor {
    
    @Override
    public boolean canHandle(String eventType) {
        return eventType.startsWith("user.");
    }
    
    @Override
    public void handle(Event event, Entity entity) {
        Map<String, Object> payload = event.getPayload();
        
        switch (event.getEventType()) {
            case "user.created":
                handleUserCreated(event, entity, payload);
                break;
            case "user.updated":
                handleUserUpdated(event, entity, payload);
                break;
            case "user.deleted":
                handleUserDeleted(event, entity, payload);
                break;
            default:
                // Обрабатываем как обычное событие
                if (payload != null && !payload.isEmpty()) {
                    entity.updateState(payload);
                }
                break;
        }
        
        // Обновляем метаданные
        entity.updateState("lastUserEvent", event.getEventType());
        entity.updateState("lastUserEventTimestamp", event.getTimestamp());
    }
    
    private void handleUserCreated(Event event, Entity entity, Map<String, Object> payload) {
        if (payload != null) {
            entity.updateState("status", "active");
            entity.updateState("createdAt", event.getTimestamp());
            entity.updateState("userData", payload);
        }
    }
    
    private void handleUserUpdated(Event event, Entity entity, Map<String, Object> payload) {
        if (payload != null) {
            entity.updateState("updatedAt", event.getTimestamp());
            entity.updateState("userData", payload);
        }
    }
    
    private void handleUserDeleted(Event event, Entity entity, Map<String, Object> payload) {
        entity.updateState("status", "deleted");
        entity.updateState("deletedAt", event.getTimestamp());
    }
    
    @Override
    public int getPriority() {
        return 50; // Высокий приоритет для пользовательских событий
    }
}