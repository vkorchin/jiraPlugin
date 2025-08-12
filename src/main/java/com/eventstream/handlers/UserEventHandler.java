package com.eventstream.handlers;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;
import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик событий для пользователей
 */
public class UserEventHandler implements EventHandler {
    
    @Override
    public boolean canHandle(String eventType) {
        return "USER_CREATED".equals(eventType) || 
               "USER_UPDATED".equals(eventType) || 
               "USER_DELETED".equals(eventType);
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        if (currentEntity == null) {
            currentEntity = new Entity(event.getEntityId());
        }
        
        Entity updatedEntity = currentEntity.copy();
        
        switch (event.getEventType()) {
            case "USER_CREATED":
                handleUserCreated(event, updatedEntity);
                break;
            case "USER_UPDATED":
                handleUserUpdated(event, updatedEntity);
                break;
            case "USER_DELETED":
                handleUserDeleted(event, updatedEntity);
                break;
        }
        
        return updatedEntity;
    }
    
    @Override
    public Entity rollback(Event event, Entity currentEntity) {
        if (currentEntity == null) {
            return null;
        }
        
        Entity rolledBackEntity = currentEntity.copy();
        
        switch (event.getEventType()) {
            case "USER_CREATED":
                // Откат создания - удаляем сущность
                return null;
            case "USER_UPDATED":
                // Откат обновления - восстанавливаем предыдущие значения
                handleUserUpdatedRollback(event, rolledBackEntity);
                break;
            case "USER_DELETED":
                // Откат удаления - восстанавливаем сущность
                handleUserDeletedRollback(event, rolledBackEntity);
                break;
        }
        
        return rolledBackEntity;
    }
    
    private void handleUserCreated(Event event, Entity entity) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("status", "ACTIVE");
        userData.put("createdAt", event.getTimestamp());
        userData.putAll(event.getPayload());
        entity.updateState(userData);
    }
    
    private void handleUserUpdated(Event event, Entity entity) {
        entity.updateState(event.getPayload());
    }
    
    private void handleUserDeleted(Event event, Entity entity) {
        entity.updateState("status", "DELETED");
        entity.updateState("deletedAt", event.getTimestamp());
    }
    
    private void handleUserUpdatedRollback(Event event, Entity entity) {
        // В реальной реализации здесь должна быть логика восстановления
        // предыдущих значений из payload события
        Map<String, Object> previousValues = (Map<String, Object>) event.getPayload().get("previousValues");
        if (previousValues != null) {
            entity.updateState(previousValues);
        }
    }
    
    private void handleUserDeletedRollback(Event event, Entity entity) {
        entity.updateState("status", "ACTIVE");
        entity.updateState("deletedAt", null);
    }
}