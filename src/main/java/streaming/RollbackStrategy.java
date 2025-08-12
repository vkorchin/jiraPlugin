package streaming;

import java.time.Instant;

public interface RollbackStrategy<S extends EntityState<S>> {
    S getStateAt(String entityId, Instant timestamp, EventEngine<S> engine);
}