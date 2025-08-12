package streaming.demo;

import streaming.Event;
import streaming.EventApplier;

public final class AccountAppliers {
    private AccountAppliers() {}

    public static final EventApplier<AccountState> DEPOSIT = (state, event) -> {
        long amount = ((Number) event.getPayload().getOrDefault("amountCents", 0)).longValue();
        long newBalance = state.getBalanceCents() + amount;
        return new AccountState(newBalance, state.getVersion() + 1);
    };

    public static final EventApplier<AccountState> WITHDRAW = (state, event) -> {
        long amount = ((Number) event.getPayload().getOrDefault("amountCents", 0)).longValue();
        long newBalance = state.getBalanceCents() - amount;
        return new AccountState(newBalance, state.getVersion() + 1);
    };
}