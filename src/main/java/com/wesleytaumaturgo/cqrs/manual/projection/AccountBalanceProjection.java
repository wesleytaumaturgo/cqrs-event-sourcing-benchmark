package com.wesleytaumaturgo.cqrs.manual.projection;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Projetor que mantém account_balance_view sincronizado com os eventos de domínio.
 * Também serve como query handler para leitura de saldo (sem replay de eventos).
 */
@Component
public class AccountBalanceProjection {

    private static final Logger log = LoggerFactory.getLogger(AccountBalanceProjection.class);

    private final AccountBalanceViewRepository repository;

    public AccountBalanceProjection(AccountBalanceViewRepository repository) {
        this.repository = repository;
    }

    public AccountBalanceView getBalance(AccountId accountId) {
        return repository.findById(accountId.getValue())
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public void onAccountOpened(AccountOpenedEvent event) {
        repository.save(new AccountBalanceView(
            event.accountId().getValue(),
            event.ownerId(),
            event.initialBalance().getValue(),
            event.occurredAt(),
            0L
        ));
    }

    public void onMoneyDeposited(MoneyDepositedEvent event) {
        repository.findById(event.accountId().getValue()).ifPresentOrElse(
            view -> {
                view.setBalance(view.getBalance().add(event.amount().getValue()));
                view.setLastUpdated(event.occurredAt());
                view.setVersion(view.getVersion() + 1);
                repository.save(view);
            },
            () -> log.warn("MoneyDepositedEvent recebido para conta inexistente: {}", event.accountId())
        );
    }

    public void onMoneyWithdrawn(MoneyWithdrawnEvent event) {
        repository.findById(event.accountId().getValue()).ifPresentOrElse(
            view -> {
                view.setBalance(view.getBalance().subtract(event.amount().getValue()));
                view.setLastUpdated(event.occurredAt());
                view.setVersion(view.getVersion() + 1);
                repository.save(view);
            },
            () -> log.warn("MoneyWithdrawnEvent recebido para conta inexistente: {}", event.accountId())
        );
    }
}
