package streaming.strategy;

import streaming.*;

import java.time.Instant;
import java.util.Map;

public final class FullRebuildRollback<S extends EntityState<S>> implements RollbackStrategy<S> {
    @Override
    public S getStateAt(String entityId, Instant timestamp, EventEngine<S> engine) {
        VersionedStateStore<S>.EntityHistory<S> history = engine.getStateStore().getHistoryOrThrow(entityId);
        S state = history.baselineSnapshotAtOrBefore(Instant.EPOCH);
        for (Map.Entry<Instant, Event> e : history.eventLogView().headMap(timestamp, true).entrySet()) {
            if (e.getKey().isAfter(timestamp)) break;
            state = engine.getRegistry().resolve(e.getValue().getEventType()).apply(state, e.getValue());
        }
        return state;
    }
}