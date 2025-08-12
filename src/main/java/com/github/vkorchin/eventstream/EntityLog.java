package com.github.vkorchin.eventstream;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Lock-free append-only log of events per entity, ordered by (timestamp, sequence).
 */
public class EntityLog {
    private final String entityId;
    // key: logical sequence for tie-break; value: Event
    private final NavigableMap<EventKey, Event> events = new ConcurrentSkipListMap<>();

    public EntityLog(String entityId) {
        this.entityId = Objects.requireNonNull(entityId, "entityId");
    }

    public String getEntityId() {
        return entityId;
    }

    public void append(long sequence, Event event) {
        events.put(new EventKey(event.getTimestamp(), sequence), event);
    }

    public List<Event> range(Instant fromInclusive, Instant toInclusive) {
        EventKey from = new EventKey(fromInclusive, Long.MIN_VALUE);
        EventKey to = new EventKey(toInclusive, Long.MAX_VALUE);
        return new ArrayList<>(events.subMap(from, true, to, true).values());
    }

    public List<Event> from(Instant fromInclusive) {
        EventKey from = new EventKey(fromInclusive, Long.MIN_VALUE);
        return new ArrayList<>(events.tailMap(from, true).values());
    }

    public EventKey lastKeyOrNull() {
        return events.isEmpty() ? null : events.lastKey();
    }

    static final class EventKey implements Comparable<EventKey> {
        final Instant timestamp;
        final long sequence;

        EventKey(Instant timestamp, long sequence) {
            this.timestamp = timestamp;
            this.sequence = sequence;
        }

        @Override
        public int compareTo(EventKey other) {
            int cmp = this.timestamp.compareTo(other.timestamp);
            if (cmp != 0) return cmp;
            return Long.compare(this.sequence, other.sequence);
        }
    }
}