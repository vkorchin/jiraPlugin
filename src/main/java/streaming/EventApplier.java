package streaming;

/**
 * Applies an event to a concrete state type.
 * Implementations are registered by eventType string.
 */
public interface EventApplier<S extends EntityState<S>> {
    /**
     * Apply event to the provided mutable state instance.
     * Implementations may mutate and return the same state instance for efficiency.
     */
    S apply(S state, Event event);
}