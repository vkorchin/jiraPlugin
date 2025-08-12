package com.github.vkorchin.eventstream;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class EventDispatcher<S extends EntityState<S>> {
    private final Map<String, EntityEventHandler<S>> handlers = new ConcurrentHashMap<>();

    public void register(String eventType, EntityEventHandler<S> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        handlers.put(eventType, handler);
    }

    public S dispatch(S state, Event event) {
        EntityEventHandler<S> handler = handlers.get(event.getEventType());
        if (handler == null) {
            return state; // unknown event is ignored by default
        }
        return handler.onEvent(state, event);
    }
}