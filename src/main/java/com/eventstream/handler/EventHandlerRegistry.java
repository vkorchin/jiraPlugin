package com.eventstream.handler;

import com.eventstream.model.Event;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Registry for event handlers with thread-safe registration and lookup
 */
public class EventHandlerRegistry {
    private static final Logger logger = Logger.getLogger(EventHandlerRegistry.class.getName());
    
    private final ConcurrentMap<String, EventHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Register an event handler
     * @param handler the handler to register
     * @throws IllegalArgumentException if a handler for this event type is already registered
     */
    public void registerHandler(EventHandler handler) {
        String eventType = handler.getEventType();
        EventHandler existingHandler = handlers.putIfAbsent(eventType, handler);
        
        if (existingHandler != null) {
            throw new IllegalArgumentException("Handler for event type '" + eventType + "' is already registered");
        }
        
        logger.info("Registered handler for event type: " + eventType);
    }

    /**
     * Get handler for a specific event type
     * @param eventType the event type
     * @return the handler or null if not found
     */
    public EventHandler getHandler(String eventType) {
        return handlers.get(eventType);
    }

    /**
     * Get handler for a specific event
     * @param event the event
     * @return the handler or null if not found
     */
    public EventHandler getHandler(Event event) {
        return getHandler(event.getEventType());
    }

    /**
     * Check if a handler is registered for the event type
     * @param eventType the event type
     * @return true if a handler is registered
     */
    public boolean hasHandler(String eventType) {
        return handlers.containsKey(eventType);
    }

    /**
     * Check if a handler is registered for the event
     * @param event the event
     * @return true if a handler is registered
     */
    public boolean hasHandler(Event event) {
        return hasHandler(event.getEventType());
    }

    /**
     * Unregister a handler for an event type
     * @param eventType the event type
     * @return the unregistered handler or null if none was registered
     */
    public EventHandler unregisterHandler(String eventType) {
        EventHandler handler = handlers.remove(eventType);
        if (handler != null) {
            logger.info("Unregistered handler for event type: " + eventType);
        }
        return handler;
    }

    /**
     * Get the number of registered handlers
     * @return number of handlers
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Clear all registered handlers
     */
    public void clear() {
        handlers.clear();
        logger.info("Cleared all registered handlers");
    }
}