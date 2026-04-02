package com.wesleytaumaturgo.cqrs.domain.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountIdTest {

    @Test
    void generate_shouldProduceDifferentIds() {
        var id1 = AccountId.generate();
        var id2 = AccountId.generate();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void of_uuid_shouldWrapValue() {
        var uuid = UUID.randomUUID();
        var accountId = AccountId.of(uuid);
        assertThat(accountId.getValue()).isEqualTo(uuid);
    }

    @Test
    void of_string_shouldParseUUID() {
        var uuid = UUID.randomUUID();
        var accountId = AccountId.of(uuid.toString());
        assertThat(accountId.getValue()).isEqualTo(uuid);
    }

    @Test
    void equals_shouldBeBasedOnValue() {
        var uuid = UUID.randomUUID();
        assertThat(AccountId.of(uuid)).isEqualTo(AccountId.of(uuid));
        assertThat(AccountId.of(uuid)).isNotEqualTo(AccountId.generate());
    }

    @Test
    void hashCode_shouldBeConsistentWithEquals() {
        var uuid = UUID.randomUUID();
        assertThat(AccountId.of(uuid).hashCode()).isEqualTo(AccountId.of(uuid).hashCode());
    }

    @Test
    void toString_shouldReturnUUIDString() {
        var uuid = UUID.randomUUID();
        assertThat(AccountId.of(uuid).toString()).isEqualTo(uuid.toString());
    }
}
