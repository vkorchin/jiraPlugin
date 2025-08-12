package com.github.vkorchin.eventstream;

@FunctionalInterface
public interface EntityEventHandler<S extends EntityState<S>> {
    S onEvent(S state, Event event);
}