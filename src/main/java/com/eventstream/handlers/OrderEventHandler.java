package com.eventstream.handlers;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик событий для заказов
 */
public class OrderEventHandler implements EventHandler {
    
    @Override
    public boolean canHandle(String eventType) {
        return "ORDER_CREATED".equals(eventType) || 
               "ORDER_UPDATED".equals(eventType) || 
               "ORDER_CANCELLED".equals(eventType) ||
               "ORDER_COMPLETED".equals(eventType);
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        if (currentEntity == null) {
            currentEntity = new Entity(event.getEntityId());
        }
        
        Entity updatedEntity = currentEntity.copy();
        
        switch (event.getEventType()) {
            case "ORDER_CREATED":
                handleOrderCreated(event, updatedEntity);
                break;
            case "ORDER_UPDATED":
                handleOrderUpdated(event, updatedEntity);
                break;
            case "ORDER_CANCELLED":
                handleOrderCancelled(event, updatedEntity);
                break;
            case "ORDER_COMPLETED":
                handleOrderCompleted(event, updatedEntity);
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
            case "ORDER_CREATED":
                // Откат создания заказа
                return null;
            case "ORDER_UPDATED":
                // Откат обновления заказа
                handleOrderUpdatedRollback(event, rolledBackEntity);
                break;
            case "ORDER_CANCELLED":
                // Откат отмены заказа
                handleOrderCancelledRollback(event, rolledBackEntity);
                break;
            case "ORDER_COMPLETED":
                // Откат завершения заказа
                handleOrderCompletedRollback(event, rolledBackEntity);
                break;
        }
        
        return rolledBackEntity;
    }
    
    private void handleOrderCreated(Event event, Entity entity) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("status", "CREATED");
        orderData.put("createdAt", event.getTimestamp());
        orderData.putAll(event.getPayload());
        entity.updateState(orderData);
    }
    
    private void handleOrderUpdated(Event event, Entity entity) {
        entity.updateState(event.getPayload());
    }
    
    private void handleOrderCancelled(Event event, Entity entity) {
        entity.updateState("status", "CANCELLED");
        entity.updateState("cancelledAt", event.getTimestamp());
        entity.updateState("cancellationReason", event.getPayload().get("reason"));
    }
    
    private void handleOrderCompleted(Event event, Entity entity) {
        entity.updateState("status", "COMPLETED");
        entity.updateState("completedAt", event.getTimestamp());
        entity.updateState("completionNotes", event.getPayload().get("notes"));
    }
    
    private void handleOrderUpdatedRollback(Event event, Entity entity) {
        // Восстанавливаем предыдущие значения
        Map<String, Object> previousValues = (Map<String, Object>) event.getPayload().get("previousValues");
        if (previousValues != null) {
            entity.updateState(previousValues);
        }
    }
    
    private void handleOrderCancelledRollback(Event event, Entity entity) {
        // Восстанавливаем статус заказа
        entity.updateState("status", "ACTIVE");
        entity.updateState("cancelledAt", null);
        entity.updateState("cancellationReason", null);
    }
    
    private void handleOrderCompletedRollback(Event event, Entity entity) {
        // Восстанавливаем статус заказа
        entity.updateState("status", "ACTIVE");
        entity.updateState("completedAt", null);
        entity.updateState("completionNotes", null);
    }
}