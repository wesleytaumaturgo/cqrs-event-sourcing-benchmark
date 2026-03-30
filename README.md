# Otimização de Event Sourcing — CQRS Benchmark

> Benchmark comparativo de Event Sourcing: implementação manual vs Axon Framework com métricas de latência, throughput e complexidade. Java 21 + Spring Boot 3.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?style=for-the-badge)
![Axon](https://img.shields.io/badge/Axon%20Framework-4.9-blue?style=for-the-badge)
![Licença](https://img.shields.io/badge/Licença-MIT-blue?style=for-the-badge)

## Sobre

Migrar para Event Sourcing é a decisão arquitetural mais cara e irreversível em enterprise. Uma vez que o event store é a fonte de verdade, não dá pra voltar atrás. Mas não existe benchmark público que compare as duas abordagens mais comuns — implementação manual vs Axon Framework — com métricas reais.

Este projeto implementa o mesmo domínio (BankAccount) de duas formas: ES manual (PostgreSQL event store + projeções custom) e ES com Axon Framework. Os 5 cenários de benchmark medem latência de comando, throughput de eventos, tempo de reconstrução de aggregate, tempo de projeção, e custo de complexidade (linhas de código, classes, configuração).

## Como Rodar
```bash
git clone https://github.com/wesleytaumaturgo/cqrs-event-sourcing-benchmark.git
cd cqrs-event-sourcing-benchmark
docker-compose up -d
make benchmark
make report
```

## Contexto

Este projeto nasceu do MBA em Arquitetura de Software (Full Cycle) onde estudei CQRS com Greg Young (criador do padrão) e Event Sourcing com Vaughn Vernon. A pergunta que nenhum curso responde: "Axon Framework vale a pena ou implementação manual é melhor?" Este benchmark responde com dados.

## 🇺🇸 English

CQRS Event Sourcing benchmark comparing manual implementation vs Axon Framework. Measures command latency, event throughput, aggregate reconstruction time, projection latency, and code complexity. Java 21 + Spring Boot 3.3. Run: `docker-compose up -d && make benchmark`.

## Licença

MIT
