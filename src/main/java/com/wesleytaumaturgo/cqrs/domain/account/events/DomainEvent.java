package com.wesleytaumaturgo.cqrs.domain.account.events;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;

import java.time.Instant;

public interface DomainEvent {
    AccountId accountId();
    Instant occurredAt();
}
