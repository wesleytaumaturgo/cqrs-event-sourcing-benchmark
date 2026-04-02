package com.wesleytaumaturgo.cqrs.manual.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
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
class AccountBalanceProjectionTest {

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
    AccountBalanceProjection projection;

    @Autowired
    AccountBalanceViewRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void getBalance_shouldReadFromProjection_notFromEventStore() {
        // REQ-4.EARS-1: lê da projeção account_balance_view, sem replay de eventos
        var accountId = AccountId.generate();
        var expectedBalance = new BigDecimal("250.00");

        // Popula projeção diretamente (sem usar event store)
        projection.onAccountOpened(new AccountOpenedEvent(
            accountId, "owner-1", Money.of(expectedBalance), Instant.now()
        ));

        var view = projection.getBalance(accountId);

        assertThat(view.getAccountId()).isEqualTo(accountId.getValue());
        assertThat(view.getBalance()).isEqualByComparingTo(expectedBalance);
        assertThat(view.getLastUpdated()).isNotNull();
    }

    @Test
    void getBalance_shouldReturn404_whenAccountNotFound() {
        // REQ-4.EARS-2: conta inexistente → AccountNotFoundException
        var unknownId = AccountId.generate();

        assertThatThrownBy(() -> projection.getBalance(unknownId))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining(unknownId.toString());
    }

    @Test
    void onMoneyDeposited_shouldIncreaseBalance() {
        // REQ-4.EARS-1 (cobertura do projetor: depósito atualiza saldo)
        var accountId = AccountId.generate();
        projection.onAccountOpened(new AccountOpenedEvent(
            accountId, "owner-1", Money.of(new BigDecimal("100.00")), Instant.now()
        ));

        projection.onMoneyDeposited(new MoneyDepositedEvent(
            accountId, Money.of(new BigDecimal("50.00")), Instant.now()
        ));

        var view = projection.getBalance(accountId);
        assertThat(view.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void onMoneyWithdrawn_shouldDecreaseBalance() {
        // REQ-4.EARS-1 (cobertura do projetor: saque atualiza saldo)
        var accountId = AccountId.generate();
        projection.onAccountOpened(new AccountOpenedEvent(
            accountId, "owner-1", Money.of(new BigDecimal("100.00")), Instant.now()
        ));

        projection.onMoneyWithdrawn(new MoneyWithdrawnEvent(
            accountId, Money.of(new BigDecimal("30.00")), Instant.now()
        ));

        var view = projection.getBalance(accountId);
        assertThat(view.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
    }
}
