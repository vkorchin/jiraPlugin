package streaming.strategy;

import streaming.*;

import java.time.Instant;
import java.util.Map;
import java.util.NavigableMap;

public final class FastSnapshotRollback<S extends EntityState<S>> implements RollbackStrategy<S> {
    @Override
    public S getStateAt(String entityId, Instant timestamp, EventEngine<S> engine) {
        VersionedStateStore<S> store = engine.getStateStore();
        VersionedStateStore<S>.EntityHistory<S> history = store.getHistoryOrThrow(entityId);
        S base = history.baselineSnapshotAtOrBefore(timestamp);
        Instant fromTs = history.baselineTimestampAtOrBefore(timestamp);
        NavigableMap<Instant, Event> log = history.eventLogView();
        for (Map.Entry<Instant, Event> e : log.subMap(fromTs, false, timestamp, true).entrySet()) {
            if (e.getKey().isAfter(timestamp)) break;
            Event ev = e.getValue();
            EventApplier<S> applier = engine.getRegistry().resolve(ev.getEventType());
            base = applier.apply(base, ev);
        }
        return base;
    }
}