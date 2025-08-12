package com.eventstream.handlers;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Базовый обработчик событий по умолчанию
 */
public class DefaultEventHandler implements EventHandler {
    
    private final String supportedEventType;
    
    public DefaultEventHandler(String supportedEventType) {
        this.supportedEventType = supportedEventType;
    }
    
    @Override
    public boolean canHandle(String eventType) {
        return supportedEventType.equals(eventType);
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        if (currentEntity == null) {
            currentEntity = new Entity(event.getEntityId());
        }
        
        // Создаем копию состояния для обновления
        Entity updatedEntity = currentEntity.copy();
        
        // Обновляем состояние на основе payload события
        Map<String, Object> updates = new HashMap<>();
        for (Map.Entry<String, Object> entry : event.getPayload().entrySet()) {
            updates.put(entry.getKey(), entry.getValue());
        }
        
        updatedEntity.updateState(updates);
        
        return updatedEntity;
    }
    
    @Override
    public Entity rollback(Event event, Entity currentEntity) {
        if (currentEntity == null) {
            return null;
        }
        
        // Для простого отката просто возвращаем предыдущее состояние
        // В реальной реализации здесь должна быть логика восстановления
        return currentEntity;
    }
}