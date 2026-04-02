package com.wesleytaumaturgo.cqrs.axon.aggregate;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.InsufficientFundsException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
public class BankAccountAggregate {

    @AggregateIdentifier
    private String accountId;
    private Money balance;

    protected BankAccountAggregate() {}

    @CommandHandler
    public BankAccountAggregate(OpenBankAccountAxonCommand cmd) {
        if (cmd.ownerId() == null || cmd.ownerId().isBlank()) {
            throw new IllegalArgumentException("ownerId is required");
        }
        Objects.requireNonNull(cmd.initialBalance(), "initialBalance must be non-negative");

        AggregateLifecycle.apply(new AccountOpenedEvent(
            AccountId.of(UUID.fromString(cmd.accountId())),
            cmd.ownerId(),
            cmd.initialBalance(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(DepositMoneyAxonCommand cmd) {
        Objects.requireNonNull(cmd.amount(), "amount must be positive");
        if (cmd.amount().getValue().signum() == 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        AggregateLifecycle.apply(new MoneyDepositedEvent(
            AccountId.of(UUID.fromString(cmd.accountId())),
            cmd.amount(),
            Instant.now()
        ));
    }

    @CommandHandler
    public void handle(WithdrawMoneyAxonCommand cmd) {
        Objects.requireNonNull(cmd.amount(), "amount must be positive");
        if (cmd.amount().getValue().signum() == 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (!balance.isGreaterThanOrEqualTo(cmd.amount())) {
            throw new InsufficientFundsException(balance, cmd.amount());
        }
        AggregateLifecycle.apply(new MoneyWithdrawnEvent(
            AccountId.of(UUID.fromString(cmd.accountId())),
            cmd.amount(),
            Instant.now()
        ));
    }

    @EventSourcingHandler
    public void on(AccountOpenedEvent event) {
        this.accountId = event.accountId().toString();
        this.balance = event.initialBalance();
    }

    @EventSourcingHandler
    public void on(MoneyDepositedEvent event) {
        this.balance = this.balance.add(event.amount());
    }

    @EventSourcingHandler
    public void on(MoneyWithdrawnEvent event) {
        this.balance = this.balance.subtract(event.amount());
    }
}
