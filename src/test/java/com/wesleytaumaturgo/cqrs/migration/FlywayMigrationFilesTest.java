package com.wesleytaumaturgo.cqrs.migration;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que os 3 scripts de migration Flyway existem no classpath
 * e estão na ordem correta de execução.
 * Sem EARS — task de infraestrutura (TASK-002).
 */
class FlywayMigrationFilesTest {

    @Test
    void V1_manualEventStore_migration_shouldExist() {
        URL resource = getClass().getClassLoader()
            .getResource("db/migration/V1__manual_event_store.sql");
        assertThat(resource)
            .as("V1__manual_event_store.sql deve existir em resources/db/migration/")
            .isNotNull();
    }

    @Test
    void V2_accountBalanceView_migration_shouldExist() {
        URL resource = getClass().getClassLoader()
            .getResource("db/migration/V2__account_balance_view.sql");
        assertThat(resource)
            .as("V2__account_balance_view.sql deve existir em resources/db/migration/")
            .isNotNull();
    }

    @Test
    void V3_axonEventStore_migration_shouldExist() {
        URL resource = getClass().getClassLoader()
            .getResource("db/migration/V3__axon_event_store.sql");
        assertThat(resource)
            .as("V3__axon_event_store.sql deve existir em resources/db/migration/")
            .isNotNull();
    }

    @Test
    void V1_shouldContainDomainEventsTable() throws Exception {
        URL resource = getClass().getClassLoader()
            .getResource("db/migration/V1__manual_event_store.sql");
        assertThat(resource).isNotNull();
        String content = new String(resource.openStream().readAllBytes());
        assertThat(content)
            .contains("domain_events")
            .contains("aggregate_id")
            .contains("sequence_number")
            .contains("payload");
    }

    @Test
    void V2_shouldContainAccountBalanceViewTable() throws Exception {
        URL resource = getClass().getClassLoader()
            .getResource("db/migration/V2__account_balance_view.sql");
        assertThat(resource).isNotNull();
        String content = new String(resource.openStream().readAllBytes());
        assertThat(content)
            .contains("account_balance_view")
            .contains("account_id")
            .contains("balance");
    }

    @Test
    void V3_shouldContainAxonTables() throws Exception {
        URL resource = getClass().getClassLoader()
            .getResource("db/migration/V3__axon_event_store.sql");
        assertThat(resource).isNotNull();
        String content = new String(resource.openStream().readAllBytes());
        assertThat(content)
            .contains("domain_event_entry")
            .contains("snapshot_event_entry")
            .contains("token_entry");
    }
}
