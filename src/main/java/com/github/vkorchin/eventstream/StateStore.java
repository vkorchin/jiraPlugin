package com.github.vkorchin.eventstream;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrent state store that applies events, maintains per-entity logs and snapshots, and supports rollbacks.
 */
public class StateStore<S extends EntityState<S>> implements AutoCloseable {
    private final EventDispatcher<S> dispatcher;
    private final SnapshotStore<S> snapshotStore;

    private final ConcurrentMap<String, EntityContext> entityContexts = new ConcurrentHashMap<>();

    private final int snapshotEveryNEvents;

    public StateStore(EventDispatcher<S> dispatcher, SnapshotStore<S> snapshotStore, int snapshotEveryNEvents) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.snapshotStore = Objects.requireNonNull(snapshotStore);
        this.snapshotEveryNEvents = snapshotEveryNEvents;
    }

    public void ingest(Event event, S initialState) {
        entityContexts.computeIfAbsent(event.getEntityId(), id -> new EntityContext(id, initialState.copy()))
                .submit(event);
    }

    public S getCurrentState(String entityId) {
        EntityContext ctx = entityContexts.get(entityId);
        if (ctx == null) return null;
        return ctx.readCurrentState();
    }

    public S getStateAt(String entityId, Instant timestamp, S initialState) {
        EntityContext ctx = entityContexts.computeIfAbsent(entityId, id -> new EntityContext(id, initialState.copy()));
        return ctx.computeStateAt(timestamp, initialState);
    }

    @Override
    public void close() {
        entityContexts.values().forEach(EntityContext::shutdown);
    }

    private final class EntityContext {
        private final String entityId;
        private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        private final ExecutorService singleExecutor;
        private final AtomicLong sequenceCounter = new AtomicLong();

        private final EntityLog log;
        private volatile S currentState;
        private volatile Instant currentTimestamp = Instant.EPOCH;
        private long numAppliedSinceSnapshot = 0L;

        EntityContext(String entityId, S initialState) {
            this.entityId = entityId;
            this.singleExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "entity-processor-" + this.entityId));
            this.currentState = initialState;
            this.log = new EntityLog(entityId);
            startLoop();
        }

        void startLoop() {
            singleExecutor.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Event event = queue.take();
                        applyEvent(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        void submit(Event event) {
            queue.offer(event);
        }

        S readCurrentState() {
            return currentState.copy();
        }

        void applyEvent(Event event) {
            long seq = sequenceCounter.incrementAndGet();
            // Handle late/out-of-order by placing into log with timestamp ordering
            log.append(seq, event);
            // If event is older than current, we still update current by reapplying from latest snapshot + tail
            if (event.getTimestamp().isBefore(currentTimestamp)) {
                // Rebuild fast from nearest snapshot
                rebuildFromLatestSnapshot();
                return;
            }
            // Normal forward application
            currentState = dispatcher.dispatch(currentState, event);
            currentTimestamp = event.getTimestamp();
            numAppliedSinceSnapshot++;
            maybeSnapshot(seq);
        }

        void rebuildFromLatestSnapshot() {
            StateSnapshot<S> snap = snapshotStore.floor(entityId, Instant.now());
            S base = snap != null ? snap.getState().copy() : currentState.copy();
            Instant fromTs = snap != null ? snap.getTimestamp() : Instant.EPOCH;
            List<Event> tail = log.from(fromTs);
            S rebuilt = base;
            Instant ts = fromTs;
            for (Event e : tail) {
                rebuilt = dispatcher.dispatch(rebuilt, e);
                ts = e.getTimestamp();
            }
            currentState = rebuilt;
            currentTimestamp = ts;
        }

        S computeStateAt(Instant timestamp, S initialState) {
            // Strategy: use floor snapshot, then apply events up to timestamp
            StateSnapshot<S> snap = snapshotStore.floor(entityId, timestamp);
            S base = snap != null ? snap.getState().copy() : initialState.copy();
            Instant fromTs = snap != null ? snap.getTimestamp() : Instant.EPOCH;
            List<Event> events = log.range(fromTs, timestamp);
            S res = base;
            for (Event e : events) {
                res = dispatcher.dispatch(res, e);
            }
            return res;
        }

        void maybeSnapshot(long sequence) {
            if (snapshotEveryNEvents <= 0) return;
            if (numAppliedSinceSnapshot >= snapshotEveryNEvents) {
                snapshotStore.put(new StateSnapshot<>(entityId, sequence, currentTimestamp, currentState.copy()));
                numAppliedSinceSnapshot = 0L;
            }
        }

        void shutdown() {
            singleExecutor.shutdownNow();
        }
    }
}