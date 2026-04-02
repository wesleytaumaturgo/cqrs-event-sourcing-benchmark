package com.wesleytaumaturgo.cqrs.domain.account.events;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import java.time.Instant;

public record AccountOpenedEvent(
    AccountId accountId,
    String ownerId,
    Money initialBalance,
    Instant occurredAt
) implements DomainEvent {}
