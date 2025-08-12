package com.eventprocessing.handler.impl;

import com.eventprocessing.handler.EventHandler;
import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;

/**
 * Обработчик событий создания сущностей
 */
public class CreateEventHandler implements EventHandler {
    
    @Override
    public String getEventType() {
        return "CREATE";
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        // Если сущность уже существует, просто обновляем её состояние
        Entity entity = currentEntity != null ? currentEntity : new Entity(event.getEntityId());
        
        // Добавляем все поля из payload в состояние сущности
        return entity.withUpdatedState(event.getPayload(), event.getTimestamp());
    }
}