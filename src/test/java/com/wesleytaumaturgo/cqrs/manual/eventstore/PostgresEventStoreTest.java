package com.wesleytaumaturgo.cqrs.manual.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.BankAccount;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.commands.DepositMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.OpenAccountCommand;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.OptimisticLockingException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    // ─── Testes existentes (assinatura atualizada) ────────────────────────────

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

        // version=-1 antes de qualquer commit → primeira versão esperada
        eventStore.append(accountId, account.getVersion(), account.getUncommittedEvents());
        account.clearUncommittedEvents();

        // reconstitute para obter version=0 correta
        var loaded = BankAccount.reconstitute(accountId, eventStore.loadEvents(accountId));
        loaded.deposit(new DepositMoneyCommand(accountId, Money.of(new BigDecimal("50.00"))));
        eventStore.append(accountId, loaded.getVersion(), loaded.getUncommittedEvents());

        var events = eventStore.loadEvents(accountId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(AccountOpenedEvent.class);
        assertThat(events.get(1)).isInstanceOf(MoneyDepositedEvent.class);
    }

    @Test
    void loadEvents_shouldThrowAccountNotFoundException_whenAccountDoesNotExistForEventsQuery() {
        // REQ-5.EARS-2: extrato de conta inexistente → 404
        var unknownId = AccountId.generate();

        assertThatThrownBy(() -> eventStore.loadEvents(unknownId))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining(unknownId.toString());
    }

    // ─── Testes de Optimistic Locking ─────────────────────────────────────────

    @Test
    void append_shouldThrowOptimisticLockingException_whenSameExpectedVersionUsedTwice() {
        // Garante rejeição de append com versão já ocupada
        var account = BankAccount.open(new OpenAccountCommand("owner-ol", Money.of(new BigDecimal("100.00"))));
        var accountId = account.getAccountId();

        // Primeiro append: seq=0, expectedVersion=-1 → OK
        eventStore.append(accountId, -1L, account.getUncommittedEvents());

        // Segundo append com mesma expectedVersion=-1 → seq=0 já ocupado → conflito
        var conflictingDeposit = List.<DomainEvent>of(
            new MoneyDepositedEvent(accountId, Money.of(new BigDecimal("10.00")), Instant.now())
        );

        assertThatThrownBy(() -> eventStore.append(accountId, -1L, conflictingDeposit))
            .isInstanceOf(OptimisticLockingException.class);
    }

    @Test
    void append_shouldSucceed_whenSequentialVersionsAreUsed() {
        // Appends sequenciais com versões corretas devem todos ter sucesso
        var account = BankAccount.open(new OpenAccountCommand("owner-seq", Money.of(new BigDecimal("100.00"))));
        var accountId = account.getAccountId();

        // Append 1: seq=0, expectedVersion=-1
        eventStore.append(accountId, -1L, account.getUncommittedEvents());

        // Append 2: seq=1, expectedVersion=0
        var deposit = List.<DomainEvent>of(
            new MoneyDepositedEvent(accountId, Money.of(new BigDecimal("25.00")), Instant.now())
        );
        eventStore.append(accountId, 0L, deposit);

        assertThat(eventStore.loadEvents(accountId)).hasSize(2);
    }

    @Test
    void append_shouldEnforceOptimisticLocking_underConcurrentDeposits() throws Exception {
        // Dois threads tentam gravar na mesma versão — exatamente 1 deve ter sucesso
        var seed = BankAccount.open(new OpenAccountCommand("owner-race", Money.of(new BigDecimal("1000.00"))));
        var accountId = seed.getAccountId();
        eventStore.append(accountId, -1L, seed.getUncommittedEvents());

        // Ambas as threads usam expectedVersion=0 (último evento commitado = seq 0)
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
            try {
                eventStore.append(accountId, 0L, deposit1);
                return true;
            } catch (OptimisticLockingException e) {
                return false;
            }
        });
        Future<Boolean> f2 = pool.submit(() -> {
            start.await();
            try {
                eventStore.append(accountId, 0L, deposit2);
                return true;
            } catch (OptimisticLockingException e) {
                return false;
            }
        });

        start.countDown();
        pool.shutdown();

        boolean r1 = f1.get();
        boolean r2 = f2.get();

        // Exatamente 1 deve ter sucesso (XOR) ou pelo menos 1 — nunca ambos
        assertThat(r1 || r2).isTrue();
        assertThat(r1 && r2).isFalse();

        // O event store deve ter exatamente 2 eventos (AccountOpened + 1 depósito)
        var stored = repository.findByAggregateIdOrderBySequenceNumberAsc(accountId.getValue());
        assertThat(stored).hasSize(2);

        var seqNumbers = stored.stream().map(StoredEvent::getSequenceNumber).toList();
        assertThat(seqNumbers).doesNotHaveDuplicates();
        assertThat(seqNumbers).containsExactly(0L, 1L);
    }

    @Test
    void append_shouldIncludeAggregateInfoInOptimisticLockingException() {
        // A exception deve conter aggregateId e expectedVersion para facilitar diagnóstico
        var account = BankAccount.open(new OpenAccountCommand("owner-msg", Money.of(new BigDecimal("100.00"))));
        var accountId = account.getAccountId();

        eventStore.append(accountId, -1L, account.getUncommittedEvents());

        var conflicting = List.<DomainEvent>of(
            new MoneyDepositedEvent(accountId, Money.of(new BigDecimal("5.00")), Instant.now())
        );

        assertThatThrownBy(() -> eventStore.append(accountId, -1L, conflicting))
            .isInstanceOf(OptimisticLockingException.class)
            .hasMessageContaining(accountId.toString())
            .hasMessageContaining("-1");
    }
}
