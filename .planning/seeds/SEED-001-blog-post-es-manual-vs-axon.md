# SEED-001: Blog post comparando ES manual vs Axon com dados reais do benchmark
Data: 2026-03-30
Status: planted
Trigger: quando cqrs-event-sourcing-benchmark estiver publicado (benchmarks rodados, resultados no README)
Target: cqrs-event-sourcing-benchmark

## Ideia
Escrever um blog post técnico usando os dados reais gerados pelo benchmark:
latência de comando, throughput de eventos, tempo de reconstrução de aggregate,
tempo de projeção e custo de complexidade (LOC, classes, configuração).
A pergunta central que nenhum curso responde: "Axon Framework vale a pena?"

## Quando Ativar
- Benchmarks executados e resultados consolidados no README
- GitHub público com código completo e documentado
- ADRs explicando as decisões de implementação de ambas as abordagens

## Contexto
A decisão de usar Axon vs implementação manual é irreversível em produção.
Com dados reais do benchmark, o post se diferencia de todo conteúdo existente
(que é opinativo, sem métricas). Audiência: tech leads e arquitetos avaliando
Event Sourcing pela primeira vez.

## Breadcrumbs
- README.md — contexto e dados do benchmark
- docs/adr/ — decisões arquiteturais documentadas
- benchmark/BenchmarkReport.java — fonte dos dados

## Notas
Plataformas candidatas: Dev.to, Medium (Java/Spring publication), LinkedIn artigo.
Considerar submeter para o Spring Blog ou Axon blog como guest post.
