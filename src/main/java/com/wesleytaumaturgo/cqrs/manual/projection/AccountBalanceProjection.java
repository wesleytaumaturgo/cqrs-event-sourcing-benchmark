package com.wesleytaumaturgo.cqrs.manual.projection;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Projetor que mantém account_balance_view sincronizado com os eventos de domínio.
 * Também serve como query handler para leitura de saldo (sem replay de eventos).
 */
@Component
public class AccountBalanceProjection {

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
            event.initialBalance(),
            event.occurredAt(),
            0L
        ));
    }

    public void onMoneyDeposited(MoneyDepositedEvent event) {
        repository.findById(event.accountId().getValue()).ifPresent(view -> {
            view.setBalance(view.getBalance().add(event.amount()));
            view.setLastUpdated(event.occurredAt());
            view.setVersion(view.getVersion() + 1);
            repository.save(view);
        });
    }

    public void onMoneyWithdrawn(MoneyWithdrawnEvent event) {
        repository.findById(event.accountId().getValue()).ifPresent(view -> {
            view.setBalance(view.getBalance().subtract(event.amount()));
            view.setLastUpdated(event.occurredAt());
            view.setVersion(view.getVersion() + 1);
            repository.save(view);
        });
    }
}
