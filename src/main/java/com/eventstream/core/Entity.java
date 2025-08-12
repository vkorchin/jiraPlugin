package com.eventstream.core;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Представляет состояние сущности в определенный момент времени
 */
public class Entity {
    private final String id;
    private final Map<String, Object> state;
    private final Instant lastUpdated;
    private final long lastEventSequence;
    private final AtomicLong version;

    public Entity(String id) {
        this.id = id;
        this.state = new ConcurrentHashMap<>();
        this.lastUpdated = Instant.now();
        this.lastEventSequence = 0;
        this.version = new AtomicLong(0);
    }

    public Entity(String id, Map<String, Object> state, Instant lastUpdated, long lastEventSequence, long version) {
        this.id = id;
        this.state = new ConcurrentHashMap<>(state);
        this.lastUpdated = lastUpdated;
        this.lastEventSequence = lastEventSequence;
        this.version = new AtomicLong(version);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getState() {
        return new ConcurrentHashMap<>(state);
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public long getLastEventSequence() {
        return lastEventSequence;
    }

    public long getVersion() {
        return version.get();
    }

    public void updateState(String key, Object value) {
        state.put(key, value);
        version.incrementAndGet();
    }

    public void updateState(Map<String, Object> updates) {
        state.putAll(updates);
        version.incrementAndGet();
    }

    public Object getStateValue(String key) {
        return state.get(key);
    }

    public boolean hasStateKey(String key) {
        return state.containsKey(key);
    }

    /**
     * Создает копию текущего состояния
     */
    public Entity copy() {
        return new Entity(id, state, lastUpdated, lastEventSequence, version.get());
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                ", state=" + state +
                ", lastUpdated=" + lastUpdated +
                ", lastEventSequence=" + lastEventSequence +
                ", version=" + version.get() +
                '}';
    }
}