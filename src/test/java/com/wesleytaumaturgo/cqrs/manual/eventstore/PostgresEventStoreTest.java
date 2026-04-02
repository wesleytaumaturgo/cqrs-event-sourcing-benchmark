package com.wesleytaumaturgo.cqrs.manual.eventstore;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.BankAccount;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.commands.DepositMoneyCommand;
import com.wesleytaumaturgo.cqrs.domain.account.commands.OpenAccountCommand;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
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
import java.util.ArrayList;
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
        var account = BankAccount.open(new OpenAccountCommand("owner-race", Money.of(new BigDecimal("1000.00"))));
        var accountId = account.getAccountId();
        eventStore.append(accountId, account.getUncommittedEvents());
        account.clearUncommittedEvents();

        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    account.deposit(new DepositMoneyCommand(accountId, Money.of(new BigDecimal((idx + 1) + ".00"))));
                    eventStore.append(accountId, account.getUncommittedEvents());
                    return true;
                } catch (DataIntegrityViolationException e) {
                    return false; // colisão de sequence_number detectada pelo DB — comportamento esperado
                }
            }));
        }

        start.countDown();
        pool.shutdown();

        long successes = futures.stream().filter(f -> { try { return f.get(); } catch (Exception e) { return false; } }).count();
        long failures  = threads - successes;

        // Pelo menos 1 thread teve sucesso; colisões geram falha (não perda silenciosa)
        assertThat(successes).isGreaterThanOrEqualTo(1);
        assertThat(successes + failures).isEqualTo(threads);

        var stored = repository.findByAggregateIdOrderBySequenceNumberAsc(accountId.getValue());
        // sequence_numbers devem ser distintos — sem colisões silenciosas
        var seqNumbers = stored.stream().map(e -> e.getSequenceNumber()).toList();
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
