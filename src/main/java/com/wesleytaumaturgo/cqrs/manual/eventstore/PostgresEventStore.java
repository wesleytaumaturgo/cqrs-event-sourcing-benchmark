package com.wesleytaumaturgo.cqrs.manual.eventstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.OptimisticLockingException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter de persistência para o event store manual.
 *
 * Implementa optimistic locking via expectedVersion explícito.
 * O sequence_number de cada evento é calculado como expectedVersion + 1 + i,
 * e a constraint uk_aggregate_sequence (aggregate_id, sequence_number) garante
 * que appends concorrentes com o mesmo expectedVersion resultem em
 * OptimisticLockingException em vez de corrupção silenciosa de dados.
 */
@Component
public class PostgresEventStore implements EventStore {

    private static final String APPEND_SQL = """
        INSERT INTO domain_events (aggregate_id, aggregate_type, sequence_number, event_type, payload, occurred_at)
        VALUES (?::uuid, ?, ?, ?, ?::jsonb, ?)
        """;

    private final StoredEventRepository repository;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public PostgresEventStore(StoredEventRepository repository,
                              JdbcTemplate jdbc,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void append(AccountId accountId, long expectedVersion, List<DomainEvent> events) {
        String id = accountId.getValue().toString();
        for (int i = 0; i < events.size(); i++) {
            DomainEvent event = events.get(i);
            long sequenceNumber = expectedVersion + 1 + i;
            try {
                jdbc.update(APPEND_SQL,
                    id,
                    "BankAccount",
                    sequenceNumber,
                    event.getClass().getSimpleName(),
                    serialize(event),
                    Timestamp.from(event.occurredAt())
                );
            } catch (DataIntegrityViolationException e) {
                throw new OptimisticLockingException(accountId.getValue(), expectedVersion);
            }
        }
    }

    @Override
    public List<DomainEvent> loadEvents(AccountId accountId) {
        var stored = repository.findByAggregateIdOrderBySequenceNumberAsc(accountId.getValue());
        if (stored.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        return stored.stream()
            .map(e -> deserialize(accountId, e))
            .toList();
    }

    private String serialize(DomainEvent event) {
        try {
            if (event instanceof AccountOpenedEvent e) {
                return objectMapper.writeValueAsString(Map.of(
                    "ownerId", e.ownerId(),
                    "initialBalance", e.initialBalance().getValue().toPlainString(),
                    "occurredAt", e.occurredAt().toString()
                ));
            } else if (event instanceof MoneyDepositedEvent e) {
                return objectMapper.writeValueAsString(Map.of(
                    "amount", e.amount().getValue().toPlainString(),
                    "occurredAt", e.occurredAt().toString()
                ));
            } else if (event instanceof MoneyWithdrawnEvent e) {
                return objectMapper.writeValueAsString(Map.of(
                    "amount", e.amount().getValue().toPlainString(),
                    "occurredAt", e.occurredAt().toString()
                ));
            }
            throw new IllegalArgumentException("Tipo de evento desconhecido: " + event.getClass().getSimpleName());
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar evento: " + event.getClass().getSimpleName(), ex);
        }
    }

    private DomainEvent deserialize(AccountId accountId, StoredEvent stored) {
        try {
            JsonNode node = objectMapper.readTree(stored.getPayload());
            return switch (stored.getEventType()) {
                case "AccountOpenedEvent" -> new AccountOpenedEvent(
                    accountId,
                    node.get("ownerId").asText(),
                    Money.of(new BigDecimal(node.get("initialBalance").asText())),
                    Instant.parse(node.get("occurredAt").asText())
                );
                case "MoneyDepositedEvent" -> new MoneyDepositedEvent(
                    accountId,
                    Money.of(new BigDecimal(node.get("amount").asText())),
                    Instant.parse(node.get("occurredAt").asText())
                );
                case "MoneyWithdrawnEvent" -> new MoneyWithdrawnEvent(
                    accountId,
                    Money.of(new BigDecimal(node.get("amount").asText())),
                    Instant.parse(node.get("occurredAt").asText())
                );
                default -> throw new IllegalArgumentException(
                    "Tipo de evento desconhecido: " + stored.getEventType());
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao desserializar evento id=" + stored.getId(), ex);
        }
    }
}
