# Validation Contract — BC core
Gerado: 2026-03-30
Status: draft

Rastreabilidade: docs/specs/core/requirements.md → testes automatizados

---

## Mapeamento EARS → Teste Automatizado

| EARS ID | Requirement resumido | Verify Command | Status |
|---------|---------------------|----------------|--------|
| REQ-1.EARS-1 | Abertura de conta válida → 201 + evento persiste | `mvn test -Dtest=BankAccountTest#openAccount_shouldEmitAccountOpenedEvent` | pending |
| REQ-1.EARS-2 | Rejeitar initialBalance negativo → 400 | `mvn test -Dtest=BankAccountTest#openAccount_shouldReject_whenInitialBalanceNegative` | pending |
| REQ-1.EARS-3 | Rejeitar ownerId em branco → 400 | `mvn test -Dtest=BankAccountTest#openAccount_shouldReject_whenOwnerIdBlank` | pending |
| REQ-2.EARS-1 | Depósito válido → 200 + evento persiste | `mvn test -Dtest=BankAccountTest#deposit_shouldEmitMoneyDepositedEvent` | pending |
| REQ-2.EARS-2 | Rejeitar amount <= 0 no depósito → 400 | `mvn test -Dtest=BankAccountTest#deposit_shouldReject_whenAmountNotPositive` | pending |
| REQ-2.EARS-3 | Depósito em conta inexistente → 404 | `mvn test -Dtest=PostgresEventStoreTest#deposit_shouldReturn404_whenAccountNotFound` | pending |
| REQ-3.EARS-1 | Saque válido com saldo suficiente → 200 | `mvn test -Dtest=BankAccountTest#withdraw_shouldEmitMoneyWithdrawnEvent` | pending |
| REQ-3.EARS-2 | Rejeitar saque com saldo insuficiente → 422 | `mvn test -Dtest=BankAccountTest#withdraw_shouldReject_whenInsufficientFunds` | pending |
| REQ-3.EARS-3 | Rejeitar amount <= 0 no saque → 400 | `mvn test -Dtest=BankAccountTest#withdraw_shouldReject_whenAmountNotPositive` | pending |
| REQ-3.EARS-4 | Saque em conta inexistente → 404 | `mvn test -Dtest=PostgresEventStoreTest#withdraw_shouldReturn404_whenAccountNotFound` | pending |
| REQ-3.EARS-5 | Saque exato ao saldo → 200 com balance=0 | `mvn test -Dtest=BankAccountTest#withdraw_shouldSucceed_whenAmountEqualsBalance` | pending |
| REQ-4.EARS-1 | Saldo lido da projeção (sem replay) → 200 | `mvn test -Dtest=AccountBalanceProjectionTest#getBalance_shouldReadFromProjection_notFromEventStore` | pending |
| REQ-4.EARS-2 | Saldo de conta inexistente → 404 | `mvn test -Dtest=AccountBalanceProjectionTest#getBalance_shouldReturn404_whenAccountNotFound` | pending |
| REQ-5.EARS-1 | Extrato retorna eventos em ordem de sequence_number | `mvn test -Dtest=PostgresEventStoreTest#getEvents_shouldReturnEventsInOrder` | pending |
| REQ-5.EARS-2 | Extrato de conta inexistente → 404 | `mvn test -Dtest=PostgresEventStoreTest#getEvents_shouldReturn404_whenAccountNotFound` | pending |
| REQ-6.EARS-1 | Paridade de contrato Manual vs Axon (mesmo status/shape) | `mvn test -Dtest=ManualAccountIntegrationTest,AxonAccountIntegrationTest` | pending |
| REQ-6.EARS-2 | Ambas as implementações persistem evento no PostgreSQL | `mvn test -Dtest=ManualAccountIntegrationTest#command_shouldPersistEvent,AxonAccountIntegrationTest#command_shouldPersistEvent` | pending |
| REQ-7.EARS-1 | Reconstitui aggregate via replay de N eventos | `mvn test -Dtest=BankAccountTest#reconstitute_shouldReplayAllEvents` | pending |
| REQ-7.EARS-2 | Replay não escreve no event store nem na projeção | `mvn test -Dtest=BankAccountTest#reconstitute_shouldNotWriteDuringReplay` | pending |
| REQ-8.EARS-1 | JMH gera target/jmh-result.json com todos os benchmarks | `mvn verify -P benchmark -Djmh.includes=".*Benchmark" -Djmh.rf=json` | pending |
| REQ-8.EARS-2 | Suite JMH executável via `mvn verify -P benchmark` sem config manual | `mvn verify -P benchmark` (CI smoke) | pending |
| NFR-4 | Cobertura de linhas ≥ 70% | `mvn verify -P coverage && cat target/site/jacoco/index.html \| grep -E "Total.*[0-9]+%"` | pending |
| NFR-5 | `mvn clean test` passa do zero com apenas PostgreSQL up | `docker-compose up -d postgres && mvn clean test` | pending |

---

## Gaps

Não há EARS sem verify command viável nesta versão.

Observações:
- **NFR-1** (latência p95 < 50ms): não verificável via `mvn test` unitário — coberto pelo benchmark B1 (JMH).
  Verify: `mvn verify -P benchmark -Djmh.includes="CommandLatencyBenchmark"` + assert manual no JSON.
- **NFR-2** (throughput ≥ 500 events/s): coberto pelo benchmark B2 — assert no relatório JMH.
- **NFR-3** (reconstituição < 10ms p95): coberto pelo benchmark B3 — assert no relatório JMH.
- **NFR-6** (startup < 15s): verificável via `docker-compose up` + health check com timeout de 15s.

---

## Wave 0 — Test Scaffolding

Testes que precisam ser criados **antes** da implementação (fase RED do TDD):

| Arquivo | Cobre | Prioridade |
|---------|-------|-----------|
| `src/test/java/com/benchmark/domain/account/BankAccountTest.java` | REQ-1, REQ-2, REQ-3, REQ-7 | P0 — domínio puro, sem dependências |
| `src/test/java/com/benchmark/manual/eventstore/PostgresEventStoreTest.java` | REQ-2.EARS-3, REQ-3.EARS-4, REQ-5 | P1 — requer Testcontainers/PostgreSQL |
| `src/test/java/com/benchmark/manual/projection/AccountBalanceProjectionTest.java` | REQ-4 | P1 — requer projeção implementada |
| `src/test/java/com/benchmark/axon/aggregate/BankAccountAggregateTest.java` | REQ-1, REQ-2, REQ-3 (Axon) | P1 — requer Axon test fixtures |
| `src/test/java/com/benchmark/axon/projection/AxonAccountBalanceProjectionTest.java` | REQ-4 (Axon) | P2 — requer projeção Axon |
| `src/test/java/com/benchmark/integration/ManualAccountIntegrationTest.java` | REQ-6 | P2 — requer app completa + DB |
| `src/test/java/com/benchmark/integration/AxonAccountIntegrationTest.java` | REQ-6 | P2 — requer app completa + DB |

**Ordem de execução recomendada:**
1. `BankAccountTest` — domínio puro (sem infra)
2. `BankAccountAggregateTest` — aggregate Axon com `AggregateTestFixture`
3. `PostgresEventStoreTest` + `AccountBalanceProjectionTest` — com Testcontainers
4. `AxonAccountBalanceProjectionTest` — com Testcontainers
5. Integration tests — com `@SpringBootTest` + Testcontainers

---

## Como atualizar este arquivo

Ao passar um teste de `pending` para `passing`, atualizar a coluna Status:
- `pending` → aguardando implementação
- `passing` → teste existe e passa no CI
- `skipped` → deliberadamente não testado (com justificativa)
- `blocked` → bloqueado por dependência externa (documentar motivo)
