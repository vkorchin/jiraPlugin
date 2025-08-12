package streaming;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Per-entity state store that keeps snapshots and per-entity event logs.
 */
public final class VersionedStateStore<S extends EntityState<S>> {
    private final ConcurrentMap<String, EntityHistory<S>> histories = new ConcurrentHashMap<>();
    private final int snapshotEveryNEvents;

    public VersionedStateStore(int snapshotEveryNEvents) {
        if (snapshotEveryNEvents <= 0) throw new IllegalArgumentException("snapshotEveryNEvents must be > 0");
        this.snapshotEveryNEvents = snapshotEveryNEvents;
    }

    public EntityHistory<S> getOrCreateHistory(String entityId, S initialState) {
        return histories.computeIfAbsent(entityId, id -> new EntityHistory<>(initialState));
    }

    public EntityHistory<S> getHistoryOrThrow(String entityId) {
        EntityHistory<S> history = histories.get(entityId);
        if (history == null) {
            throw new IllegalArgumentException("Unknown entityId: " + entityId);
        }
        return history;
    }

    public EntityHistory<S> getHistoryOrNull(String entityId) {
        return histories.get(entityId);
    }

    public final class EntityHistory<T extends EntityState<T>> {
        private final NavigableMap<Instant, T> snapshots = new ConcurrentSkipListMap<>();
        private final NavigableMap<Instant, Event> events = new ConcurrentSkipListMap<>();

        private volatile T latestState;
        private volatile Instant latestTimestamp = Instant.EPOCH;
        private int eventsSinceSnapshot = 0;

        EntityHistory(T initialState) {
            this.latestState = Objects.requireNonNull(initialState, "initialState");
            this.snapshots.put(latestTimestamp, initialState.copy());
        }

        public synchronized void appendEvent(Event event, EventApplier<T> applier) {
            if (event.getTimestamp().isBefore(latestTimestamp)) {
                // Late event: insert into log; do not mutate latestState here. Leave to rollback reconstructions.
                events.put(event.getTimestamp(), event);
                return;
            }
            // Apply in order for real-time progression
            T next = latestState.copy();
            next = applier.apply(next, event);
            latestState = next;
            latestTimestamp = event.getTimestamp();
            events.put(event.getTimestamp(), event);
            eventsSinceSnapshot++;
            if (eventsSinceSnapshot >= snapshotEveryNEvents) {
                snapshots.put(latestTimestamp, latestState.copy());
                eventsSinceSnapshot = 0;
            }
        }

        public T getLatestStateSnapshot() {
            return latestState;
        }

        public T baselineSnapshotAtOrBefore(Instant timestamp) {
            var floor = snapshots.floorEntry(timestamp);
            if (floor != null) {
                return floor.getValue().copy();
            }
            return snapshots.firstEntry() != null ? snapshots.firstEntry().getValue().copy() : latestState.copy();
        }

        public Instant baselineTimestampAtOrBefore(Instant timestamp) {
            var floor = snapshots.floorEntry(timestamp);
            if (floor != null) return floor.getKey();
            return snapshots.firstEntry() != null ? snapshots.firstEntry().getKey() : Instant.EPOCH;
        }

        public NavigableMap<Instant, Event> eventLogView() {
            return events;
        }

        public NavigableMap<Instant, T> snapshotView() {
            return snapshots;
        }
    }
}