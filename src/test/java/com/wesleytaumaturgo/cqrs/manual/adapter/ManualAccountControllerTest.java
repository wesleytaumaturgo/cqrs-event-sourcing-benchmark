package com.wesleytaumaturgo.cqrs.manual.adapter;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ManualAccountControllerTest {

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

    // ─── REQ-1: Abertura de Conta ──────────────────────────────────────────────

    @Test
    void openAccount_shouldReturn201_withAccountId() throws Exception {
        // REQ-1.EARS-1
        mvc.perform(post(BASE + "/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ownerId":"owner-1","initialBalance":100.00}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").isNotEmpty());
    }

    @Test
    void openAccount_shouldReturn400_whenInitialBalanceNegative() throws Exception {
        // REQ-1.EARS-2
        mvc.perform(post(BASE + "/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ownerId":"owner-1","initialBalance":-1.00}
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Money amount must not be negative"));
    }

    @Test
    void openAccount_shouldReturn400_whenOwnerIdBlank() throws Exception {
        // REQ-1.EARS-3
        mvc.perform(post(BASE + "/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ownerId":"  ","initialBalance":100.00}
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("ownerId is required"));
    }

    // ─── REQ-2: Depósito ───────────────────────────────────────────────────────

    @Test
    void deposit_shouldReturn200_withUpdatedBalance() throws Exception {
        // REQ-2.EARS-1
        var accountId = createAccount("owner-2", "50.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":30.00}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(80.00));
    }

    @Test
    void deposit_shouldReturn400_whenAmountNotPositive() throws Exception {
        // REQ-2.EARS-2
        var accountId = createAccount("owner-2", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":0}
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("amount must be positive"));
    }

    @Test
    void deposit_shouldReturn404_whenAccountNotFound() throws Exception {
        // REQ-2.EARS-3
        var nonExistentId = "00000000-0000-0000-0000-000000000000";

        mvc.perform(post(BASE + "/accounts/" + nonExistentId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":10.00}
                        """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(containsString(nonExistentId)));
    }

    // ─── REQ-3: Saque ──────────────────────────────────────────────────────────

    @Test
    void withdraw_shouldReturn200_withUpdatedBalance() throws Exception {
        // REQ-3.EARS-1
        var accountId = createAccount("owner-3", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":40.00}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(60.00));
    }

    @Test
    void withdraw_shouldReturn422_whenInsufficientFunds() throws Exception {
        // REQ-3.EARS-2
        var accountId = createAccount("owner-3", "50.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":100.00}
                        """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value(containsString("balance=50.00")))
            .andExpect(jsonPath("$.error").value(containsString("requested=100.00")));
    }

    @Test
    void withdraw_shouldReturn400_whenAmountNotPositive() throws Exception {
        // REQ-3.EARS-3
        var accountId = createAccount("owner-3", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":-5.00}
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Money amount must not be negative"));
    }

    @Test
    void withdraw_shouldReturn404_whenAccountNotFound() throws Exception {
        // REQ-3.EARS-4
        var nonExistentId = "00000000-0000-0000-0000-000000000001";

        mvc.perform(post(BASE + "/accounts/" + nonExistentId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":10.00}
                        """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(containsString(nonExistentId)));
    }

    @Test
    void withdraw_shouldReturn200_withZeroBalance_whenAmountEqualsBalance() throws Exception {
        // REQ-3.EARS-5
        var accountId = createAccount("owner-3", "75.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":75.00}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(0.00));
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
