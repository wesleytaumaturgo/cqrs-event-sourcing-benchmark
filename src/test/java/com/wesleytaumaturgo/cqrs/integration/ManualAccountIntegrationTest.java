package com.wesleytaumaturgo.cqrs.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração da implementação Manual (ES Manual via PostgreSQL JSONB).
 * Valida paridade de contrato com a implementação Axon (REQ-6.EARS-1)
 * e persistência de eventos no PostgreSQL (REQ-6.EARS-2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ManualAccountIntegrationTest {

    static final String BASE = "/api/v1/manual";

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
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE domain_events, account_balance_view CASCADE");
    }

    // ─── REQ-6.EARS-1: Contrato idêntico ao Axon ──────────────────────────────

    @Test
    void openAccount_shouldReturn201_withAccountId() throws Exception {
        // REQ-6.EARS-1
        mvc.perform(post(BASE + "/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":\"owner-1\",\"initialBalance\":100.00}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").isNotEmpty());
    }

    @Test
    void deposit_shouldReturn200_withUpdatedBalance() throws Exception {
        // REQ-6.EARS-1
        var accountId = createAccount("owner-1", "50.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":30.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.balance").value(80.00));
    }

    @Test
    void withdraw_shouldReturn200_withUpdatedBalance() throws Exception {
        // REQ-6.EARS-1
        var accountId = createAccount("owner-1", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":40.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.balance").value(60.00));
    }

    @Test
    void withdraw_shouldReturn422_whenInsufficientFunds() throws Exception {
        // REQ-6.EARS-1
        var accountId = createAccount("owner-1", "50.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":100.00}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value(containsString("balance=50.00")))
            .andExpect(jsonPath("$.error").value(containsString("requested=100.00")));
    }

    @Test
    void deposit_shouldReturn400_whenAmountNotPositive() throws Exception {
        // REQ-6.EARS-1
        var accountId = createAccount("owner-1", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("amount must be positive"));
    }

    @Test
    void deposit_shouldReturn404_whenAccountNotFound() throws Exception {
        // REQ-6.EARS-1
        var unknownId = "00000000-0000-0000-0000-000000000011";

        mvc.perform(post(BASE + "/accounts/" + unknownId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10.00}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(containsString(unknownId)));
    }

    @Test
    void getBalance_shouldReturn404_whenAccountNotFound() throws Exception {
        // REQ-6.EARS-1
        var unknownId = "00000000-0000-0000-0000-000000000012";

        mvc.perform(get(BASE + "/accounts/" + unknownId + "/balance"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(containsString(unknownId)));
    }

    // ─── REQ-6.EARS-2: Evento persistido no PostgreSQL ────────────────────────

    @Test
    void openAccount_shouldPersistEventInDomainEvents() throws Exception {
        // REQ-6.EARS-2
        createAccount("owner-2", "100.00");

        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM domain_events WHERE event_type = 'AccountOpenedEvent'",
            Long.class);
        assertThat(count).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void deposit_shouldPersistEventInDomainEvents() throws Exception {
        // REQ-6.EARS-2
        var accountId = createAccount("owner-2", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50.00}"))
            .andExpect(status().isOk());

        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM domain_events WHERE event_type = 'MoneyDepositedEvent'",
            Long.class);
        assertThat(count).isGreaterThanOrEqualTo(1L);
    }

    // ─── Helper ────────────────────────────────────────────────────────────────

    private String createAccount(String ownerId, String initialBalance) throws Exception {
        var result = mvc.perform(post(BASE + "/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"ownerId\":\"%s\",\"initialBalance\":%s}",
                    ownerId, initialBalance)))
            .andExpect(status().isCreated())
            .andReturn();

        var body = result.getResponse().getContentAsString();
        return body.replaceAll(".*\"accountId\":\"([^\"]+)\".*", "$1");
    }
}
