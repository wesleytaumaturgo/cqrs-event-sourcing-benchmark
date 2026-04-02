package com.wesleytaumaturgo.cqrs.axon.projection;

import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ProcessingGroup("axon-account-projection")
public class AxonAccountBalanceProjection {

    private static final Logger log = LoggerFactory.getLogger(AxonAccountBalanceProjection.class);

    private final AxonBalanceViewRepository repository;

    public AxonAccountBalanceProjection(AxonBalanceViewRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    @Transactional
    public void on(AccountOpenedEvent event) {
        repository.save(new AxonBalanceView(
            event.accountId().toString(),
            event.ownerId(),
            event.initialBalance().getValue(),
            event.occurredAt(),
            0L
        ));
    }

    @EventHandler
    @Transactional
    public void on(MoneyDepositedEvent event) {
        repository.findById(event.accountId().toString()).ifPresentOrElse(
            view -> {
                view.setBalance(view.getBalance().add(event.amount().getValue()));
                view.setLastUpdated(event.occurredAt());
                view.setVersion(view.getVersion() + 1);
                repository.save(view);
            },
            () -> log.warn("MoneyDepositedEvent recebido para conta inexistente: {}", event.accountId())
        );
    }

    @EventHandler
    @Transactional
    public void on(MoneyWithdrawnEvent event) {
        repository.findById(event.accountId().toString()).ifPresentOrElse(
            view -> {
                view.setBalance(view.getBalance().subtract(event.amount().getValue()));
                view.setLastUpdated(event.occurredAt());
                view.setVersion(view.getVersion() + 1);
                repository.save(view);
            },
            () -> log.warn("MoneyWithdrawnEvent recebido para conta inexistente: {}", event.accountId())
        );
    }

    @Transactional(readOnly = true)
    public AxonBalanceView getBalance(String accountId) {
        return repository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
