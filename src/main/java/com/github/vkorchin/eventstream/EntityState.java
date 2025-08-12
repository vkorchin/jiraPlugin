package com.github.vkorchin.eventstream;

public interface EntityState<S extends EntityState<S>> extends Cloneable {
    S apply(Event event);
    S copy();
}