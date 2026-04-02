package com.wesleytaumaturgo.cqrs.manual.eventstore;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import java.util.List;

/**
 * Port: event store abstraction para o ES Manual.
 * Lança AccountNotFoundException quando nenhum evento existe para o aggregate.
 */
public interface EventStore {

    /**
     * Persiste eventos para o aggregate identificado por accountId.
     *
     * @param expectedVersion versão do último evento já commitado (-1 se o aggregate é novo).
     *                        O banco rejeita com OptimisticLockingException se a versão já foi ocupada.
     */
    void append(AccountId accountId, long expectedVersion, List<DomainEvent> events);

    List<DomainEvent> loadEvents(AccountId accountId);
}
