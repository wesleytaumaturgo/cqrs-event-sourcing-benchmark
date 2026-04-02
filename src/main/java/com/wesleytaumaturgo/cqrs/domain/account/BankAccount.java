package com.wesleytaumaturgo.cqrs.domain.account;

import com.wesleytaumaturgo.cqrs.domain.account.commands.DepositMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.OpenAccountCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.WithdrawMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.InsufficientFundsException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BankAccount {

    private AccountId accountId;
    private String ownerId;
    private Money balance;
    // Rastreia apenas eventos já commitados (incrementado em reconstitute).
    // Valor -1 indica aggregate novo (sem eventos persistidos).
    // Equals ao último sequence_number gravado no event store.
    private long version = -1L;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    private BankAccount() {}

    public static BankAccount open(OpenAccountCommand cmd) {
        Objects.requireNonNull(cmd.ownerId(), "ownerId is required");
        if (cmd.ownerId().isBlank()) {
            throw new IllegalArgumentException("ownerId is required");
        }
        Objects.requireNonNull(cmd.initialBalance(), "initialBalance must be non-negative");

        BankAccount account = new BankAccount();
        account.raiseEvent(new AccountOpenedEvent(
            AccountId.generate(),
            cmd.ownerId(),
            cmd.initialBalance(),
            Instant.now()
        ));
        return account;
    }

    public static BankAccount reconstitute(AccountId id, List<? extends DomainEvent> events) {
        BankAccount account = new BankAccount();
        account.accountId = id;
        account.balance = Money.zero();
        for (DomainEvent event : events) {
            account.apply(event);
            account.version++;
        }
        return account;
    }

    public void deposit(DepositMoneyCommand cmd) {
        Objects.requireNonNull(cmd.amount(), "amount must be positive");
        if (cmd.amount().getValue().signum() == 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        raiseEvent(new MoneyDepositedEvent(accountId, cmd.amount(), Instant.now()));
    }

    public void withdraw(WithdrawMoneyCommand cmd) {
        Objects.requireNonNull(cmd.amount(), "amount must be positive");
        if (cmd.amount().getValue().signum() == 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (!balance.isGreaterThanOrEqualTo(cmd.amount())) {
            throw new InsufficientFundsException(balance, cmd.amount());
        }
        raiseEvent(new MoneyWithdrawnEvent(accountId, cmd.amount(), Instant.now()));
    }

    private void raiseEvent(DomainEvent event) {
        apply(event);
        uncommittedEvents.add(event);
    }

    private void apply(DomainEvent event) {
        if (event instanceof AccountOpenedEvent e) {
            this.accountId = e.accountId();
            this.ownerId = e.ownerId();
            this.balance = e.initialBalance();
        } else if (event instanceof MoneyDepositedEvent e) {
            this.balance = this.balance.add(e.amount());
        } else if (event instanceof MoneyWithdrawnEvent e) {
            this.balance = this.balance.subtract(e.amount());
        } else {
            throw new IllegalStateException("Unknown event type: " + event.getClass().getSimpleName());
        }
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public long getVersion() {
        return version;
    }

    public Money getBalance() {
        return balance;
    }

    public List<DomainEvent> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }
}
