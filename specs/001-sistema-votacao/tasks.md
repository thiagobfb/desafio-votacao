# Tasks 001 — Sistema de Votação

**Refere-se a:** [spec.md](./spec.md) · [plan.md](./plan.md)
**Status:** Pendente

> Cada task deixa o repositório **compilando e com testes passando** ao final. Tasks são ordenadas por dependência. Marcar `[x]` quando concluída e abrir commit no formato `feat: ...` / `test: ...` / `chore: ...`.

---

## Fase 0 — Bootstrap

- [x] **T-001** Criar `pom.xml` com dependências do plan §1 (Spring Boot 3.3.5, web, validation, data-jpa, h2, postgresql, flyway, springdoc 2.6.0, test). Java 21. ✓ `mvn package` ok.
- [x] **T-002** Criar classe `VotacaoApplication` (main) e `application.yml` com profile default (H2 file em `./data/votacao`).
- [x] **T-003** Criar `application-test.yml` (H2 in-memory) e `application-postgres.yml`. Adicionar `.gitignore`.
- [x] **T-004** Configurar `logback-spring.xml` com pattern timestamp/level/thread/logger.
- [x] **T-005** Criar `ClockConfig` expondo bean `Clock systemUTC()`. Teste `ClockConfigTest` confirmando bean e zona UTC. ✓

## Fase 1 — Persistência

- [x] **T-010** Migration `V1__init.sql` com tabelas `pauta`, `sessao`, `voto` (UNIQUE + CHECK constraints). _Adiantada: necessária para o context Spring subir._
- [x] **T-011** Entidades JPA: `Pauta`, `Sessao`, `Voto` + enums `Escolha`, `EstadoPauta`. IDs `Long` com `@GeneratedValue(strategy = IDENTITY)`. Sem `@ManyToOne` — FKs como `Long pautaId` direto. Sem setters/equals.
- [x] **T-012** Repositories: `PautaRepository`, `SessaoRepository.findByPautaId`, `VotoRepository.{countByPautaIdAndEscolha, countByPautaId, existsByPautaIdAndAssociadoId}`.
- [x] **T-013** `PersistenciaIntegracaoTest` (`@DataJpaTest` + Flyway) — 7 testes cobrindo persistência, constraint `UNIQUE(sessao.pauta_id)`, constraint `UNIQUE(voto.pauta_id, associado_id)`, contagens, e helper `Sessao.estaAbertaEm`.

## Fase 2 — Domínio (Services)

- [x] **T-020** `PautaService.criar(titulo, descricao)` + `buscarObrigatorio(id)`. 4 testes unitários (criação com clock, descrição nula, lookup, lookup faltando).
- [x] **T-021** `SessaoService.abrir(pautaId, duracaoMinutos)` + `buscarPorPautaId`. 7 testes (custom, default, pauta inexistente, 2ª sessão, duração ≤0, negativa, acima do máximo). Validação de duração antes de DB lookup (fail-fast).
- [x] **T-022** `VotoService.registrar(...)`. 5 testes (sucesso, sem sessão, encerrada, fechamento exato, voto duplicado via `DataIntegrityViolationException` traduzido). Usa `saveAndFlush` para forçar surge da constraint dentro do try.
- [x] **T-023** `ResultadoService.apurar(pautaId)` + `ResultadoApurado` (record) + `ResultadoVotacao` (enum). 6 testes cobrindo `SEM_SESSAO`, `EM_ANDAMENTO`, `APROVADA`, `REJEITADA`, `EMPATE`, e empate-zero.

## Fase 3 — API

- [x] **T-030** 8 DTOs (records): `CriarPautaRequest`, `PautaResponse`, `AbrirSessaoRequest`, `SessaoResponse`, `RegistrarVotoRequest`, `VotoResponse`, `ResultadoResponse`, `ErroResponse`. Bean Validation nos requests.
- [x] **T-031** `PautaController` (POST/GET/GET-by-id) + `EstadoPautaResolver` (helper que evita ciclo `PautaService↔SessaoService`). 6 testes `@WebMvcTest`.
- [x] **T-032** `SessaoController` (POST `/pautas/{pautaId}/sessoes`, body opcional) + 5 testes (custom, default, 404, 409, 400).
- [x] **T-033** `VotoController` (POST `/pautas/{pautaId}/votos`) + 7 testes (sucesso, voto inválido, associado vazio, 404, 409 não-aberta/encerrada/duplicado).
- [x] **T-034** `ResultadoController` (GET `/pautas/{pautaId}/resultado`) + 6 testes cobrindo os 5 estados + 404.
- [x] **T-035** `GlobalExceptionHandler` mapeando NegocioException + Bean Validation + IllegalArgumentException + HttpMessageNotReadableException + Exception genérica. `Clock` injetado para timestamp.

## Fase 4 — Documentação e qualidade

- [x] **T-040** `OpenApiConfig` com `@OpenAPIDefinition` (title, version, description) + `@Tag` em cada controller. Smoke test em `FluxoCompletoIntegracaoTest.openApiDocsDisponiveis` valida `/v3/api-docs`.
- [x] **T-041** `FluxoCompletoIntegracaoTest` (`@SpringBootTest` + `MutableClock` injetado via `@TestConfiguration`). 5 cenários: fluxo completo aprovada com avanço de relógio, 2ª sessão (409), voto sem sessão (409), apuração 404, OpenAPI smoke.
- [x] **T-042** README reescrito: stack, pré-requisitos, comandos de execução, link para Swagger/H2 console, exemplo curl, decisões de design, ponteiros para `specs/`. Desafio original preservado ao final.

## Fase 5 — Polimento

- [x] **T-050** Logs estruturados em todos os pontos do plan §6. Adicionados: WARN "voto rejeitado: pauta sem sessão" em `VotoService` e INFO "sessão expirada detectada na apuração" em `ResultadoService`.
- [x] **T-051** Revisar mensagens de erro (consistência, sem leak de stack). Mensagens das `NegocioException` são consistentes (`<entidade> <id> <verbo>...`) e o handler genérico devolve apenas `"Erro interno do servidor"` (stack só vai para `log.error`).
- [x] **T-052** `mvn verify` limpo: BUILD SUCCESS, 60/60 testes verdes.
- [ ] **T-053** Commit final com tag `v0.1.0-sistema-votacao` (opcional).

---

## Dependências entre tasks

```
T-001 ─▶ T-002 ─▶ T-003 ─▶ T-004 ─▶ T-005
                                     │
                  ┌──────────────────┘
                  ▼
                T-010 ─▶ T-011 ─▶ T-012 ─▶ T-013
                                            │
                  ┌─────────────────────────┘
                  ▼
                T-020 ─┬─▶ T-021 ─▶ T-022 ─▶ T-023
                       │              │
                       ▼              ▼
                T-030 ─▶ T-031 ─▶ T-032 ─▶ T-033 ─▶ T-034 ─▶ T-035
                                                              │
                                                              ▼
                                                            T-040 ─▶ T-041 ─▶ T-042
                                                                                │
                                                                                ▼
                                                                              T-050..053
```

Sub-tasks da mesma fase podem ser paralelas se você quiser, mas a sequência acima é segura.

---

## Critérios de aceite global

- Todos RFs da spec verificados por teste.
- Todos RNs garantidos (RN-1 e RN-3 por banco, RN-2 e RN-5 por service+teste).
- README permite ao avaliador rodar e testar em < 5 min.
- `mvn verify` verde.
