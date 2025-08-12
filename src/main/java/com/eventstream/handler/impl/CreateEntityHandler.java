package com.eventstream.handler.impl;

import com.eventstream.handler.EventHandler;
import com.eventstream.model.Event;
import com.eventstream.model.EntityState;

/**
 * Handler for entity creation events
 */
public class CreateEntityHandler implements EventHandler {
    
    public static final String EVENT_TYPE = "ENTITY_CREATED";

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public EntityState apply(EntityState currentState, Event event) {
        if (currentState != null) {
            throw new IllegalStateException("Cannot create entity that already exists: " + event.getEntityId());
        }
        
        // Create new entity with properties from event payload
        EntityState newState = new EntityState(event.getEntityId());
        return newState.withUpdatedProperties(event.getPayload(), event.getTimestamp());
    }
}