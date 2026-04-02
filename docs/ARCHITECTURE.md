# Architecture — CQRS / Event Sourcing Benchmark

> Referências: [ADR-001](adr/ADR-001-postgresql-vs-eventstoredb.md) · [ADR-002](adr/ADR-002-axon-postgresql-vs-axon-server.md) · [ADR-003](adr/ADR-003-jackson-vs-xstream-axon-serializer.md)

---

## C4 — Level 1: System Context

```
┌────────────────────────────────────────────────────────────────┐
│                       External User / Client                   │
│                  (REST HTTP — curl / benchmark tool)           │
└────────────────────────────┬───────────────────────────────────┘
                             │ HTTP/REST
                             ▼
          ┌──────────────────────────────────────┐
          │   cqrs-event-sourcing-benchmark       │
          │                                      │
          │  Exposes two parallel implementations│
          │  of CQRS + Event Sourcing for        │
          │  performance comparison via JMH.     │
          │                                      │
          │  Runtime: Java 21 / Spring Boot 3    │
          └──────────────────────────────────────┘
                             │
                             │ JDBC / JPA
                             ▼
          ┌──────────────────────────────────────┐
          │           PostgreSQL 16               │
          │  - domain_events (manual ES)         │
          │  - account_balance_view (projection) │
          │  - axon_* tables (Axon ES backend)   │
          └──────────────────────────────────────┘
```

---

## C4 — Level 2: Container

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application (JVM)                        │
│                                                                              │
│  ┌──────────────────────────────┐  ┌──────────────────────────────────────┐ │
│  │    Manual CQRS/ES Stack       │  │          Axon Framework Stack         │ │
│  │  ──────────────────────────  │  │  ──────────────────────────────────  │ │
│  │  ManualAccountController      │  │  AxonAccountController                │ │
│  │  POST /api/v1/manual/...      │  │  POST /api/v1/axon/...               │ │
│  │         │                    │  │         │                            │ │
│  │  ManualAccountService         │  │  AxonAccountService                  │ │
│  │  (loads + saves aggregates)   │  │  (dispatches Axon commands)          │ │
│  │         │          │         │  │         │                            │ │
│  │  BankAccount    EventStore    │  │  BankAccountAggregate                 │ │
│  │  (domain agg)  (port)        │  │  (@Aggregate — Axon managed)         │ │
│  │                   │          │  │         │                            │ │
│  │  PostgresEventStore           │  │  AxonAccountBalanceProjection         │ │
│  │  (JDBC adapter)               │  │  (@EventHandler — subscribing)       │ │
│  │                   │          │  │         │                            │ │
│  │  AccountBalanceProjection     │  │  AxonBalanceViewRepository           │ │
│  │  (query handler)              │  │  (JPA — axon_balance_view)           │ │
│  └──────────────────────────────┘  └──────────────────────────────────────┘ │
│                    │                               │                         │
│              JdbcTemplate                     Spring Data JPA                │
└────────────────────┼───────────────────────────────┼─────────────────────── ┘
                     │                               │
                     ▼                               ▼
         ┌──────────────────────────────────────────────────────┐
         │                    PostgreSQL 16                       │
         │                                                        │
         │  domain_events           │  account_balance_view       │
         │  (aggregate_id, seq,     │  (materialized read model   │
         │   event_type, payload)   │   for manual projection)    │
         │  UNIQUE(aggregate_id,    │                             │
         │   sequence_number)       │  axon_domain_event_entry    │
         │                         │  axon_token_entry            │
         │                         │  axon_snapshot_entry         │
         │                         │  axon_balance_view           │
         └──────────────────────────────────────────────────────┘
```

> **ADR-001**: PostgreSQL escolhido sobre EventStoreDB por simplicidade e zero infra adicional.
> **ADR-002**: Axon Server substituído por PostgreSQL backend; processamento subscribing para evitar conflito OID/BYTEA em `token_entry`.

---

## C4 — Level 3: Component — Manual CQRS/ES

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Bounded Context: Manual CQRS/ES                                         │
│                                                                           │
│  ┌──────────────┐   ┌──────────────────┐   ┌────────────────────────┐   │
│  │   adapter/   │   │    service/       │   │      domain/           │   │
│  │              │   │                  │   │                        │   │
│  │  Manual-     │──▶│  ManualAccount-  │──▶│  BankAccount           │   │
│  │  Account-    │   │  Service         │   │  (aggregate)           │   │
│  │  Controller  │   │                  │   │                        │   │
│  └──────────────┘   │  loadEvents()    │   │  AccountId (VO)        │   │
│                     │  reconstitute()  │   │  Money (VO)            │   │
│  ┌──────────────┐   │  append(v, evts) │   │                        │   │
│  │ common/dto/  │   └────────┬─────────┘   │  Commands:             │   │
│  │              │            │             │  - OpenAccountCommand  │   │
│  │  OpenAccount │            │             │  - DepositMoneyCommand │   │
│  │  Request     │     ┌──────┴──────┐      │  - WithdrawMoneyCommand│   │
│  │  BalanceRsp  │     │  EventStore │      │                        │   │
│  └──────────────┘     │  (port)     │      │  Events:               │   │
│                       └──────┬──────┘      │  - AccountOpenedEvent  │   │
│                              │             │  - MoneyDepositedEvent │   │
│                    ┌─────────┴──────────┐  │  - MoneyWithdrawnEvent │   │
│                    │ PostgresEventStore │  │                        │   │
│                    │ (JDBC adapter)     │  │  Exceptions:           │   │
│                    │                   │  │  - AccountNotFoundException│ │
│                    │ UNIQUE constraint │  │  - InsufficientFunds   │   │
│                    │ prevents OL race  │  │  - OptimisticLocking   │   │
│                    └───────────────────┘  └────────────────────────┘   │
│                                                                           │
│  ┌─────────────────────────────────┐                                     │
│  │  projection/                    │                                     │
│  │  AccountBalanceProjection        │                                     │
│  │  onAccountOpened() / onDeposited│                                     │
│  │  onWithdrawn() / getBalance()   │                                     │
│  │  log.warn for unknown accounts  │                                     │
│  └─────────────────────────────────┘                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Command → Aggregate → EventStore → Projection Flow

### Manual Implementation

```
Client
  │
  │  POST /api/v1/manual/accounts/{id}/deposits
  │  { "amount": 100.00 }
  ▼
ManualAccountController
  │  deposit(id, amount)
  ▼
ManualAccountService
  │
  ├─1─▶ EventStore.loadEvents(accountId)
  │       │
  │       └─▶ PostgresEventStore
  │               └─▶ SELECT * FROM domain_events
  │                   WHERE aggregate_id = ? ORDER BY sequence_number ASC
  │                   → [AccountOpenedEvent, ...]
  │
  ├─2─▶ BankAccount.reconstitute(accountId, events)
  │       └─▶ apply(AccountOpenedEvent) → version=0
  │
  ├─3─▶ account.deposit(DepositMoneyCommand)
  │       └─▶ validate amount > 0
  │           raiseEvent(MoneyDepositedEvent)
  │
  ├─4─▶ EventStore.append(accountId, version=0, [MoneyDepositedEvent])
  │       └─▶ PostgresEventStore
  │               └─▶ INSERT INTO domain_events
  │                   (aggregate_id, sequence_number=1, ...)
  │                   ← uk_aggregate_sequence rejects concurrent seq=1
  │                   ← throws OptimisticLockingException → HTTP 409
  │
  ├─5─▶ AccountBalanceProjection.onMoneyDeposited(event)
  │       └─▶ UPDATE account_balance_view SET balance += amount
  │
  └─6─▶ return AccountBalanceView → BalanceResponse (HTTP 200)
```

### Axon Framework Implementation

```
Client
  │
  │  POST /api/v1/axon/accounts/{id}/deposits
  ▼
AxonAccountController
  │  deposit(id, amount)
  ▼
AxonAccountService
  │
  ├─1─▶ commandGateway.sendAndWait(DepositMoneyAxonCommand)
  │       │
  │       └─▶ Axon CommandBus
  │               └─▶ BankAccountAggregate (@CommandHandler)
  │                       ├─▶ validate amount > 0
  │                       └─▶ AggregateLifecycle.apply(MoneyDepositedEvent)
  │                               └─▶ Axon persists to axon_domain_event_entry
  │
  ├─2─▶ AxonAccountBalanceProjection (@EventHandler — subscribing)
  │       └─▶ on(MoneyDepositedEvent)
  │               └─▶ UPDATE axon_balance_view SET balance += amount
  │
  └─3─▶ projection.getBalance(accountId) → AxonBalanceView → HTTP 200
```

> **ADR-003**: Jackson serializer escolhido sobre XStream por segurança e performance.

---

## Bounded Contexts

| Pacote | Responsabilidade |
|---|---|
| `domain.account` | Aggregate `BankAccount`, Value Objects (`AccountId`, `Money`), Commands, Events, Exceptions — **zero imports de framework** |
| `manual.eventstore` | Port `EventStore` + adapter `PostgresEventStore` — persiste eventos com optimistic locking via UNIQUE constraint |
| `manual.projection` | `AccountBalanceProjection` — mantém `account_balance_view` sincronizado com eventos |
| `manual.service` | Orquestra load → reconstitute → command → append → project |
| `manual.adapter` | `ManualAccountController` — HTTP layer, `ResponseEntity<T>` |
| `axon.aggregate` | `BankAccountAggregate` — equivalente ao domain model, gerenciado pelo Axon |
| `axon.projection` | `AxonAccountBalanceProjection` — `@EventHandler` em modo subscribing |
| `axon.service` | `AxonAccountService` — envia comandos ao `CommandGateway` |
| `axon.adapter` | `AxonAccountController` — HTTP layer, espelhado ao manual |
| `config` | `AxonConfig` (PostgreSQL backend, subscribing processor), `GlobalExceptionHandler` |
| `adapter.common.dto` | DTOs compartilhados entre os dois controllers |

### Regra de dependência (enforçada por ArchUnit)

```
domain/ ←── manual/  ←── adapter/
domain/ ←── axon/    ←── adapter/

domain/ não importa nada fora de domain/
```

---

## Módulo de Benchmarks JMH

```
src/jmh/java/
└── benchmark/
    ├── CommandLatencyBenchmark.java       (B1 — latência end-to-end)
    ├── ThroughputBenchmark.java           (B2 — throughput op/s)
    ├── EventReconstitutionBenchmark.java  (B3 — replay de histórico)
    ├── ProjectionUpdateBenchmark.java     (B4 — custo de projeção)
    └── ComplexityCostAnalysis.java        (B5 — overhead por operação)
```

Execução: `mvn verify -P benchmark`
Resultado: `target/jmh-result.json`

---

## ADR Cross-Reference

| ADR | Decisão | Status |
|---|---|---|
| [ADR-001](adr/ADR-001-postgresql-vs-eventstoredb.md) | PostgreSQL puro como event store (vs EventStoreDB) | ACCEPTED |
| [ADR-002](adr/ADR-002-axon-postgresql-vs-axon-server.md) | Axon com backend PostgreSQL + SubscribingEventProcessor | ACCEPTED |
| [ADR-003](adr/ADR-003-jackson-vs-xstream-axon-serializer.md) | Jackson como serializador Axon (vs XStream) | ACCEPTED |

### Decisões implícitas documentadas no código

- **Optimistic locking via `expectedVersion`**: `EventStore.append(accountId, expectedVersion, events)` — a constraint `uk_aggregate_sequence(aggregate_id, sequence_number)` garante rejeição atômica de appends concorrentes com a mesma versão (→ `OptimisticLockingException` / HTTP 409).
- **SubscribingEventProcessor**: elimina escritas em `token_entry`, evitando conflito BYTEA/OID do Hibernate 6 com PostgreSQL.
- **Flyway**: 3 migrações versionadas (`V1` manual ES, `V2` balance view, `V3` Axon tables).
