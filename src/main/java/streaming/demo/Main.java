package streaming.demo;

import streaming.*;
import streaming.strategy.FastSnapshotRollback;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class Main {
    public static void main(String[] args) throws Exception {
        EventHandlerRegistry<AccountState> registry = new EventHandlerRegistry<>();
        registry.register(AccountEventTypes.DEPOSIT, AccountAppliers.DEPOSIT);
        registry.register(AccountEventTypes.WITHDRAW, AccountAppliers.WITHDRAW);

        try (EventEngine<AccountState> engine = new EventEngine<>(
                Runtime.getRuntime().availableProcessors(),
                5,
                registry,
                new FastSnapshotRollback<>())) {

            String id = "acc-1";
            engine.ensureEntity(id, new AccountState());

            Instant t0 = Instant.now();
            engine.onEvent(event(id, t0.plusMillis(10), AccountEventTypes.DEPOSIT, 10_00));
            engine.onEvent(event(id, t0.plusMillis(20), AccountEventTypes.WITHDRAW, 3_00));
            engine.onEvent(event(id, t0.plusMillis(30), AccountEventTypes.DEPOSIT, 5_00));

            Thread.sleep(50); // let executor drain

            CompletableFuture<AccountState> f = engine.queryStateAt(id, t0.plusMillis(25));
            AccountState at25 = f.get();
            System.out.println("State at t=25ms: " + at25);

            // Continue processing after rollback query
            engine.onEvent(event(id, t0.plusMillis(40), AccountEventTypes.WITHDRAW, 2_00));
            Thread.sleep(50);

            CompletableFuture<AccountState> f2 = engine.queryStateAt(id, t0.plusMillis(50));
            AccountState at50 = f2.get();
            System.out.println("State at t=50ms: " + at50);
        }
    }

    private static Event event(String id, Instant ts, String type, long amountCents) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amountCents", amountCents);
        return new Event(id, ts, type, payload);
        
    }
}