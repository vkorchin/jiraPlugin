package com.eventstream.handlers;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;

import java.time.Instant;
import java.util.Map;

/**
 * Базовый обработчик событий по умолчанию
 * Обрабатывает события, обновляя состояние сущности
 */
public class DefaultEventProcessor implements EventProcessor {
    
    private final String eventType;
    
    public DefaultEventProcessor(String eventType) {
        this.eventType = eventType;
    }
    
    @Override
    public boolean canHandle(String eventType) {
        return this.eventType.equals(eventType);
    }
    
    @Override
    public void handle(Event event, Entity entity) {
        // Обновляем состояние сущности данными из события
        Map<String, Object> payload = event.getPayload();
        if (payload != null && !payload.isEmpty()) {
            entity.updateState(payload);
        }
        
        // Обновляем метаданные
        entity.updateState("lastEventTimestamp", event.getTimestamp());
        entity.updateState("lastEventType", event.getEventType());
        entity.updateState("lastEventSequence", event.getSequenceNumber());
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
}