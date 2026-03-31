package com.wesleytaumaturgo.cqrs.domain.account;

import com.wesleytaumaturgo.cqrs.domain.account.commands.DepositMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.OpenAccountCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.WithdrawMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BankAccount {

    private AccountId accountId;
    private String ownerId;
    private BigDecimal balance;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    private BankAccount() {}

    public static BankAccount open(OpenAccountCommand cmd) {
        if (cmd.ownerId() == null || cmd.ownerId().isBlank()) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (cmd.initialBalance() == null || cmd.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("initialBalance must be non-negative");
        }

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
        account.balance = BigDecimal.ZERO;
        for (DomainEvent event : events) {
            account.apply(event);
        }
        return account;
    }

    public void deposit(DepositMoneyCommand cmd) {
        if (cmd.amount() == null || cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        raiseEvent(new MoneyDepositedEvent(accountId, cmd.amount(), Instant.now()));
    }

    public void withdraw(WithdrawMoneyCommand cmd) {
        if (cmd.amount() == null || cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (balance.compareTo(cmd.amount()) < 0) {
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
        }
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public List<DomainEvent> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }
}
