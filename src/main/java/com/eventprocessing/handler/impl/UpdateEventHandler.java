package com.eventprocessing.handler.impl;

import com.eventprocessing.handler.EventHandler;
import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;

/**
 * Обработчик событий обновления сущностей
 */
public class UpdateEventHandler implements EventHandler {
    
    @Override
    public String getEventType() {
        return "UPDATE";
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        // Если сущности не существует, создаем новую
        Entity entity = currentEntity != null ? currentEntity : new Entity(event.getEntityId());
        
        // Обновляем состояние сущности данными из события
        return entity.withUpdatedState(event.getPayload(), event.getTimestamp());
    }
}