# CQRS Event Sourcing Benchmark — Manual ES vs Axon Framework

> Benchmark comparativo de Event Sourcing: implementação manual vs Axon Framework com métricas de latência, throughput e complexidade.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?style=for-the-badge)
![Axon](https://img.shields.io/badge/Axon%20Framework-4.9-blue?style=for-the-badge)
![Licença](https://img.shields.io/badge/Licença-MIT-blue?style=for-the-badge)

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

> Resultados variam conforme hardware. Rode `make benchmark` para medir na sua máquina.
> Configuração: Java 21, PostgreSQL local, JMH 3 warmup + 5 medições, fork=1.

---

## Sobre

Não existe benchmark público que compare implementação manual de Event Sourcing vs Axon Framework com métricas reproduzíveis — este projeto preenche essa lacuna.

O **mesmo domínio** (`BankAccount`: abertura, depósito, saque) é implementado de duas formas simétricas: ES Manual (PostgreSQL JSONB + projeção custom) e ES Axon 4.9 (JPA backend, sem Axon Server). Mesma API, mesmo banco, mesmo Docker — apenas a implementação difere.

Resultado: Axon reduz **38% do código** ao custo de **~50% de latência adicional** em operações simples. Para sistemas com sagas e projections assíncronas, o Axon vence. Para microsserviços de alto throughput, a implementação manual vence.

---

## Funcionalidades

- 📊 **5 cenários JMH** comparativos — latência, throughput, reconstituição, projeção e complexidade
- 🏗️ **Duas implementações completas** lado a lado com o mesmo domínio e API
- 🔄 **API REST simétrica** — `/api/v1/manual/**` e `/api/v1/axon/**` com contratos idênticos
- ✅ **69 testes** — unitários (domínio puro), integração (Testcontainers) e projeção
- 📄 **3 ADRs** documentando decisões arquiteturais (PostgreSQL, Axon JPA, Jackson)
- 🐳 **Docker Compose** — PostgreSQL sobe em 1 comando, sem configuração manual

---

## Arquitetura

```
                   ┌──────────────────────────────────────────┐
                   │           Spring Boot (porta 8080)        │
   POST /manual ──►│  ManualController → ManualService         │
                   │    → EventStore.append(JSONB)             │──► domain_events
                   │    → AccountBalanceProjection             │──► account_balance_view
   POST /axon   ──►│  AxonController → AxonService             │
                   │    → CommandGateway → BankAccountAggregate│──► domain_event_entry
                   │    → AxonAccountBalanceProjection         │──► axon_account_balance_view
                   │  ┌───────────────────────────────────┐   │
                   │  │  domain/ (zero framework imports)  │   │
                   │  └───────────────────────────────────┘   │
                   └──────────────────────────────────────────┘
                                      │ PostgreSQL 16
```

Ver [ARCHITECTURE.md](ARCHITECTURE.md) para diagramas C4 completos e fluxos de sequência.

---

## Tecnologias

Java 21 · Spring Boot 3.3.4 · Axon Framework 4.9.4 · PostgreSQL 16 · Flyway 10 · JMH 1.37 · JUnit 5 + Testcontainers · Maven 3.9

---

## Como Rodar

```bash
git clone https://github.com/wesleytaumaturgo/cqrs-event-sourcing-benchmark.git
cd cqrs-event-sourcing-benchmark
docker compose up -d postgres   # sobe PostgreSQL
make test                        # 69 testes (Testcontainers)
make benchmark                   # benchmarks JMH (~20 min)
make report                      # exibe target/jmh-result.json
```

---

## Estrutura

```
src/main/java/
├── domain/account/    # Aggregate puro (zero imports de framework)
├── manual/            # EventStore JSONB, Projection, Service, Controller
├── axon/              # @Aggregate, @EventHandler, Service, Controller
├── benchmark/         # 5 cenários JMH (B1–B5)
└── config/            # AxonConfig, GlobalExceptionHandler
docs/adr/              # ADR-001, ADR-002, ADR-003
ARCHITECTURE.md        # C4 + trade-offs
```

---

## Contexto

Em 12 anos de Java enterprise, trabalhei com sistemas onde auditabilidade de transações e reconstrução de estado eram requisitos críticos. No **MBA em Arquitetura de Software (Full Cycle)**, estudei CQRS com Greg Young (criador do padrão) e Event Sourcing com Vaughn Vernon (*Implementing Domain-Driven Design*). A pergunta que nenhum curso responde — Axon vale o custo? — este benchmark responde com dados.

---

## 🇺🇸 English

CQRS Event Sourcing benchmark comparing manual implementation (PostgreSQL JSONB) vs Axon Framework 4.9 (JPA backend). Measures command latency, event throughput, aggregate reconstitution, projection lag, and code complexity. Same domain, same database, same API — only the implementation differs. Java 21 + Spring Boot 3.3. Run: `docker compose up -d && make benchmark`.

---

## Licença

MIT © Wesley Taumaturgo
