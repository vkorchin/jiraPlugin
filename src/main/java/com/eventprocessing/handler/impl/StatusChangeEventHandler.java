package com.eventprocessing.handler.impl;

import com.eventprocessing.handler.EventHandler;
import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик событий изменения статуса сущностей
 */
public class StatusChangeEventHandler implements EventHandler {
    
    @Override
    public String getEventType() {
        return "STATUS_CHANGE";
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        // Если сущности не существует, создаем новую
        Entity entity = currentEntity != null ? currentEntity : new Entity(event.getEntityId());
        
        // Обновляем статус и добавляем информацию из payload
        Map<String, Object> updates = new HashMap<>();
        if (event.getPayload() != null) {
            updates.putAll(event.getPayload());
        }
        
        // Если в payload есть статус, записываем его
        if (updates.containsKey("status")) {
            Object previousStatus = entity.getStateValue("status");
            if (previousStatus != null) {
                updates.put("previousStatus", previousStatus);
            }
            updates.put("statusChangedAt", event.getTimestamp().toString());
        }
        
        return entity.withUpdatedState(updates, event.getTimestamp());
    }
}