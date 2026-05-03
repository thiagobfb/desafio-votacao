# Spec 004 — Performance e suporte a alto volume

**Refere-se à Tarefa Bônus 2 do desafio.**
**Status:** Aceita (entrega inicial)
**Data:** 2026-05-02
**Constitution:** v1

## Resumo

Tornar o sistema **performático em cenários com centenas de milhares de votos** (citação literal do enunciado), com instrumentação que torne a performance **observável** e **testável**.

A entrega inicial desta spec foca em ganhos de baixo custo e alto impacto + um teste de carga reproduzível. Otimizações mais agressivas (cache, materialização) ficam diferidas para o dia em que houver dados de produção mostrando que precisam.

## Requisitos funcionais

- **RF-4.1.** A apuração (`GET /api/v1/pautas/{id}/resultado`) executa em **uma única query** ao banco.
- **RF-4.2.** Existe um teste de carga reproduzível pela linha de comando (`mvn -Dperf.enabled=true -Dtest=CargaSistemaPerformanceTest test`) que reporta throughput, latência (p50/p95/p99) e tempo da apuração.
- **RF-4.3.** Endpoints de **observabilidade** (Spring Actuator) expõem `/actuator/health`, `/actuator/info`, `/actuator/metrics`.

## Não-funcionais (objetivos baseline)

Medidos em ambiente local (JDK 21, H2 in-memory, sem rede):

| Métrica | Alvo |
|---|---|
| Throughput de voto | ≥ 2.000 req/s sustentado |
| Latência p95 do voto | ≤ 50 ms |
| Latência p99 do voto | ≤ 100 ms |
| Apuração com 10 000 votos | ≤ 200 ms |
| Erro HTTP em carga | ≤ 1 % (margem para retries esporádicos do transporte) |

Resultados da rodada de baseline em [`plan.md` §4](./plan.md#4-resultados-de-baseline).

## Decisões de design (resumo)

1. **Java 21 virtual threads habilitados** (`spring.threads.virtual.enabled=true`).
2. **Apuração em 1 query** (substitui 2× `count` por `GROUP BY` agregado em uma chamada).
3. **HikariCP tunado** (pool=20 com VTs; timeouts e lifetime conservadores).
4. **Tomcat com compressão de resposta** (gzip em JSON ≥ 1KB).
5. **Hibernate batch_size=50** + ordering de inserts/updates (relevante quando o cliente fizer batch).
6. **Spring Actuator** expondo health/info/metrics.

Detalhamento e trade-offs em [ADR-022](../../docs/adr/022-performance.md).

## Trabalho deferido (gatilhos explícitos)

| Otimização | Quando ativar |
|---|---|
| Cache (Caffeine/Redis) na apuração | p99 da apuração > 200 ms em produção, ou cota de DB chegando a 80 % |
| Materialização de view de contagem | apuração > 1 s OU 10 M+ votos por pauta |
| Particionamento da tabela `voto` por pauta | tabela > 100 M linhas |
| Async write com fila (Kafka/Outbox) | throughput de voto > 10 000 req/s sustentado |
| Migração para `prometheus`/`grafana` em prod | quando entrar em prod com SLO formal |

Esses **não** estão implementados — a engenharia é "deixe pronto para medir, otimize quando houver dado".

## Critérios de aceite

- [x] `mvn verify` continua verde após mudanças (68 testes).
- [x] Teste de carga roda sob demanda e reporta números coerentes.
- [x] Apuração usa 1 query (verificado em `agregarVotosPorEscolhaRetornaContagemEmUmaQuery`).
- [x] Virtual threads ativos por configuração.
- [x] Actuator expondo endpoints documentados.
- [x] `tarefas-bonus.md` reflete o status.
