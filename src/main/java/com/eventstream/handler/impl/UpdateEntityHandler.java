package com.eventstream.handler.impl;

import com.eventstream.handler.EventHandler;
import com.eventstream.model.Event;
import com.eventstream.model.EntityState;

/**
 * Handler for entity update events
 */
public class UpdateEntityHandler implements EventHandler {
    
    public static final String EVENT_TYPE = "ENTITY_UPDATED";

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public EntityState apply(EntityState currentState, Event event) {
        if (currentState == null) {
            throw new IllegalStateException("Cannot update entity that does not exist: " + event.getEntityId());
        }
        
        // Update existing entity with properties from event payload
        return currentState.withUpdatedProperties(event.getPayload(), event.getTimestamp());
    }
}