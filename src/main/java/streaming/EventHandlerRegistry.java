package streaming;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class EventHandlerRegistry<S extends EntityState<S>> {
    private final Map<String, EventApplier<S>> eventTypeToApplier = new ConcurrentHashMap<>();

    public void register(String eventType, EventApplier<S> applier) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(applier, "applier");
        eventTypeToApplier.put(eventType, applier);
    }

    public EventApplier<S> resolve(String eventType) {
        EventApplier<S> applier = eventTypeToApplier.get(eventType);
        if (applier == null) {
            throw new IllegalArgumentException("No applier registered for eventType=" + eventType);
        }
        return applier;
    }
}