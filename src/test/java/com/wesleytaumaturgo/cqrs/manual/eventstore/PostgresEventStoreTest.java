package com.wesleytaumaturgo.cqrs.manual.eventstore;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.BankAccount;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.commands.DepositMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.OpenAccountCommand;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PostgresEventStoreTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("benchmark")
        .withUsername("user")
        .withPassword("pass");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    EventStore eventStore;

    @Autowired
    StoredEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void loadEvents_shouldThrowAccountNotFoundException_whenAccountDoesNotExistForDeposit() {
        // REQ-2.EARS-3: depósito em conta inexistente → 404
        var unknownId = AccountId.generate();

        assertThatThrownBy(() -> eventStore.loadEvents(unknownId))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining(unknownId.toString());
    }

    @Test
    void loadEvents_shouldThrowAccountNotFoundException_whenAccountDoesNotExistForWithdrawal() {
        // REQ-3.EARS-4: saque em conta inexistente → 404
        var unknownId = AccountId.generate();

        assertThatThrownBy(() -> eventStore.loadEvents(unknownId))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void loadEvents_shouldReturnEventsInAscendingSequenceOrder() {
        // REQ-5.EARS-1: extrato retorna eventos ordenados por sequence_number ASC
        var account = BankAccount.open(new OpenAccountCommand("owner-1", Money.of(new BigDecimal("100.00"))));
        var accountId = account.getAccountId();

        eventStore.append(accountId, account.getUncommittedEvents());
        account.clearUncommittedEvents();

        account.deposit(new DepositMoneyCommand(accountId, Money.of(new BigDecimal("50.00"))));
        eventStore.append(accountId, account.getUncommittedEvents());

        var events = eventStore.loadEvents(accountId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(AccountOpenedEvent.class);
        assertThat(events.get(1)).isInstanceOf(MoneyDepositedEvent.class);
    }

    @Test
    void append_shouldEnforceUniqueSequence_underConcurrentDeposits() throws Exception {
        // Garante que dois appends concorrentes não corrompem sequência (uk_aggregate_sequence)
        var seed = BankAccount.open(new OpenAccountCommand("owner-race", Money.of(new BigDecimal("1000.00"))));
        var accountId = seed.getAccountId();
        eventStore.append(accountId, seed.getUncommittedEvents());

        // Pré-criar eventos independentes por thread — sem estado compartilhado entre threads
        var deposit1 = List.<DomainEvent>of(
            new MoneyDepositedEvent(accountId, Money.of(new BigDecimal("10.00")), Instant.now())
        );
        var deposit2 = List.<DomainEvent>of(
            new MoneyDepositedEvent(accountId, Money.of(new BigDecimal("20.00")), Instant.now())
        );

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Boolean> f1 = pool.submit(() -> {
            start.await();
            try { eventStore.append(accountId, deposit1); return true; }
            catch (DataIntegrityViolationException e) { return false; }
        });
        Future<Boolean> f2 = pool.submit(() -> {
            start.await();
            try { eventStore.append(accountId, deposit2); return true; }
            catch (DataIntegrityViolationException e) { return false; }
        });

        start.countDown();
        pool.shutdown();

        boolean r1 = f1.get();
        boolean r2 = f2.get();

        // Pelo menos 1 deve ter tido sucesso — sem perda silenciosa de dados
        assertThat(r1 || r2).isTrue();

        var stored = repository.findByAggregateIdOrderBySequenceNumberAsc(accountId.getValue());
        var seqNumbers = stored.stream().map(StoredEvent::getSequenceNumber).toList();
        // sequence_numbers devem ser distintos — constraint uk_aggregate_sequence aplicada
        assertThat(seqNumbers).doesNotHaveDuplicates();
    }

    @Test
    void loadEvents_shouldThrowAccountNotFoundException_whenAccountDoesNotExistForEventsQuery() {
        // REQ-5.EARS-2: extrato de conta inexistente → 404
        var unknownId = AccountId.generate();

        assertThatThrownBy(() -> eventStore.loadEvents(unknownId))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining(unknownId.toString());
    }
}
