# Tarefas bônus — status de implementação

O `README.md` lista três tarefas bônus do desafio. Aqui está o **estado real** de cada uma neste código.

| Bônus | Tema | Status |
|---|---|---|
| 1 | Validação externa de CPF | ✅ **Implementado** (Spec 002) |
| 2 | Performance | ✅ **Implementado** (Spec 004) — otimizações + teste de carga reproduzível |
| 3 | Versionamento de API | ✅ **Implementado** (Spec 003) — estratégia + política de deprecação documentadas |

---

## Bônus 1 — Validação externa de CPF

**Status:** ✅ Implementado em [`specs/002-validacao-cpf/`](../specs/002-validacao-cpf/).

### O que existe hoje no código

- **Interface** `CpfValidator` em `cpf/domain/` com método `validar(String cpf)` retornando o enum `StatusValidacaoCpf` (`INVALIDO` / `UNABLE_TO_VOTE` / `ABLE_TO_VOTE`).
- **Implementação fake** `FakeCpfValidator` (`cpf/service/`) — `@Component` em duas etapas:
  - **Formato (determinístico)** — algoritmo de dígitos verificadores DV1 e DV2 da Receita Federal:
    ```
    Soma1 = d1*10+d2*9+d3*8+d4*7+d5*6+d6*5+d7*4+d8*3+d9*2
    DV1   = (Soma1 % 11 < 2) ? 0 : 11 - (Soma1 % 11)

    Soma2 = d1*11+d2*10+d3*9+d4*8+d5*7+d6*6+d7*5+d8*4+d9*3+DV1*2
    DV2   = (Soma2 % 11 < 2) ? 0 : 11 - (Soma2 % 11)
    ```
    CPF é válido quando `d10 == DV1` e `d11 == DV2`. Aceita formatado (`111.444.777-35`) e nu (`11144477735`).
  - **Habilitação (aleatória)** — CPF estruturalmente válido sorteia entre `ABLE_TO_VOTE` e `UNABLE_TO_VOTE` (enunciado: *"um mesmo CPF pode funcionar em um teste e não funcionar no outro"*).
- **Hook em `VotoService.registrar()`** — chamada ao validador é a primeira checagem (fail-fast); falha não consulta pauta nem sessão.
- **Mapeamento HTTP no `GlobalExceptionHandler`:**
  - `CpfInvalidoException` → **404** com mensagem `"CPF X inválido ou não encontrado"`.
  - `AssociadoNaoPodeVotarException` → **404** com mensagem `"Associado com CPF X não está habilitado a votar no momento"`.
- **Schema do banco** evoluiu via migration `V2__renomeia_associado_id_para_cpf.sql` — coluna `associado_id` foi renomeada para `cpf` e a constraint UNIQUE virou `uk_voto_pauta_cpf`. Funciona em H2 e PostgreSQL.
- **Testes** novos: `FakeCpfValidatorTest` cobre CPFs válidos (puro e formatado), DV1 errado, DV2 errado, comprimento incorreto, não-numérico, null, vazio + distribuição entre `ABLE_TO_VOTE` e `UNABLE_TO_VOTE`. Mais 2 cenários em `VotoServiceTest` e 2 em `VotoControllerTest`.
- **Determinismo em testes:** `FluxoCompletoIntegracaoTest` injeta um `@Primary CpfValidator` permissivo (`ABLE_TO_VOTE` sempre) via `@TestConfiguration` — mesmo padrão usado para `MutableClock`.

### Tentativa via curl

```bash
# 1. Cria pauta + sessão
curl -X POST http://localhost:8080/api/v1/pautas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Aprovação 2026"}'

curl -X POST http://localhost:8080/api/v1/pautas/1/sessoes \
  -H 'Content-Type: application/json' \
  -d '{"duracaoMinutos":5}'

# 2. Tenta votar com CPF válido — pode dar 201 (ABLE) ou 404 (UNABLE)
curl -X POST http://localhost:8080/api/v1/pautas/1/votos \
  -H 'Content-Type: application/json' \
  -d '{"cpf":"11144477735","voto":"SIM"}'

# 3. Tenta votar com CPF inválido — sempre 404 (INVALIDO; algoritmo de DV reprova)
curl -X POST http://localhost:8080/api/v1/pautas/1/votos \
  -H 'Content-Type: application/json' \
  -d '{"cpf":"12345678901","voto":"SIM"}'
```

A parte de **formato** é determinística (CPF com DVs errados sempre dá `INVALIDO`); a parte de **habilitação** é aleatória — o avaliador pode precisar tentar mais de uma vez para ver `ABLE_TO_VOTE` em um CPF válido.

### Decisão sobre o status HTTP

O enunciado original mostra ambos os erros (CPF inválido e UNABLE_TO_VOTE) sendo tratados com 404 (`"// CPF Nao Ok para votar - retornar 404 no client tb"`). Seguimos literalmente o spec; mensagens distintas no body permitem o cliente diferenciar os dois casos sem ler o status code.

### O que ficou de fora (escopo Spec 002.1 ou 005)

- **Cliente HTTP real** para um serviço externo (apenas a interface está pronta — basta uma nova `@Component` substituindo a fake).
- **Cache** de respostas do validador.
- **Validação de formato CPF** (algoritmo dos dígitos verificadores) — desnecessária dado que o validador externo é a fonte de verdade.

---

## Bônus 2 — Performance

**Status:** ✅ Implementado em [`specs/004-performance/`](../specs/004-performance/) — ver [ADR-022](adr/022-performance.md).

### Otimizações implementadas

- **Java 21 virtual threads** habilitados (`spring.threads.virtual.enabled: true`). Tomcat passa a usar VTs.
- **Apuração em 1 query** — substituí 2× `count` por uma query agregada (`agregarVotosPorEscolha` com `GROUP BY`). Plano de execução é index range scan em `idx_voto_pauta`.
- **HikariCP tunado** — pool 20 (com VTs), `connection-timeout=5s`, `max-lifetime=30min`.
- **Hibernate batch** — `jdbc.batch_size: 50` + `order_inserts/updates`.
- **Tomcat** — compressão gzip de JSON ≥ 1 KB, `threads.max=200`, `accept-count=100`, `max-connections=8192`.
- **Spring Boot Actuator** expondo `/actuator/health`, `/actuator/info`, `/actuator/metrics` (Micrometer pronto para `prometheus`).

### Teste de carga reproduzível

Arquivo: `src/test/java/.../performance/CargaSistemaPerformanceTest.java`. Opt-in para não atrasar `mvn verify`:

```bash
mvn -Dperf.enabled=true -Dtest=CargaSistemaPerformanceTest test

# customizando volume e concorrência
mvn -Dperf.enabled=true -Dperf.votantes=20000 -Dperf.concorrencia=64 \
    -Dtest=CargaSistemaPerformanceTest test
```

Reporta throughput, latência p50/p95/p99, tempo da apuração e taxa de erro. Asserções:
- Erro HTTP ≤ 1 % (margem para retries esporádicos do transporte).
- `totalVotos` apurados = sucessos do envio (nenhum voto perdido).

### Baseline medido (JDK 21 + H2 in-memory)

| Cenário | Throughput | p50 | p95 | p99 | Apuração |
|---|---|---|---|---|---|
| 5 000 votos / conc=32 | 2 279 req/s | 12,0 ms | 30,5 ms | 47,3 ms | 95,8 ms |
| 10 000 votos / conc=32 | 3 276 req/s | 7,8 ms | 21,8 ms | 35,3 ms | 81,8 ms |
| 10 000 votos / conc=32 (rerun) | 3 498 req/s | 7,1 ms | 21,8 ms | 31,2 ms | 65,7 ms |

Erros < 0,2 % nas três rodadas (todos `409 voto duplicado` por retransmissão esporádica do transporte HTTP, não votos perdidos).

### O que ficou de fora (gatilhos explícitos para reativar)

| Otimização | Quando ativar |
|---|---|
| Cache (Caffeine/Redis) | p99 da apuração > 200 ms em prod |
| Materialização de view | apuração > 1 s OU 10 M+ votos por pauta |
| Particionamento da tabela `voto` | tabela > 100 M linhas |
| Async write (Kafka/Outbox) | throughput > 10 000 req/s sustentado |
| `micrometer-registry-prometheus` | quando entrar em prod com SLO formal |

---

## Bônus 3 — Versionamento de API

**Status:** ✅ Implementado em [`specs/003-versionamento-api/`](../specs/003-versionamento-api/) — ver [ADR-020](adr/020-versionamento-uri.md) (mecanismo) + [ADR-023](adr/023-deprecacao-versao-api.md) (política).

### O que existe hoje

- **Prefixo de URI `/api/v1/`** em todos os endpoints. Visível em logs, curl, CDN; cacheável por path.
- **Política de deprecação documentada** (`Deprecation` + `Sunset` headers; 6 meses de coexistência; 30 dias de `410 Gone`).
- **Critérios objetivos** para classificar mudanças em "aditiva" (sem bumpar) vs "breaking" (`vN+1`) — ver [Spec 003 §"O que é breaking"](../specs/003-versionamento-api/spec.md#o-que-é-breaking).

### Resposta à pergunta "Como você versionaria?" (do desafio)

> Uso **prefixo de URI** (`/api/vN/`): é visível em logs/curl/CDN; o cliente sabe imediatamente qual contrato consome; cacheável por path. Mudanças **aditivas** (campos opcionais, endpoints novos, status novos para casos não previstos) ficam na **mesma versão** — clientes antigos ignoram. Mudanças **breaking** (renomear/remover campo, mudar tipo, mudar semântica) forçam **`vN+1`** em paralelo. `vN` permanece servindo por **≥ 6 meses** com headers `Deprecation: true` e `Sunset: <data>` (padrões IETF). Após o sunset, 30 dias de `410 Gone` antes de remover o código. Roteamento por controllers em pacotes versionados (`feature/api/vN/`); services compartilhados quando o domínio não muda. OpenAPI gera docs separados por versão via `GroupedOpenApi`.
>
> Alternativas (header `Accept-Version`, media type `application/vnd.X.v1+json`) são RESTfully mais puras mas pioram observabilidade — versão fica invisível em log/curl básico.

### Trabalho Fase 2 (gatilho: surgir mudança breaking real)

- Criar `feature/api/v2/` ao lado de `v1/`.
- `ApiVersionMdcFilter` populando MDC `apiVersion` para correlação em log.
- `Deprecation` + `Sunset` headers em respostas `v1` via interceptor configurável.
- `springdoc-openapi` com `GroupedOpenApi` por versão.

---

## Resumo executável

Quando o avaliador rodar o sistema, encontrará:

- ✅ Endpoints todos sob `/api/v1/...`. Estratégia completa (deprecação, breaking criteria, roteamento) documentada em Spec 003 + ADRs 020/023.
- ✅ Validação de CPF ativa — `cpf` validado pelo fake antes de qualquer regra de domínio; resposta HTTP é 404 nos dois caminhos de falha.
- ✅ Performance: virtual threads + apuração em 1 query + Hikari tunado + Actuator + teste de carga reproduzível com baseline medido (3 000+ req/s, p99 < 50 ms para 10 000 votos).
