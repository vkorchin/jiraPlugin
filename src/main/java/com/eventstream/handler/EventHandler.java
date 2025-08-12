package com.eventstream.handler;

import com.eventstream.model.Event;
import com.eventstream.model.EntityState;

/**
 * Interface for event handlers that process specific event types
 */
public interface EventHandler {
    
    /**
     * Get the event type this handler can process
     * @return event type string
     */
    String getEventType();
    
    /**
     * Apply the event to the entity state
     * @param currentState current state of the entity (can be null for new entities)
     * @param event the event to apply
     * @return new state after applying the event
     */
    EntityState apply(EntityState currentState, Event event);
    
    /**
     * Check if this handler can process the given event
     * @param event the event to check
     * @return true if this handler can process the event
     */
    default boolean canHandle(Event event) {
        return getEventType().equals(event.getEventType());
    }
}