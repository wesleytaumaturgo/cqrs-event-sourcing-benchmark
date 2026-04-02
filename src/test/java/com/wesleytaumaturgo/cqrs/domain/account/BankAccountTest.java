package com.wesleytaumaturgo.cqrs.domain.account;

import static org.assertj.core.api.Assertions.*;

import com.wesleytaumaturgo.cqrs.domain.account.commands.DepositMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.OpenAccountCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.WithdrawMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.InsufficientFundsException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BankAccountTest {

    // ─── REQ-1: Abertura de Conta ─────────────────────────────────────────────

    @Test
    void openAccount_shouldEmitAccountOpenedEvent() { // REQ-1.EARS-1
        var cmd = new OpenAccountCommand("owner-123", Money.of(new BigDecimal("100.00")));
        var account = BankAccount.open(cmd);

        var events = account.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(AccountOpenedEvent.class);

        var event = (AccountOpenedEvent) events.get(0);
        assertThat(event.ownerId()).isEqualTo("owner-123");
        assertThat(event.initialBalance()).isEqualTo(Money.of(new BigDecimal("100.00")));
        assertThat(event.accountId()).isNotNull();
    }

    @Test
    void openAccount_shouldReject_whenInitialBalanceNegative() { // REQ-1.EARS-2
        // Money VO rejeita valores negativos na construção
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Money.of(new BigDecimal("-1.00")))
            .withMessage("Money amount must not be negative");
    }

    @Test
    void openAccount_shouldReject_whenOwnerIdBlank() { // REQ-1.EARS-3
        var cmd = new OpenAccountCommand("  ", Money.of(new BigDecimal("100.00")));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> BankAccount.open(cmd))
            .withMessage("ownerId is required");
    }

    // ─── REQ-2: Depósito ─────────────────────────────────────────────────────

    @Test
    void deposit_shouldEmitMoneyDepositedEvent() { // REQ-2.EARS-1
        var account = BankAccount.open(new OpenAccountCommand("owner-123", Money.of(new BigDecimal("100.00"))));
        account.clearUncommittedEvents();

        account.deposit(new DepositMoneyCommand(account.getAccountId(), Money.of(new BigDecimal("50.00"))));

        var events = account.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(MoneyDepositedEvent.class);

        var event = (MoneyDepositedEvent) events.get(0);
        assertThat(event.amount()).isEqualTo(Money.of(new BigDecimal("50.00")));
        assertThat(account.getBalance()).isEqualTo(Money.of(new BigDecimal("150.00")));
    }

    @Test
    void deposit_shouldReject_whenAmountNotPositive() { // REQ-2.EARS-2
        var account = BankAccount.open(new OpenAccountCommand("owner-123", Money.of(new BigDecimal("100.00"))));

        // Zero é rejeitado pelo BankAccount (Money.zero() é válido, mas deposit exige positivo)
        assertThatIllegalArgumentException()
            .isThrownBy(() -> account.deposit(new DepositMoneyCommand(account.getAccountId(), Money.zero())))
            .withMessage("amount must be positive");

        // Negativo é rejeitado pelo Money VO antes de chegar ao aggregate
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Money.of(new BigDecimal("-10.00")))
            .withMessage("Money amount must not be negative");
    }

    // ─── REQ-3: Saque ────────────────────────────────────────────────────────

    @Test
    void withdraw_shouldEmitMoneyWithdrawnEvent() { // REQ-3.EARS-1
        var account = BankAccount.open(new OpenAccountCommand("owner-123", Money.of(new BigDecimal("100.00"))));
        account.clearUncommittedEvents();

        account.withdraw(new WithdrawMoneyCommand(account.getAccountId(), Money.of(new BigDecimal("30.00"))));

        var events = account.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(MoneyWithdrawnEvent.class);

        var event = (MoneyWithdrawnEvent) events.get(0);
        assertThat(event.amount()).isEqualTo(Money.of(new BigDecimal("30.00")));
        assertThat(account.getBalance()).isEqualTo(Money.of(new BigDecimal("70.00")));
    }

    @Test
    void withdraw_shouldReject_whenInsufficientFunds() { // REQ-3.EARS-2
        var account = BankAccount.open(new OpenAccountCommand("owner-123", Money.of(new BigDecimal("50.00"))));

        assertThatExceptionOfType(InsufficientFundsException.class)
            .isThrownBy(() -> account.withdraw(new WithdrawMoneyCommand(account.getAccountId(), Money.of(new BigDecimal("100.00")))))
            .withMessageContaining("balance=50.00")
            .withMessageContaining("requested=100.00");
    }

    @Test
    void withdraw_shouldReject_whenAmountNotPositive() { // REQ-3.EARS-3
        var account = BankAccount.open(new OpenAccountCommand("owner-123", Money.of(new BigDecimal("100.00"))));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> account.withdraw(new WithdrawMoneyCommand(account.getAccountId(), Money.zero())))
            .withMessage("amount must be positive");
    }

    @Test
    void withdraw_shouldSucceed_whenAmountEqualsBalance() { // REQ-3.EARS-5
        var account = BankAccount.open(new OpenAccountCommand("owner-123", Money.of(new BigDecimal("100.00"))));

        account.withdraw(new WithdrawMoneyCommand(account.getAccountId(), Money.of(new BigDecimal("100.00"))));

        assertThat(account.getBalance()).isEqualTo(Money.zero());
    }

    // ─── REQ-7: Reconstrução de Aggregate ────────────────────────────────────

    @Test
    void reconstitute_shouldReplayAllEvents() { // REQ-7.EARS-1
        var accountId = AccountId.generate();
        var events = List.of(
            new AccountOpenedEvent(accountId, "owner-123", Money.of(new BigDecimal("100.00")), null),
            new MoneyDepositedEvent(accountId, Money.of(new BigDecimal("50.00")), null),
            new MoneyWithdrawnEvent(accountId, Money.of(new BigDecimal("30.00")), null)
        );

        var account = BankAccount.reconstitute(accountId, events);

        assertThat(account.getBalance()).isEqualTo(Money.of(new BigDecimal("120.00")));
        assertThat(account.getAccountId()).isEqualTo(accountId);
        assertThat(account.getUncommittedEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldNotWriteDuringReplay() { // REQ-7.EARS-2
        var accountId = AccountId.generate();
        var events = List.of(
            new AccountOpenedEvent(accountId, "owner-123", Money.of(new BigDecimal("100.00")), null)
        );

        var account = BankAccount.reconstitute(accountId, events);

        // Reconstituição não deve gerar eventos uncommitted
        assertThat(account.getUncommittedEvents()).isEmpty();
    }
}
