package com.wesleytaumaturgo.cqrs.manual.eventstore;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;

import java.util.List;

/**
 * Port: event store abstraction para o ES Manual.
 * Lança AccountNotFoundException quando nenhum evento existe para o aggregate.
 */
public interface EventStore {

    void append(AccountId accountId, List<DomainEvent> events);

    List<DomainEvent> loadEvents(AccountId accountId);
}
