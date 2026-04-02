package com.wesleytaumaturgo.cqrs.domain.account.events;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import java.time.Instant;

public record MoneyWithdrawnEvent(
    AccountId accountId,
    Money amount,
    Instant occurredAt
) implements DomainEvent {}
