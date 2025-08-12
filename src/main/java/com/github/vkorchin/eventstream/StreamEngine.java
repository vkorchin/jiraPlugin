package com.github.vkorchin.eventstream;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class StreamEngine<S extends EntityState<S>> implements AutoCloseable {
    private final StateStore<S> stateStore;
    private final Supplier<S> initialStateSupplier;
    private final ExecutorService ingestPool;

    public StreamEngine(StateStore<S> stateStore, Supplier<S> initialStateSupplier, int ingestThreads) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.initialStateSupplier = Objects.requireNonNull(initialStateSupplier);
        this.ingestPool = Executors.newFixedThreadPool(Math.max(1, ingestThreads));
    }

    public void publish(Event event) {
        ingestPool.submit(() -> stateStore.ingest(event, initialStateSupplier.get()));
    }

    @Override
    public void close() {
        ingestPool.shutdown();
        try {
            ingestPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        stateStore.close();
    }
}