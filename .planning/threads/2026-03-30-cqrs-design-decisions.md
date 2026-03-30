# Thread: cqrs-design-decisions

Status: active
Criado: 2026-03-30
Atualizado: 2026-03-30

## Objetivo
Decisões de design para implementação CQRS + Event Sourcing no benchmark.

## Contexto Atual

### Domínio
BankAccount com os seguintes casos de uso:
- Abertura de conta
- Depósito
- Saque
- Consulta de saldo
- Extrato (histórico de eventos)

### Implementações
1. **ES Manual** — PostgreSQL como event store + projeções manuais
2. **ES Axon** — Axon Framework (decisão de backend em aberto)

### Cenários de Benchmark
1. Latência de comando (tempo de escrita de evento)
2. Throughput de eventos (eventos/segundo sob carga)
3. Reconstrução de aggregate (replay de eventos)
4. Tempo de projeção (atualização de read model)
5. Custo de complexidade (LOC, boilerplate, manutenibilidade)

## Decisões Tomadas

### D1: Event store para implementação manual — PostgreSQL puro
- Tabela `events` customizada, sem extensão
- Mesma infra que D2 → benchmark compara framework, não banco

### D2: Backend do Axon Framework — PostgreSQL backend (sem Axon Server)
- Axon Framework + spring-boot + JPA sobre PostgreSQL
- Paridade de infra com ES Manual → comparação justa

## Achados

## Próximos Passos
- Decidir D1 e D2 antes de iniciar spec
- Considerar: paridade de infra favorece PostgreSQL em ambas (D1-A + D2-B)

## Log de Sessões

### 2026-03-30
- Thread criada.
- Contexto inicial registrado: domínio BankAccount, 2 implementações, 5 cenários, 2 decisões abertas.
- D1 e D2 decididas: ambas usam PostgreSQL puro, sem Axon Server.
- Prosseguindo com /nova-spec core.
