package com.wesleytaumaturgo.cqrs.axon.projection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
    "axon.eventhandling.processors.axon-account-projection.mode=subscribing"
})
class AxonAccountBalanceProjectionTest {

    static final String BASE = "/api/v1/axon";

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
        jdbc.execute("TRUNCATE axon_account_balance_view CASCADE");
        jdbc.execute("TRUNCATE domain_event_entry CASCADE");
        jdbc.execute("TRUNCATE snapshot_event_entry CASCADE");
        jdbc.execute("DELETE FROM token_entry");
    }

    @Test
    void getBalance_shouldReturn200_fromProjection() throws Exception {
        // REQ-4.EARS-1
        var accountId = createAccount("owner-4", "200.00");

        mvc.perform(get(BASE + "/accounts/" + accountId + "/balance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.balance").value(200.00))
            .andExpect(jsonPath("$.lastUpdated").isNotEmpty());
    }

    @Test
    void getBalance_shouldReturn404_whenAccountNotFound() throws Exception {
        // REQ-4.EARS-2
        var nonExistentId = "00000000-0000-0000-0000-000000000099";

        mvc.perform(get(BASE + "/accounts/" + nonExistentId + "/balance"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(containsString(nonExistentId)));
    }

    @Test
    void deposit_shouldReturn200_withUpdatedBalance() throws Exception {
        // REQ-4.EARS-1 (cobertura via depósito)
        var accountId = createAccount("owner-4", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void withdraw_shouldReturn200_withUpdatedBalance() throws Exception {
        // REQ-4.EARS-1 (cobertura via saque)
        var accountId = createAccount("owner-4", "100.00");

        mvc.perform(post(BASE + "/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":30.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(70.00));
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
