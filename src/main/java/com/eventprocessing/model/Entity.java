package com.eventprocessing.model;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Модель сущности с текущим состоянием
 * Поддерживает copy-on-write для потокобезопасности
 */
public class Entity {
    private final String id;
    private final Map<String, Object> state;
    private final Instant lastUpdated;

    public Entity(String id) {
        this.id = id;
        this.state = new ConcurrentHashMap<>();
        this.lastUpdated = Instant.now();
    }

    private Entity(String id, Map<String, Object> state, Instant lastUpdated) {
        this.id = id;
        this.state = new ConcurrentHashMap<>(state);
        this.lastUpdated = lastUpdated;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getState() {
        return Map.copyOf(state);
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public Object getStateValue(String key) {
        return state.get(key);
    }

    /**
     * Создает новую копию сущности с обновленным состоянием
     * Copy-on-write подход для потокобезопасности
     */
    public Entity withUpdatedState(String key, Object value, Instant timestamp) {
        Entity newEntity = new Entity(this.id, this.state, timestamp);
        newEntity.state.put(key, value);
        return newEntity;
    }

    /**
     * Создает новую копию сущности с множественными обновлениями состояния
     */
    public Entity withUpdatedState(Map<String, Object> updates, Instant timestamp) {
        Entity newEntity = new Entity(this.id, this.state, timestamp);
        newEntity.state.putAll(updates);
        return newEntity;
    }

    /**
     * Создает полную копию сущности
     */
    public Entity copy() {
        return new Entity(this.id, this.state, this.lastUpdated);
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                ", state=" + state +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}