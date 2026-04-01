package com.wesleytaumaturgo.cqrs.manual.service;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.BankAccount;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.commands.DepositMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.OpenAccountCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.WithdrawMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.manual.eventstore.EventStore;
import com.wesleytaumaturgo.cqrs.manual.projection.AccountBalanceProjection;
import com.wesleytaumaturgo.cqrs.manual.projection.AccountBalanceView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class ManualAccountService {

    private final EventStore eventStore;
    private final AccountBalanceProjection projection;

    public ManualAccountService(EventStore eventStore, AccountBalanceProjection projection) {
        this.eventStore = eventStore;
        this.projection = projection;
    }

    public String openAccount(String ownerId, BigDecimal initialBalance) {
        var account = BankAccount.open(new OpenAccountCommand(ownerId, Money.of(initialBalance)));
        var uncommitted = account.getUncommittedEvents();

        eventStore.append(account.getAccountId(), uncommitted);

        uncommitted.forEach(event -> {
            if (event instanceof AccountOpenedEvent e) projection.onAccountOpened(e);
        });

        return account.getAccountId().toString();
    }

    public AccountBalanceView deposit(String accountIdStr, BigDecimal amount) {
        var accountId = AccountId.of(UUID.fromString(accountIdStr));
        var events = eventStore.loadEvents(accountId);
        var account = BankAccount.reconstitute(accountId, events);

        account.deposit(new DepositMoneyCommand(accountId, Money.of(amount)));
        var uncommitted = account.getUncommittedEvents();

        eventStore.append(accountId, uncommitted);

        uncommitted.forEach(event -> {
            if (event instanceof MoneyDepositedEvent e) projection.onMoneyDeposited(e);
        });

        return projection.getBalance(accountId);
    }

    public AccountBalanceView withdraw(String accountIdStr, BigDecimal amount) {
        var accountId = AccountId.of(UUID.fromString(accountIdStr));
        var events = eventStore.loadEvents(accountId);
        var account = BankAccount.reconstitute(accountId, events);

        account.withdraw(new WithdrawMoneyCommand(accountId, Money.of(amount)));
        var uncommitted = account.getUncommittedEvents();

        eventStore.append(accountId, uncommitted);

        uncommitted.forEach(event -> {
            if (event instanceof MoneyWithdrawnEvent e) projection.onMoneyWithdrawn(e);
        });

        return projection.getBalance(accountId);
    }

    @Transactional(readOnly = true)
    public AccountBalanceView getBalance(String accountIdStr) {
        var accountId = AccountId.of(UUID.fromString(accountIdStr));
        return projection.getBalance(accountId);
    }
}
