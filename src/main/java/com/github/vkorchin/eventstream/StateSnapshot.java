package com.github.vkorchin.eventstream;

import java.time.Instant;

public final class StateSnapshot<S extends EntityState<S>> {
    private final String entityId;
    private final long sequence;
    private final Instant timestamp;
    private final S state;

    public StateSnapshot(String entityId, long sequence, Instant timestamp, S state) {
        this.entityId = entityId;
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.state = state;
    }

    public String getEntityId() {
        return entityId;
    }

    public long getSequence() {
        return sequence;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public S getState() {
        return state;
    }
}