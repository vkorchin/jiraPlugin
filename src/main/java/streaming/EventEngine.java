package streaming;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class EventEngine<S extends EntityState<S>> implements AutoCloseable {
    private final VersionedStateStore<S> stateStore;
    private final EventHandlerRegistry<S> registry;
    private final PerKeySerialExecutor<String> perKeyExecutor;
    private final ConcurrentMap<String, S> initialStateByEntity = new ConcurrentHashMap<>();
    private final RollbackStrategy<S> rollbackStrategy;

    public EventEngine(int parallelism,
                       int snapshotEveryNEvents,
                       EventHandlerRegistry<S> registry,
                       RollbackStrategy<S> rollbackStrategy) {
        this.stateStore = new VersionedStateStore<>(snapshotEveryNEvents);
        this.registry = Objects.requireNonNull(registry, "registry");
        this.perKeyExecutor = new PerKeySerialExecutor<>(parallelism);
        this.rollbackStrategy = Objects.requireNonNull(rollbackStrategy, "rollbackStrategy");
    }

    public void ensureEntity(String entityId, S initialState) {
        initialStateByEntity.putIfAbsent(entityId, initialState);
        stateStore.getOrCreateHistory(entityId, initialState);
    }

    public void onEvent(Event event) {
        String entityId = event.getEntityId();
        S initial = initialStateByEntity.get(entityId);
        if (initial == null) {
            throw new IllegalStateException("Entity not initialized: " + entityId);
        }
        perKeyExecutor.execute(entityId, () -> {
            @SuppressWarnings("unchecked")
            VersionedStateStore<S>.EntityHistory<S> history = stateStore.getOrCreateHistory(entityId, initial);
            EventApplier<S> applier = registry.resolve(event.getEventType());
            history.appendEvent(event, applier);
        });
    }

    public CompletableFuture<S> queryStateAt(String entityId, Instant timestamp) {
        CompletableFuture<S> future = new CompletableFuture<>();
        perKeyExecutor.execute(entityId, () -> {
            try {
                S s = rollbackStrategy.getStateAt(entityId, timestamp, this);
                future.complete(s);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public VersionedStateStore<S> getStateStore() {
        return stateStore;
    }

    public EventHandlerRegistry<S> getRegistry() {
        return registry;
    }

    @Override
    public void close() {
        perKeyExecutor.close();
    }
}