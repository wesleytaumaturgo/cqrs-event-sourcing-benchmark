package com.wesleytaumaturgo.cqrs.domain.account.events;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;

import java.math.BigDecimal;
import java.time.Instant;

public record MoneyWithdrawnEvent(
    AccountId accountId,
    BigDecimal amount,
    Instant occurredAt
) implements DomainEvent {}
