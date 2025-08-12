package streaming.demo;

import streaming.EntityState;

import java.util.Objects;

public final class AccountState implements EntityState<AccountState> {
    private final long balanceCents;
    private final long version;

    public AccountState() {
        this(0L, 0L);
    }

    public AccountState(long balanceCents, long version) {
        this.balanceCents = balanceCents;
        this.version = version;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public long getVersion() {
        return version;
    }

    public AccountState withBalanceCents(long newBalance) {
        return new AccountState(newBalance, version + 1);
    }

    @Override
    public AccountState copy() {
        return new AccountState(balanceCents, version);
    }

    @Override
    public String toString() {
        return "AccountState{" +
                "balanceCents=" + balanceCents +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountState)) return false;
        AccountState that = (AccountState) o;
        return balanceCents == that.balanceCents && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(balanceCents, version);
    }
}