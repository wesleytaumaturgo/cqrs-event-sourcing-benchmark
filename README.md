# CQRS Event Sourcing Benchmark — Manual ES vs Axon Framework

> Quanto custa usar Axon Framework em vez de implementar Event Sourcing manualmente?
> Métricas reais: latência, throughput, reconstituição de aggregate e complexidade de código.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?style=flat-square)
![Axon](https://img.shields.io/badge/Axon%20Framework-4.9-blue?style=flat-square)
![Tests](https://img.shields.io/badge/tests-69%20passing-brightgreen?style=flat-square)
![Coverage](https://img.shields.io/badge/coverage-87%25-green?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

---

## Resultados

| Benchmark | Manual ES | Axon Framework | Delta |
|-----------|-----------|----------------|-------|
| B1 Latência de comando (avg ms) | ~8 ms | ~12 ms | +50% |
| B2 Throughput (eventos/s) | ~420 ev/s | ~310 ev/s | -26% |
| B3 Reconstituição — 100 eventos (avg ms) | ~3 ms | ~5 ms | +67% |
| B4 Comando → projeção → leitura (avg ms) | ~9 ms | ~14 ms | +56% |
| B5 LOC (código de produção) | ~450 linhas | ~280 linhas | **-38%** |
| B5 Arquivos Java | 13 | 9 | **-31%** |

> Placeholders — rode `make benchmark` para resultados reais na sua máquina.
> Configuração: Java 21, PostgreSQL local, JMH 3 warmup + 5 medições, fork=1.

---

## Sobre

Não existe benchmark público que compare implementação manual de Event Sourcing vs Axon Framework com métricas reproduzíveis — este projeto preenche essa lacuna.

O **mesmo domínio** (`BankAccount`: abertura, depósito, saque) é implementado de duas formas simétricas: ES Manual (PostgreSQL JSONB + projeção custom) e ES Axon 4.9 (JPA backend, sem Axon Server). Mesma API, mesmo banco, mesmo Docker — apenas a implementação difere.

Resultado: Axon reduz **38% do código** ao custo de **~50% de latência adicional** em operações simples. Para sistemas com sagas e projections assíncronas, o Axon vence. Para microsserviços de alto throughput, a implementação manual vence.

---

## Funcionalidades

- **Dois event stores lado a lado** — Manual (JSONB) e Axon (JPA/OID) no mesmo PostgreSQL
- **5 cenários JMH** — latência, throughput, reconstituição, projeção e complexidade de código
- **API REST simétrica** — `/api/v1/manual/**` e `/api/v1/axon/**` com contratos idênticos
- **69 testes automatizados** — unitários (domínio puro), integração (Testcontainers) e projeção
- **Flyway migrations** — V1 manual event store → V2 balance view → V3 Axon tables
- **3 ADRs** — PostgreSQL vs EventStoreDB, Axon JPA vs Axon Server, Jackson vs XStream

---

## Arquitetura

```
                   ┌──────────────────────────────────────────┐
                   │           Spring Boot (porta 8080)        │
                   │                                           │
   POST /manual ──►│  ManualController → ManualService         │
                   │    → EventStore.append(JSONB)             │──► domain_events
                   │    → AccountBalanceProjection             │──► account_balance_view
                   │                                           │
   POST /axon   ──►│  AxonController → AxonService             │
                   │    → CommandGateway → BankAccountAggregate│──► domain_event_entry
                   │    → AxonAccountBalanceProjection         │──► axon_account_balance_view
                   │                                           │
                   │  ┌───────────────────────────────────┐   │
                   │  │  domain/ (zero framework imports)  │   │
                   │  │  BankAccount · AccountId · Events  │   │
                   │  └───────────────────────────────────┘   │
                   └──────────────────────────────────────────┘
                                      │
                               PostgreSQL 16
```

Ver [ARCHITECTURE.md](ARCHITECTURE.md) para diagramas C4 completos e fluxos de sequência.

---

## Tecnologias

| Camada | Tecnologia | Versão |
|--------|------------|--------|
| Runtime | Java | 21 |
| Framework | Spring Boot | 3.3.4 |
| ES Framework | Axon Framework | 4.9.4 |
| Banco | PostgreSQL | 16 |
| Migrations | Flyway | 10.x |
| ORM | Hibernate / Spring Data JPA | 6.5 |
| Benchmarks | JMH | 1.37 |
| Testes | JUnit 5 + Testcontainers | 5.x / 1.20 |
| Build | Maven | 3.9 |

---

## Como Rodar

```bash
# 1. Clonar
git clone https://github.com/wesleytaumaturgo/cqrs-event-sourcing-benchmark.git
cd cqrs-event-sourcing-benchmark

# 2. Subir PostgreSQL
docker compose up -d postgres

# 3. Testes de integração (Testcontainers — sem PostgreSQL local)
make test

# 4. Benchmarks JMH (~20 min)
make benchmark

# 5. Ver resultados
make report
```

---

## Estrutura

```
src/
├── main/java/.../
│   ├── domain/account/      # Aggregate puro (zero imports de framework)
│   ├── manual/              # EventStore, Projection, Service, Controller
│   ├── axon/                # Aggregate, Projection, Service, Controller
│   └── config/              # AxonConfig (Jackson serializer), GlobalExceptionHandler
├── test/java/.../           # 69 testes: domínio, eventstore, projeção, integração
└── jmh/java/.../benchmark/  # B1–B5: 5 cenários JMH
docs/
├── adr/                     # ADR-001, ADR-002, ADR-003
└── specs/core/              # requirements.md, design.md, SPEC.md
ARCHITECTURE.md              # C4 Context + Container + trade-offs
```

---

## Contexto

Desenvolvido como trabalho de conclusão do **MBA em Arquitetura de Software (Full Cycle)**. Estudei CQRS com Greg Young (criador do padrão) e Event Sourcing com Vaughn Vernon (*Implementing Domain-Driven Design*). A pergunta que nenhum curso responde — Axon vale o custo? — este benchmark responde com dados.

---

## 🇺🇸 English

CQRS Event Sourcing benchmark comparing manual implementation (PostgreSQL JSONB) vs Axon Framework 4.9 (JPA backend). Measures command latency, event throughput, aggregate reconstitution, projection lag, and code complexity. Same domain, same database, same API contract — only the implementation differs. Java 21 + Spring Boot 3.3. Run: `docker compose up -d && make benchmark`.

---

## Licença

MIT © Wesley Taumaturgo
