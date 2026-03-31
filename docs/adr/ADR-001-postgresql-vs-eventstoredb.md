# ADR-001: PostgreSQL JSONB como Event Store Manual vs EventStoreDB

**Status:** ACCEPTED
**Data:** 2026-03-31
**Decisores:** Wesley Taumaturgo
**Contexto do projeto:** `cqrs-event-sourcing-benchmark` — implementação manual de Event Sourcing

---

## Contexto

O projeto requer uma implementação de Event Sourcing manual (sem framework) para ser comparada com a implementação Axon. A implementação manual precisa de um event store durável onde os eventos de domínio possam ser persistidos e recuperados para reconstituição de aggregates.

As duas principais alternativas avaliadas foram:

1. **PostgreSQL com JSONB** — banco relacional já usado pelo restante da aplicação, com suporte nativo a JSON para o payload dos eventos.
2. **EventStoreDB** — banco dedicado a Event Sourcing, desenvolvido pelo time do Greg Young, com suporte a streams, projections e subscriptions nativas.

---

## Decisão

**Usar PostgreSQL com coluna JSONB (`domain_events`) como event store da implementação manual.**

Schema:
```sql
CREATE TABLE domain_events (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_id    UUID         NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB        NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_aggregate_sequence UNIQUE (aggregate_id, sequence_number)
);
```

---

## Motivos

### Paridade de infraestrutura com Axon
O objetivo central do projeto é comparar **custo de implementação e performance** entre a abordagem manual e a abordagem com framework (Axon). Usar o mesmo banco (PostgreSQL) como backend para ambas as implementações elimina variáveis externas, tornando os benchmarks JMH diretamente comparáveis.

Se a implementação manual usasse EventStoreDB e a Axon usasse PostgreSQL, qualquer diferença de performance poderia ser atribuída ao banco, não ao framework — invalidando a comparação.

### Simplicidade operacional
O projeto já requer PostgreSQL para a projeção do Axon (tabelas `axon_account_balance_view`, `domain_event_entry`). Adicionar EventStoreDB introduziria um segundo banco gerenciado no `docker-compose.yml`, aumentando a complexidade de setup sem benefício para o benchmark.

### Sem requisito de features avançadas
EventStoreDB brilha em cenários com:
- Subscriptions concorrentes de múltiplos consumidores
- Projections complexas baseadas em streams
- Clustering e replicação nativa de eventos

Nenhum desses requisitos existe neste projeto. O único caso de uso é: **append + replay por `aggregate_id`** — algo que PostgreSQL suporta eficientemente com índice em `(aggregate_id, sequence_number)`.

### JSONB como formato de payload
PostgreSQL JSONB permite:
- Queries sobre o conteúdo dos eventos sem deserialização no código (`payload->>'amount'`)
- Índices GIN para event sourcing avançado (futuro)
- Integração natural com o ecossistema Spring/JPA

---

## Consequências

### Positivas
- Uma única tecnologia de banco para toda a aplicação
- `docker-compose.yml` simples: apenas `postgres` e `app`
- Benchmarks JMH comparáveis (mesma infraestrutura de I/O)
- Flyway gerencia ambos os schemas em uma única migração

### Negativas
- PostgreSQL não é otimizado para append-only workloads da mesma forma que EventStoreDB
- Sem suporte nativo a subscriptions/projections reativas (compensado com polling ou projeção síncrona)
- Sem garantia de ordering global entre aggregates (apenas ordering por `sequence_number` dentro de um aggregate)

### Neutras
- O `PostgresEventStore` implementa a interface `EventStore` (port hexagonal), permitindo trocar a implementação por EventStoreDB no futuro sem alterar o domínio.

---

## Alternativas descartadas

| Alternativa | Motivo do descarte |
|-------------|-------------------|
| EventStoreDB | Introduz segunda tecnologia de banco; invalida comparação de benchmark |
| PostgreSQL `BYTEA` (eventos serializados em binário) | JSONB é mais flexível para inspeção e consulta de eventos |
| H2 (in-memory) | Sem persistência real; não adequado para benchmark de I/O |
