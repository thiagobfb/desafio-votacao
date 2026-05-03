# Tasks 004 — Performance

**Refere-se a:** [spec.md](./spec.md) · [plan.md](./plan.md)

## Fase 1 — Otimizações de baixo custo

- [x] **T-401** Habilitar virtual threads (`spring.threads.virtual.enabled: true`).
- [x] **T-402** Tunar HikariCP (`maximum-pool-size`, timeouts, lifetime).
- [x] **T-403** Hibernate batch + ordering de inserts/updates.
- [x] **T-404** Tomcat — compressão gzip de JSON.
- [x] **T-405** Apuração em 1 query: `agregarVotosPorEscolha` (`GROUP BY`) substitui 2× `count`.
- [x] **T-406** Teste de persistência para a nova query (`agregarVotosPorEscolhaRetornaContagemEmUmaQuery`).

## Fase 2 — Observabilidade

- [x] **T-410** Adicionar `spring-boot-starter-actuator`.
- [x] **T-411** Configurar endpoints expostos: `health,info,metrics`.
- [x] **T-412** `info.app.*` populado para `/actuator/info`.

## Fase 3 — Teste de carga

- [x] **T-420** `CargaSistemaPerformanceTest` opt-in (`-Dperf.enabled=true`).
- [x] **T-421** Cliente HTTP nativo (`java.net.http.HttpClient`) — sem retransmissão de POST.
- [x] **T-422** `@Primary CpfValidator` permissivo via `@TestConfiguration`.
- [x] **T-423** Parâmetros via `-Dperf.votantes` e `-Dperf.concorrencia`.
- [x] **T-424** Saída com p50/p95/p99 + throughput + tempo da apuração.
- [x] **T-425** Asserções: erro HTTP ≤ 1 %; nenhum voto perdido.

## Fase 4 — Documentação

- [x] **T-430** ADR-022 — Performance choices.
- [x] **T-431** Atualizar `docs/tarefas-bonus.md` (Bônus 2 ⚠️ → ✅).
- [x] **T-432** Atualizar `docs/testes.md` com a contagem nova e mencionar o perf test.
- [x] **T-433** Atualizar `docs/arquitetura.md` com observabilidade + virtual threads.

## Fase 5 — Diferida

- [ ] **T-440** Cache (Caffeine) na apuração — gatilho: p99 > 200 ms em produção.
- [ ] **T-441** `micrometer-registry-prometheus` para scraping em prod.
- [ ] **T-442** Profile JMH para microbenchmark da query de agregação.
- [ ] **T-443** Particionamento da tabela `voto` no PostgreSQL — gatilho: > 100 M linhas.

## Critérios de aceite globais

- [x] `mvn verify` continua verde (68 testes).
- [x] `mvn -Dperf.enabled=true -Dtest=CargaSistemaPerformanceTest test` reporta números coerentes:
  - Throughput ≥ 2 000 req/s
  - p95 ≤ 50 ms
  - p99 ≤ 100 ms
  - Apuração com 10 000 votos < 200 ms
- [x] Apuração executa **uma** query (verificada por teste de persistência).
