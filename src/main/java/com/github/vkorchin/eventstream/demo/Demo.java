package com.github.vkorchin.eventstream.demo;

import com.github.vkorchin.eventstream.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Demo {
    public static void main(String[] args) throws Exception {
        EventDispatcher<AccountState> dispatcher = new EventDispatcher<>();
        // Default dispatcher delegates to state's own apply to keep open for new event types
        dispatcher.register("deposit", (s, e) -> s.apply(e));
        dispatcher.register("withdraw", (s, e) -> s.apply(e));
        dispatcher.register("setMeta", (s, e) -> s.apply(e));

        SnapshotStore<AccountState> snapshotStore = new SnapshotStore<>();
        StateStore<AccountState> stateStore = new StateStore<>(dispatcher, snapshotStore, 3);

        try (StreamEngine<AccountState> engine = new StreamEngine<>(stateStore, AccountState::new, Runtime.getRuntime().availableProcessors())) {
            String id = "A-1";
            Instant t1 = Instant.now();
            publish(engine, id, t1, "deposit", mapOf("amount", 100));
            Thread.sleep(10);
            Instant t2 = Instant.now();
            publish(engine, id, t2, "withdraw", mapOf("amount", 50));
            Thread.sleep(10);
            Instant t3 = Instant.now();
            publish(engine, id, t3, "setMeta", mapOf("key", "owner", "value", "Bob"));

            // Late out-of-order event: should trigger rebuild
            Thread.sleep(10);
            Instant tLate = t2.minusMillis(5);
            publish(engine, id, tLate, "deposit", mapOf("amount", 10));

            Thread.sleep(100);

            System.out.println("Current: " + stateStore.getCurrentState(id));
            System.out.println("At t2:   " + stateStore.getStateAt(id, t2, new AccountState()));
            System.out.println("At t3:   " + stateStore.getStateAt(id, t3, new AccountState()));
        }
    }

    private static void publish(StreamEngine<AccountState> engine, String id, Instant ts, String type, Map<String, Object> payload) {
        engine.publish(new Event(id, ts, type, payload));
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i].toString(), kv[i + 1]);
        }
        return m;
    }
}