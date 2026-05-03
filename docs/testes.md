# Estratégia de testes

> Cobertura atual: **79 testes**, todos verdes (`mvn verify`).

## Tipos de teste presentes

| Tipo | Anotação Spring | Quantos | O que cobre |
|---|---|---|---|
| **Unitário** (service) | — | 24 | Regras de negócio (RN-1..RN-5, validação de CPF) — Mockito + AssertJ |
| **Slice de persistência** | `@DataJpaTest` + Flyway | 7 | Entidades, constraints UNIQUE, queries derivadas |
| **Slice web** | `@WebMvcTest` + MockMvc | 26 | Controllers — status codes, validação, formato de erro |
| **Integração ponta-a-ponta** | `@SpringBootTest` + `MutableClock` | 5 | Fluxo completo, OpenAPI, expiração temporal |
| **Smoke / configuração** | `@SpringBootTest` | 2 | Bean `Clock` em UTC-3, contexto sobe |
| **Validador de CPF (Spec 002)** | — | 15 | `FakeCpfValidator`: algoritmo determinístico DV1+DV2 (válidos, DV1 errado, DV2 errado, comprimento, não-numérico, formatado, null/vazio) + sorteio ABLE/UNABLE |

Stack: **JUnit 5 + Mockito + AssertJ + Spring Boot Test** ([ADR-010](adr/010-stack-testes.md)).

## Mapa por arquivo

```
src/test/java/br/com/desafio/votacao/
├── VotacaoApplicationTests              (1)  context loads
├── shared/
│   ├── MutableClock.java                     utilitário (Clock mutável p/ testes de tempo)
│   └── config/ClockConfigTest           (1)  bean Clock em UTC-3
├── persistencia/
│   └── PersistenciaIntegracaoTest       (7)  Pauta/Sessao/Voto + UNIQUE
├── cpf/
│   └── service/FakeCpfValidatorTest    (15)  algoritmo DV1+DV2 + sorteio ABLE/UNABLE
├── pauta/
│   ├── service/PautaServiceTest         (4)
│   └── api/PautaControllerTest          (6)
├── sessao/
│   ├── service/SessaoServiceTest        (7)
│   └── api/SessaoControllerTest         (5)
├── voto/
│   ├── service/VotoServiceTest          (7)  + INVALIDO + UNABLE_TO_VOTE
│   └── api/VotoControllerTest           (9)  + 2 cenários 404 de CPF
├── resultado/
│   ├── service/ResultadoServiceTest     (6)
│   └── api/ResultadoControllerTest      (6)
└── integracao/
    └── FluxoCompletoIntegracaoTest      (5)  fluxo, 2ª sessão, sem sessão, 404, OpenAPI
                                       ----
                                  Total: 79
```

## O que cada nível garante

### Unitário — exemplo: `VotoServiceTest.traduzVotoDuplicadoQuandoConstraintEstoura`

Stub `votoRepository.saveAndFlush(...)` lançando `DataIntegrityViolationException`. Verifica que o service traduz para `VotoDuplicadoException` — RN-3 + concorrência ([ADR-018](adr/018-concorrencia-unique.md)).

### Slice de persistência — `PersistenciaIntegracaoTest`

Sobe Flyway + H2 in-memory. Insere dois votos com mesmo `(pauta_id, cpf)`. Espera `DataIntegrityViolationException` da constraint `uk_voto_pauta_cpf` — garante que o schema reflete RN-3 sem depender do código de aplicação.

### Slice web — `VotoControllerTest`

`@WebMvcTest(VotoController.class)`. Mocka `VotoService`. Valida:
- 201 em sucesso;
- 400 com voto inválido (`{"voto":"MAYBE"}`) → mensagem `"Valor inválido para 'voto'. Aceitos: [SIM, NAO]"`;
- 400 com `cpf` vazio;
- 404 quando service lança `RecursoNaoEncontradoException` / `CpfInvalidoException` / `AssociadoNaoPodeVotarException`;
- 409 nos três caminhos: sessão não aberta, sessão encerrada, voto duplicado.

### Integração — `FluxoCompletoIntegracaoTest.fluxoCompleto_...`

`@SpringBootTest` com `@TestConfiguration` provendo `@Primary MutableClock` e `@Primary CpfValidator` permissivo. Cenário:

1. POST cria pauta → recupera `id`.
2. POST abre sessão de 5 min.
3. 3 votos com CPFs distintos (`SIM`, `SIM`, `NAO`).
4. Tentativa de re-voto → 409.
5. GET resultado → `EM_ANDAMENTO`, `totalSim=2`, `totalNao=1`.
6. **`clock.avancar(Duration.ofMinutes(6))`** — sem `Thread.sleep`.
7. GET resultado → `ENCERRADA`, `APROVADA`.
8. Tentativa de voto pós-fechamento → 409.

## Testes de performance — não implementados (ainda)

A Tarefa Bônus 2 do desafio menciona "centenas de milhares de votos" e sugere testes de performance. **Esses testes não existem hoje neste repositório.** Detalhes em [`tarefas-bonus.md`](tarefas-bonus.md#bônus-2-performance):

- Sem **JMH** (microbenchmarks).
- Sem **k6 / Gatling / JMeter** (load tests).
- Sem **profiling** ou medição de latência p95/p99.

Trabalho proposto fica registrado como **Spec 004 — Performance e suporte a alto volume** (placeholder).

## Outros testes ausentes (fora de escopo da Spec 002)

- **Mutation testing** (PIT) — não habilitado.
- **Contract testing** (Spring Cloud Contract / Pact) — fora de escopo.
- **End-to-end com cliente mobile real** — explicitamente fora do escopo do desafio.

## Como rodar

```bash
mvn verify                            # tudo
mvn -Dtest=VotoServiceTest test       # uma classe
mvn -Dtest='*Controller*' test        # todos os slices web
```

Relatório Surefire em `target/surefire-reports/`.
