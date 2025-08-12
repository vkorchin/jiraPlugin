package com.eventstream.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable entity state with copy-on-write semantics
 */
public final class EntityState {
    private final String entityId;
    private final Map<String, Object> properties;
    private final Instant lastUpdated;
    private final long version;

    public EntityState(String entityId) {
        this(entityId, Collections.emptyMap(), Instant.now(), 0L);
    }

    public EntityState(String entityId, Map<String, Object> properties, 
                      Instant lastUpdated, long version) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.properties = new ConcurrentHashMap<>(properties);
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "lastUpdated cannot be null");
        this.version = version;
    }

    /**
     * Creates a new state with updated properties (copy-on-write)
     */
    public EntityState withUpdatedProperties(Map<String, Object> newProperties, Instant timestamp) {
        Map<String, Object> updatedProps = new ConcurrentHashMap<>(this.properties);
        updatedProps.putAll(newProperties);
        return new EntityState(entityId, updatedProps, timestamp, version + 1);
    }

    /**
     * Creates a new state with a single property update
     */
    public EntityState withProperty(String key, Object value, Instant timestamp) {
        Map<String, Object> updatedProps = new ConcurrentHashMap<>(this.properties);
        updatedProps.put(key, value);
        return new EntityState(entityId, updatedProps, timestamp, version + 1);
    }

    /**
     * Creates a new state with a property removed
     */
    public EntityState withoutProperty(String key, Instant timestamp) {
        Map<String, Object> updatedProps = new ConcurrentHashMap<>(this.properties);
        updatedProps.remove(key);
        return new EntityState(entityId, updatedProps, timestamp, version + 1);
    }

    public String getEntityId() {
        return entityId;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public long getVersion() {
        return version;
    }

    /**
     * Deep copy for snapshot purposes
     */
    public EntityState deepCopy() {
        return new EntityState(entityId, new ConcurrentHashMap<>(properties), lastUpdated, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityState that = (EntityState) o;
        return version == that.version &&
                Objects.equals(entityId, that.entityId) &&
                Objects.equals(properties, that.properties) &&
                Objects.equals(lastUpdated, that.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, properties, lastUpdated, version);
    }

    @Override
    public String toString() {
        return "EntityState{" +
                "entityId='" + entityId + '\'' +
                ", properties=" + properties +
                ", lastUpdated=" + lastUpdated +
                ", version=" + version +
                '}';
    }
}