# Plan 004 — Performance

**Refere-se a:** [spec.md](./spec.md)
**Status:** Aceito
**Data:** 2026-05-02

## 1. Otimizações implementadas

### 1.1 Apuração em uma única query

`ResultadoService.apurar()` antes fazia **2 queries** (`count` por escolha SIM + count por escolha NAO). Agora chama `votoRepository.agregarVotosPorEscolha(pautaId)` que executa **1 query** com `GROUP BY`:

```sql
SELECT v.escolha, COUNT(v) FROM voto v WHERE v.pauta_id = ? GROUP BY v.escolha
```

Plano de execução: index range scan sobre `idx_voto_pauta(pauta_id)` + agregação em memória. Para 10k votos, mede ~70-95 ms em H2 in-memory; em PostgreSQL com índice, ordem de magnitude menor.

### 1.2 Virtual threads (Java 21)

`spring.threads.virtual.enabled: true` no `application.yml`. Tomcat passa a usar VTs para handlers; `@Async` também. Comportamento: cada request é um VT que pode parquear sem bloquear thread do SO.

Trade-off conhecido: VT pode "pinar" em monitor JNI ou `synchronized`. Hibernate 6.x tem caminhos `synchronized` mas a maioria dos hot-paths já usa `ReentrantLock`. Em prática, não vimos contention notable nas medições.

### 1.3 HikariCP tunado

```yaml
hikari:
  maximum-pool-size: 20      # com VTs, pool pequeno é OK — VTs enfileiram nele
  minimum-idle: 5
  connection-timeout: 5000
  validation-timeout: 3000
  max-lifetime: 1800000
```

Recomendação geral com VTs: pool = `numCores * 2..4`. 20 cobre máquinas de até 8 cores; ajustar conforme alvo.

### 1.4 Hibernate batch

```yaml
properties:
  hibernate:
    jdbc.batch_size: 50
    order_inserts: true
    order_updates: true
```

Não há batch insert no fluxo do desafio (votos chegam um por request), mas a config fica pronta caso surja um endpoint "registrar lote".

### 1.5 Tomcat + compressão

```yaml
server:
  tomcat:
    threads.max: 200
    accept-count: 100
    max-connections: 8192
  compression:
    enabled: true
    mime-types: application/json
    min-response-size: 1024
```

Compressão gzip reduz bytes na rede para responses grandes (lista paginada de pautas, etc).

### 1.6 Spring Boot Actuator

```yaml
management:
  endpoints.web.exposure.include: health,info,metrics
  endpoint.health.show-details: always
```

Expõe métricas Micrometer (request latency, JVM, pool Hikari, JDBC). Em prod, basta plugar `micrometer-registry-prometheus` para scraping.

## 2. Teste de carga

`CargaSistemaPerformanceTest` (`src/test/java/.../performance/`):

- `@SpringBootTest` com `WebEnvironment.RANDOM_PORT` — sobe Tomcat real.
- Cliente HTTP nativo (`java.net.http.HttpClient`) — não retransmite POSTs.
- `@Primary` `CpfValidator` permissivo via `@TestConfiguration`.
- Opt-in por `-Dperf.enabled=true` (não atrasa `mvn verify`).
- Parametrizável: `-Dperf.votantes=20000 -Dperf.concorrencia=64`.

**Default:** 10 000 votos com concorrência 32. Tempo total ~3 s.

```bash
mvn -Dperf.enabled=true -Dtest=CargaSistemaPerformanceTest test
```

## 3. Decisões deliberadamente NÃO tomadas

| Otimização | Por que não agora |
|---|---|
| Cache da apuração | Dado não justifica; complexidade de invalidação não é trivial. |
| Read-replica do banco | Volume não exige; deploy adicional. |
| Particionamento Postgres | Tamanho não exige; `idx_voto_pauta` resolve a apuração para >100k votos. |
| Virtual threads via `Thread.ofVirtual()` manual em código | Spring Boot já faz pela config; intervir manualmente quebraria o gerenciamento. |
| Async write com Kafka | Throughput alvo (~5k req/s) é suportado síncrono. Async vira complexidade gratuita. |

## 4. Resultados de baseline

Medidos localmente em **JDK 21 + H2 in-memory** (não Postgres prod), profile `test`:

| Cenário | Throughput | Latência p50 | p95 | p99 | Apuração |
|---|---|---|---|---|---|
| 5 000 votos / conc=32 | 2 279 req/s | 12,0 ms | 30,5 ms | 47,3 ms | 95,8 ms |
| 10 000 votos / conc=32 | 3 276 req/s | 7,8 ms | 21,8 ms | 35,3 ms | 81,8 ms |
| 10 000 votos / conc=32 (rerun) | 3 498 req/s | 7,1 ms | 21,8 ms | 31,2 ms | 65,7 ms |

Erros HTTP < 0,2 % nas 3 rodadas — todos `409 voto duplicado` por retries esporádicos do transporte (não correspondem a votos perdidos).

**Notas:**
- H2 in-memory é mais lento em alguns workloads que PostgreSQL (não tem `prepared statement cache` agressivo). Em Postgres real, números devem melhorar.
- Latência inclui round-trip HTTP localhost — em rede real, soma RTT.
- A apuração com 10 000 votos roda em < 100 ms — bem abaixo dos 200 ms do alvo.

## 5. Como reproduzir

```bash
# Build limpo
mvn clean

# Roda os testes regulares (perf é skipado)
mvn verify

# Roda só o teste de carga
mvn -Dperf.enabled=true -Dtest=CargaSistemaPerformanceTest test

# Customiza volume e concorrência
mvn -Dperf.enabled=true -Dperf.votantes=20000 -Dperf.concorrencia=64 \
    -Dtest=CargaSistemaPerformanceTest test
```

Saída na console mostra o relatório completo (`================ Performance Report ================`).

## 6. Observabilidade em runtime

Com a aplicação rodando:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/metrics/hikaricp.connections.usage
curl http://localhost:8080/actuator/metrics/jvm.threads.live
```

## 7. Riscos

| Risco | Mitigação |
|---|---|
| VT pinning em libs com `synchronized` | Monitorar `jvm.threads.peak`. Voltar a platform threads se pinning aparecer. |
| H2 file mode trava sob alta concorrência | Profile `postgres` documentado para cargas reais. |
| Pool Hikari pequeno demais em produção | `maximum-pool-size` é via property; ajustar ao volume real. |
| Teste de carga "verde" não significa prod ok | Baseline é em ambiente local; SLO real precisa de teste em ambiente realista. |
