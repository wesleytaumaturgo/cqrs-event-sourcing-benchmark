# ADR-002: Axon Framework com PostgreSQL JPA Backend vs Axon Server

**Status:** ACCEPTED
**Data:** 2026-03-31
**Decisores:** Wesley Taumaturgo
**Contexto do projeto:** `cqrs-event-sourcing-benchmark` — implementação Axon Framework 4.9

---

## Contexto

O Axon Framework suporta dois modos de operação para persistência de eventos e roteamento de comandos/eventos:

1. **Axon Server** — servidor dedicado (processo separado) que atua como message broker e event store. É o modo padrão do Axon Framework.
2. **PostgreSQL JPA Backend** — usa o banco relacional da aplicação como event store, via configuração `EmbeddedEventStore` + `JpaEventStorageEngine`.

O projeto precisava escolher um backend para a implementação Axon antes de iniciar o desenvolvimento.

---

## Decisão

**Usar PostgreSQL JPA como backend do Axon Framework, sem Axon Server.**

Configuração no `pom.xml`:
```xml
<exclusions>
  <exclusion>
    <groupId>org.axonframework</groupId>
    <artifactId>axon-server-connector</artifactId>
  </exclusion>
</exclusions>
```

`AxonConfig.java` configura `EmbeddedEventStore` com `JpaEventStorageEngine` via auto-configuration do Spring Boot.

---

## Motivos

### Paridade de infraestrutura com implementação manual
Assim como o ADR-001, usar o mesmo banco (PostgreSQL) para o event store do Axon elimina variáveis externas nos benchmarks JMH. Se o Axon usasse Axon Server e o manual usasse PostgreSQL, qualquer diferença de performance mediria "PostgreSQL vs Axon Server", não "implementação manual vs framework".

### Redução de complexidade operacional
Axon Server exige:
- Um processo Java separado rodando (`axonserver.jar`, porta 8024/8124)
- Configuração de host/porta no `axonserver.properties`
- Gerenciamento de ciclo de vida (start, stop, health check) no CI

Sem Axon Server, o `docker-compose.yml` tem apenas `postgres` e `app` — idêntico à implementação manual.

### Sem requisito de features do Axon Server
Axon Server oferece:
- Dashboard de monitoramento de eventos/comandos
- Dead Letter Queue (DLQ) com retry
- Event sourcing cross-service com tracking global
- Clustering multi-nó do event store

Nenhum desses recursos é necessário para o benchmark. O projeto usa um único aggregate (`BankAccount`) com operações de depósito/saque — cenário que PostgreSQL JPA atende completamente.

### Compatibilidade com ambientes CI/CD restritivos
Ambientes CI (GitHub Actions) geralmente permitem PostgreSQL via `services:` nativo. Axon Server exigiria um container adicional ou download do JAR durante o workflow, complicando o pipeline.

---

## Consequências

### Positivas
- CI/CD simples: apenas PostgreSQL como serviço externo
- Sem porta adicional (8124) para gerenciar
- Event store do Axon persiste na mesma instância PostgreSQL da aplicação (facilita inspeção via SQL)
- Flyway gerencia o schema do Axon (tabelas `domain_event_entry`, `snapshot_event_entry`, `token_entry`)

### Negativas
- Sem dashboard visual de eventos/comandos (Axon Server oferece UI web)
- Performance do event store limitada pela capacidade do PostgreSQL (vs Axon Server otimizado para event sourcing)
- Sem suporte a clustering multi-instância sem configuração adicional
- `TrackingEventProcessor` usa `token_entry` no PostgreSQL (implica contention em alta concorrência)

### Observação sobre `token_entry` e `BYTEA vs OID`
Durante a implementação (TASK-007), identificamos que Hibernate 6 mapeia `@Lob byte[]` para OID (PostgreSQL large object) em vez de BYTEA. O schema V3 foi ajustado para usar OID nas colunas `meta_data`, `payload` e `token`. Ver ADR-003 para a decisão de serialização relacionada.

---

## Alternativas descartadas

| Alternativa | Motivo do descarte |
|-------------|-------------------|
| Axon Server (modo padrão) | Introduz dependência de infra extra; invalida comparação de benchmark |
| Axon Server Cloud | SaaS externo; não adequado para benchmark local reproduzível |
| MongoDB backend para Axon | Introduz terceira tecnologia de banco; fora do escopo do projeto |
