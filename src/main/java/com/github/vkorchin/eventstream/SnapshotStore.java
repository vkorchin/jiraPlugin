package com.github.vkorchin.eventstream;

import java.time.Instant;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class SnapshotStore<S extends EntityState<S>> {
    private final ConcurrentMap<String, NavigableMap<EntityLog.EventKey, StateSnapshot<S>>> perEntity = new ConcurrentHashMap<>();

    public void put(StateSnapshot<S> snapshot) {
        perEntity.computeIfAbsent(snapshot.getEntityId(), id -> new ConcurrentSkipListMap<>())
                .put(new EntityLog.EventKey(snapshot.getTimestamp(), snapshot.getSequence()), snapshot);
    }

    public StateSnapshot<S> floor(String entityId, Instant timestamp) {
        NavigableMap<EntityLog.EventKey, StateSnapshot<S>> map = perEntity.get(entityId);
        if (map == null) return null;
        EntityLog.EventKey key = new EntityLog.EventKey(Objects.requireNonNull(timestamp), Long.MAX_VALUE);
        var entry = map.floorEntry(key);
        return entry != null ? entry.getValue() : null;
    }
}