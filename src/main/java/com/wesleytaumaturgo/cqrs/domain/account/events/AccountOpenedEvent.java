package com.wesleytaumaturgo.cqrs.domain.account.events;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountOpenedEvent(
    AccountId accountId,
    String ownerId,
    BigDecimal initialBalance,
    Instant occurredAt
) implements DomainEvent {}
