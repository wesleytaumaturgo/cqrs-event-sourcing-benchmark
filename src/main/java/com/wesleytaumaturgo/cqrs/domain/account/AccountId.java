package com.wesleytaumaturgo.cqrs.domain.account;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {

    private final UUID id;

    private AccountId(UUID id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID id) {
        return new AccountId(id);
    }

    public static AccountId of(String id) {
        return new AccountId(UUID.fromString(id));
    }

    public UUID getValue() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
