package streaming;

/**
 * Represents an immutable or effectively immutable state of an entity.
 * Implementations should be designed for copy-on-write semantics.
 */
public interface EntityState<S extends EntityState<S>> {
    /**
     * Create a deep copy of this state. Mutating the copy must not affect the original.
     */
    S copy();
}