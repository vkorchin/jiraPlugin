package com.eventprocessing.handler.impl;

import com.eventprocessing.handler.EventHandler;
import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик событий удаления сущностей
 */
public class DeleteEventHandler implements EventHandler {
    
    @Override
    public String getEventType() {
        return "DELETE";
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        // Если сущности не существует, создаем новую с флагом deleted
        Entity entity = currentEntity != null ? currentEntity : new Entity(event.getEntityId());
        
        // Помечаем сущность как удаленную и добавляем информацию из payload
        Map<String, Object> updates = new HashMap<>(event.getPayload());
        updates.put("deleted", true);
        updates.put("deletedAt", event.getTimestamp().toString());
        
        return entity.withUpdatedState(updates, event.getTimestamp());
    }
}