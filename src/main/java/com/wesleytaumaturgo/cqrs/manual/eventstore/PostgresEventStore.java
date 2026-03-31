package com.wesleytaumaturgo.cqrs.manual.eventstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.events.AccountOpenedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.DomainEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyDepositedEvent;
import com.wesleytaumaturgo.cqrs.domain.account.events.MoneyWithdrawnEvent;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class PostgresEventStore implements EventStore {

    private final StoredEventRepository repository;
    private final ObjectMapper objectMapper;

    public PostgresEventStore(StoredEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(AccountId accountId, List<DomainEvent> events) {
        long nextSeq = repository.findMaxSequenceNumber(accountId.getValue())
            .map(n -> n + 1)
            .orElse(0L);

        for (DomainEvent event : events) {
            repository.save(new StoredEvent(
                accountId.getValue(),
                "BankAccount",
                nextSeq++,
                event.getClass().getSimpleName(),
                serialize(event),
                event.occurredAt()
            ));
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
                    "initialBalance", e.initialBalance().toPlainString(),
                    "occurredAt", e.occurredAt().toString()
                ));
            } else if (event instanceof MoneyDepositedEvent e) {
                return objectMapper.writeValueAsString(Map.of(
                    "amount", e.amount().toPlainString(),
                    "occurredAt", e.occurredAt().toString()
                ));
            } else if (event instanceof MoneyWithdrawnEvent e) {
                return objectMapper.writeValueAsString(Map.of(
                    "amount", e.amount().toPlainString(),
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
                    new BigDecimal(node.get("initialBalance").asText()),
                    Instant.parse(node.get("occurredAt").asText())
                );
                case "MoneyDepositedEvent" -> new MoneyDepositedEvent(
                    accountId,
                    new BigDecimal(node.get("amount").asText()),
                    Instant.parse(node.get("occurredAt").asText())
                );
                case "MoneyWithdrawnEvent" -> new MoneyWithdrawnEvent(
                    accountId,
                    new BigDecimal(node.get("amount").asText()),
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
