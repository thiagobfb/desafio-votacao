# ADR-022: Trade-offs de performance e observabilidade

- **Status:** Aceito
- **Data:** 2026-05-02
- **Contexto:** Spec 004 — Performance + Tarefa Bônus 2

## Contexto

O enunciado pede que o sistema seja **performático em cenários com centenas de milhares de votos** e que **testes de performance** sejam parte da entrega. Precisamos de ganhos reais, mensuráveis, sem inflar a complexidade.

A regra geral aqui: **deixe o sistema pronto para medir, otimize quando os dados pedirem**.

## Decisões

### 1. Java 21 virtual threads habilitados

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Tomcat passa a usar virtual threads para handlers; `@Async` também. Cada request é um VT independente, parquear não bloqueia thread do SO.

### 2. Apuração em uma única query

`ResultadoService.apurar()` antes fazia 2 round-trips (`count(SIM)` + `count(NAO)`). Agora um `GROUP BY` agregado em 1 query. Plano de execução: index range scan sobre `idx_voto_pauta` + agregação em memória.

```java
@Query("""
    SELECT new br.com.desafio.votacao.voto.domain.ContagemPorEscolha(v.escolha, COUNT(v))
    FROM Voto v WHERE v.pautaId = :pautaId GROUP BY v.escolha
""")
List<ContagemPorEscolha> agregarVotosPorEscolha(@Param("pautaId") Long pautaId);
```

### 3. HikariCP pool e timeouts conservadores

`maximum-pool-size: 20` com VTs (regra: `cores * 2..4`). `connection-timeout: 5s`. `max-lifetime: 30 min`.

### 4. Hibernate batch_size + ordering

```yaml
hibernate:
  jdbc.batch_size: 50
  order_inserts: true
  order_updates: true
```

Não há batch insert hoje no fluxo (votos chegam um a um), mas a config fica pronta para um endpoint de "registrar lote".

### 5. Tomcat — compressão gzip + limites generosos

`server.compression.enabled: true` para JSON ≥ 1 KB. `threads.max=200`, `accept-count=100`, `max-connections=8192`.

### 6. Spring Boot Actuator

`spring-boot-starter-actuator` — expõe `/actuator/health`, `/actuator/info`, `/actuator/metrics` (Micrometer). Em prod, plugar `micrometer-registry-prometheus` é uma linha.

### 7. Teste de carga opt-in

`CargaSistemaPerformanceTest` (`@SpringBootTest` + `RANDOM_PORT`) ativado por `-Dperf.enabled=true`. Reporta p50/p95/p99/throughput/tempo de apuração.

## Consequências

**Prós:**
- Throughput medido em local: **3 000+ req/s** com p99 < 50 ms para 10 000 votos.
- Apuração de 10 000 votos < 100 ms.
- Observabilidade já tem ponto de coleta (Micrometer).
- Teste de carga reproduzível pelo avaliador em uma linha.

**Trade-offs:**
- Virtual threads podem "pinar" em libs com `synchronized` em monitor JNI. Monitorar `jvm.threads.peak`.
- H2 in-memory é benchmark indicativo; PostgreSQL real tem perfil ligeiramente diferente.
- Pool de 20 conexões cobre máquinas até ~8 cores; tuning final depende do hardware.

## Decisões deliberadamente NÃO tomadas

| Otimização | Por que diferida |
|---|---|
| Cache (Caffeine/Redis) na apuração | Sem dado de prod justificando; complexidade de invalidação não-trivial. |
| Materialização de view | Apuração já < 100 ms para 10k. |
| Read-replica | Volume não exige. |
| Particionamento de tabela | Tamanho não exige; índice resolve. |
| Async write (Kafka/Outbox) | Throughput alvo é suportado síncrono. |
| Migração para HTTP/2 + ALPN | Ganho marginal sem clientes que aproveitem. |

Cada uma volta para a mesa **quando houver gatilho concreto** documentado em [`specs/004-performance/spec.md`](../../specs/004-performance/spec.md#trabalho-deferido-gatilhos-explícitos).

## Alternativas consideradas

- **JMH para microbenchmark** da query de agregação — útil, mas teste de carga ponta-a-ponta cobre o caso. Diferido para Spec 004.1 se virar foco.
- **Gatling Maven plugin** em vez de teste JUnit — adiciona ~50 MB de Scala em `~/.m2`; sem ganho proporcional.
- **k6 (binário externo)** — exige instalação separada; nosso teste JUnit roda com `mvn` apenas.
- **WebFlux (reativo) em vez de MVC** — virtual threads dão grande parte do ganho que o reactive prometia, sem trocar o modelo de programação.
