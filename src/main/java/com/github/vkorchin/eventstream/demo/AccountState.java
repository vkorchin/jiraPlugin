package com.github.vkorchin.eventstream.demo;

import com.github.vkorchin.eventstream.EntityState;
import com.github.vkorchin.eventstream.Event;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class AccountState implements EntityState<AccountState> {
    private final BigDecimal balance;
    private final Map<String, String> meta;

    public AccountState() {
        this(BigDecimal.ZERO, new HashMap<>());
    }

    private AccountState(BigDecimal balance, Map<String, String> meta) {
        this.balance = balance;
        this.meta = meta;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    @Override
    public AccountState apply(Event event) {
        switch (event.getEventType()) {
            case "deposit":
                BigDecimal dep = new BigDecimal(event.getPayload().get("amount").toString());
                return new AccountState(balance.add(dep), new HashMap<>(meta));
            case "withdraw":
                BigDecimal wd = new BigDecimal(event.getPayload().get("amount").toString());
                return new AccountState(balance.subtract(wd), new HashMap<>(meta));
            case "setMeta":
                Map<String, String> newMeta = new HashMap<>(meta);
                Object k = event.getPayload().get("key");
                Object v = event.getPayload().get("value");
                if (k != null) newMeta.put(k.toString(), v != null ? v.toString() : null);
                return new AccountState(balance, newMeta);
            default:
                return this;
        }
    }

    @Override
    public AccountState copy() {
        return new AccountState(balance, new HashMap<>(meta));
    }

    @Override
    public String toString() {
        return "AccountState{" +
                "balance=" + balance +
                ", meta=" + meta +
                '}';
    }
}