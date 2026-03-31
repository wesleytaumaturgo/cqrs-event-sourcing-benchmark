package com.wesleytaumaturgo.cqrs.axon.aggregate;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.InsufficientFundsException;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.axonframework.test.matchers.Matchers.messageWithPayload;
import static org.axonframework.test.matchers.Matchers.sequenceOf;
import static org.hamcrest.Matchers.instanceOf;

class BankAccountAggregateTest {

    private FixtureConfiguration<BankAccountAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(BankAccountAggregate.class);
    }

    @Test
    void openAccount_shouldEmitAccountOpenedEvent() {
        // REQ-1.EARS-1
        var accountId = UUID.randomUUID().toString();

        fixture.givenNoPriorActivity()
            .when(new OpenBankAccountAxonCommand(accountId, "owner-1", new BigDecimal("100.00")))
            .expectEventsMatching(sequenceOf(messageWithPayload(instanceOf(AccountOpenedEvent.class))));
    }

    @Test
    void deposit_shouldEmitMoneyDepositedEvent() {
        // REQ-2.EARS-1
        var accountId = UUID.randomUUID().toString();

        fixture.givenCommands(new OpenBankAccountAxonCommand(accountId, "owner-1", new BigDecimal("100.00")))
            .when(new DepositMoneyAxonCommand(accountId, new BigDecimal("50.00")))
            .expectEventsMatching(sequenceOf(messageWithPayload(instanceOf(MoneyDepositedEvent.class))));
    }

    @Test
    void withdraw_shouldEmitMoneyWithdrawnEvent() {
        // REQ-3.EARS-1
        var accountId = UUID.randomUUID().toString();

        fixture.givenCommands(new OpenBankAccountAxonCommand(accountId, "owner-1", new BigDecimal("100.00")))
            .when(new WithdrawMoneyAxonCommand(accountId, new BigDecimal("40.00")))
            .expectEventsMatching(sequenceOf(messageWithPayload(instanceOf(MoneyWithdrawnEvent.class))));
    }

    @Test
    void withdraw_shouldThrow_whenInsufficientFunds() {
        // REQ-3.EARS-2
        var accountId = UUID.randomUUID().toString();

        fixture.givenCommands(new OpenBankAccountAxonCommand(accountId, "owner-1", new BigDecimal("50.00")))
            .when(new WithdrawMoneyAxonCommand(accountId, new BigDecimal("100.00")))
            .expectException(InsufficientFundsException.class)
            .expectExceptionMessage(org.hamcrest.Matchers.containsString("balance=50.00"))
            .expectExceptionMessage(org.hamcrest.Matchers.containsString("requested=100.00"));
    }

    @Test
    void withdraw_shouldEmitEvent_whenAmountEqualsBalance() {
        // REQ-3.EARS-1 (edge: saldo zerado após saque total)
        var accountId = UUID.randomUUID().toString();

        fixture.given(new AccountOpenedEvent(
                    AccountId.of(accountId), "owner-1", new BigDecimal("75.00"), Instant.now()))
            .when(new WithdrawMoneyAxonCommand(accountId, new BigDecimal("75.00")))
            .expectEventsMatching(sequenceOf(messageWithPayload(instanceOf(MoneyWithdrawnEvent.class))));
    }
}
