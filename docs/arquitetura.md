# Arquitetura

> Visão geral da arquitetura do sistema. Decisões individuais com rationale completa estão em [`adr/`](adr/).

## Estilo arquitetural

**Camadas N=3** com domínio anêmico-pragmático ([ADR-013](adr/013-camadas-por-feature.md), [ADR-014](adr/014-dominio-anemico.md)):

```
┌─────────────────────────────────────────┐
│  Controller (REST + DTOs + validação)   │
├─────────────────────────────────────────┤
│  Service     (regras de negócio + Tx)   │
├─────────────────────────────────────────┤
│  Repository  (Spring Data JPA)          │
├─────────────────────────────────────────┤
│  H2 file / PostgreSQL                   │
└─────────────────────────────────────────┘
```

**Organização por feature**, não por camada técnica:

```
br.com.desafio.votacao
├── pauta/      { api, domain, repository, service }
├── sessao/     { idem }
├── voto/       { idem }
├── resultado/  { idem }
└── shared/     { config, exception }
```

## Componentes por feature

| Feature | Controller | Service principal | Entidade |
|---|---|---|---|
| Pauta | `PautaController` | `PautaService` (+ `EstadoPautaResolver`) | `Pauta` |
| Sessão | `SessaoController` | `SessaoService` | `Sessao` |
| Voto | `VotoController` | `VotoService` | `Voto` |
| Resultado | `ResultadoController` | `ResultadoService` | — (record `ResultadoApurado`) |

`EstadoPautaResolver` quebra ciclo `PautaService↔SessaoService`: depende diretamente do `SessaoRepository` para resolver `EstadoPauta` na borda.

## Componentes compartilhados (`shared/`)

- **`ClockConfig`** — bean `Clock` em UTC-3 ([ADR-017](adr/017-clock-utc-3.md)).
- **`OpenApiConfig`** — `@OpenAPIDefinition` (Swagger UI).
- **`GlobalExceptionHandler`** — mapeamento de exceções → HTTP ([ADR-019](adr/019-tratamento-excecoes.md)).
- Hierarquia `NegocioException` (abstract) + subclasses específicas.

## Schema do banco

3 tabelas, criadas por Flyway (`V1__init.sql`):

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│  pauta   │◄────────┤  sessao  │   ◄─────┤   voto   │
│──────────│ 1     1 │──────────│ 1     n │──────────│
│ id       │         │ id       │         │ id       │
│ titulo   │         │ pauta_id │ UNIQUE  │ pauta_id │ ┐
│ descricao│         │ aberta_em│         │ assoc_id │ │ UNIQUE
│ criada_em│         │ fecha_em │         │ escolha  │ ┘ (pauta_id, assoc_id)
└──────────┘         └──────────┘         │ reg_em   │
                                          └──────────┘
```

Constraints e índices ativos:
- `uk_sessao_pauta` em `sessao(pauta_id)` — RN-1 (1 sessão/pauta).
- `uk_voto_pauta_associado` em `voto(pauta_id, associado_id)` — RN-3 (1 voto/associado/pauta).
- `CHECK (fecha_em > aberta_em)` em `sessao`.
- `CHECK (escolha IN ('SIM','NAO'))` em `voto`.
- `idx_voto_pauta` em `voto(pauta_id)` — acelera apuração.

Concorrência ([ADR-018](adr/018-concorrencia-unique.md)) é resolvida pelas constraints + tradução de `DataIntegrityViolationException` no service.

## Fluxo de uma requisição (POST `/api/v1/pautas/{id}/votos`)

```
Cliente
  │ POST { associadoId: "A1", voto: "SIM" }
  ▼
[VotoController]
  │ Bean Validation no record (@NotBlank/@NotNull/@Size)
  ▼
[VotoService.registrar]
  │ pautaService.buscarObrigatorio(pautaId)        ── pode lançar RecursoNaoEncontrado (404)
  │ sessaoService.buscarPorPautaId(pautaId)        ── ausente → SessaoNaoAberta (409)
  │ sessao.estaAbertaEm(LocalDateTime.now(clock))  ── falso  → SessaoEncerrada (409)
  ▼
[VotoRepository.saveAndFlush]
  │ INSERT ... → DB
  │  ├─ sucesso → log.info("Voto registrado ...")
  │  └─ DataIntegrityViolationException
  │      → log.warn("Voto rejeitado: duplicado")
  │      → VotoDuplicadoException (409)
  ▼
[GlobalExceptionHandler]   (caso erro)
  │ exception → ResponseEntity<ErroResponse>(status, message, errors, timestamp)
  ▼
Cliente recebe 201 (sucesso) ou ErroResponse JSON com 400/404/409/500.
```

## Tratamento de tempo

`Clock` é injetado em todos os services que dependem de "agora" (`PautaService`, `SessaoService`, `VotoService`, `ResultadoService`). Isso permite:

- **Em produção:** `Clock.system(ZoneOffset.of("-03:00"))` — Horário de Brasília fixo ([ADR-017](adr/017-clock-utc-3.md)).
- **Em testes de integração:** `MutableClock` injetado via `@TestConfiguration` — avança o relógio em 6 minutos sem `Thread.sleep` para validar expiração de sessão.

## Mapeamento de exceções

Detalhado no [ADR-019](adr/019-tratamento-excecoes.md). Resumo:

| HTTP | Quando |
|---|---|
| 201 | Recurso criado |
| 200 | Consulta com sucesso |
| 400 | Validação Bean Validation, enum inválido, body ilegível |
| 404 | Pauta inexistente |
| 409 | Conflito de regra de negócio (sessão duplicada, voto duplicado, sessão fechada/inexistente) |
| 500 | Bug — logado com stack, resposta sem detalhes |

## Profiles

| Profile | Banco | Uso |
|---|---|---|
| `default` | H2 file (`./data/votacao.mv.db`) | Avaliação manual / dev |
| `test` | H2 in-memory | CI / `mvn verify` |
| `postgres` | PostgreSQL | Demo "prod-like" — opcional ([ADR-006](adr/006-postgresql-opcional.md)) |

## Observabilidade

Logs em SLF4J + Logback ([ADR-012](adr/012-logging.md)) nos pontos definidos em `plan.md` §6:

- **INFO:** pauta criada · sessão aberta · voto registrado · sessão expirada detectada na apuração.
- **WARN:** voto rejeitado (duplicado / sessão fechada / sem sessão) · tentativa de abrir 2ª sessão.
- **ERROR:** exceções não mapeadas (no `GlobalExceptionHandler`).

Métricas Actuator/Prometheus, distributed tracing e logs JSON estruturados ficam para Spec 004 (performance).
