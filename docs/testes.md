# Estratégia de testes

> Cobertura atual: **80 testes** rodados por padrão + **1 teste de carga opt-in** (Spec 004), todos verdes.

## Tipos de teste presentes

| Tipo | Anotação Spring | Quantos | O que cobre |
|---|---|---|---|
| **Unitário** (service) | — | 24 | Regras de negócio (RN-1..RN-5, validação de CPF) — Mockito + AssertJ |
| **Slice de persistência** | `@DataJpaTest` + Flyway | 8 | Entidades, constraints UNIQUE, queries derivadas, query agregada (`agregarVotosPorEscolha`) |
| **Slice web** | `@WebMvcTest` + MockMvc | 26 | Controllers — status codes, validação, formato de erro |
| **Integração ponta-a-ponta** | `@SpringBootTest` + `MutableClock` | 5 | Fluxo completo, OpenAPI, expiração temporal |
| **Smoke / configuração** | `@SpringBootTest` | 2 | Bean `Clock` em UTC-3, contexto sobe |
| **Validador de CPF (Spec 002)** | — | 15 | `FakeCpfValidator`: algoritmo determinístico DV1+DV2 (válidos, DV1 errado, DV2 errado, comprimento, não-numérico, formatado, null/vazio) + sorteio ABLE/UNABLE |
| **Carga (opt-in, Spec 004)** | `@SpringBootTest` + `@EnabledIfSystemProperty` | 1 | 10 000 votos em paralelo; reporta throughput/p50/p95/p99/apuração |

Stack: **JUnit 5 + Mockito + AssertJ + Spring Boot Test** ([ADR-010](adr/010-stack-testes.md)).

## Mapa por arquivo

```
src/test/java/br/com/desafio/votacao/
├── VotacaoApplicationTests              (1)  context loads
├── shared/
│   ├── MutableClock.java                     utilitário (Clock mutável p/ testes de tempo)
│   └── config/ClockConfigTest           (1)  bean Clock em UTC-3
├── persistencia/
│   └── PersistenciaIntegracaoTest       (8)  Pauta/Sessao/Voto + UNIQUE + query agregada
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
├── integracao/
│   └── FluxoCompletoIntegracaoTest      (5)  fluxo, 2ª sessão, sem sessão, 404, OpenAPI
└── performance/
    └── CargaSistemaPerformanceTest      (1)  opt-in: 10k votos em paralelo (Spec 004)
                                       ----
                          Padrão (mvn verify): 80
                            Opt-in (perf):     +1
```

## O que cada nível garante

### Unitário — exemplo: `VotoServiceTest.traduzVotoDuplicadoQuandoConstraintEstoura`

Stub `votoRepository.saveAndFlush(...)` lançando `DataIntegrityViolationException`. Verifica que o service traduz para `VotoDuplicadoException` — RN-3 + concorrência ([ADR-018](adr/018-concorrencia-unique.md)).

### Slice de persistência — `PersistenciaIntegracaoTest`

Sobe Flyway + H2 in-memory. Insere dois votos com mesmo `(pauta_id, associado_id)`. Espera `DataIntegrityViolationException` da constraint `uk_voto_pauta_associado` — garante que o schema reflete RN-3 sem depender do código de aplicação.

### Slice web — `VotoControllerTest`

`@WebMvcTest(VotoController.class)`. Mocka `VotoService`. Valida:
- 201 em sucesso;
- 400 com voto inválido (`{"voto":"MAYBE"}`) → mensagem `"Valor inválido para 'voto'. Aceitos: [SIM, NAO]"`;
- 400 com `associadoId` vazio;
- 404 quando service lança `RecursoNaoEncontradoException`;
- 409 nos três caminhos: sessão não aberta, sessão encerrada, voto duplicado.

### Integração — `FluxoCompletoIntegracaoTest.fluxoCompleto_...`

`@SpringBootTest` com `@TestConfiguration` provendo `@Primary MutableClock`. Cenário:

1. POST cria pauta → recupera `id`.
2. POST abre sessão de 5 min.
3. 3 votos (A1=SIM, A2=SIM, A3=NAO).
4. Tentativa de re-voto de A1 → 409.
5. GET resultado → `EM_ANDAMENTO`, `totalSim=2`, `totalNao=1`.
6. **`clock.avancar(Duration.ofMinutes(6))`** — sem `Thread.sleep`.
7. GET resultado → `ENCERRADA`, `APROVADA`.
8. Tentativa de voto pós-fechamento → 409.

## Teste de performance (opt-in, Spec 004)

`CargaSistemaPerformanceTest` é uma carga sintética que dispara N votos em paralelo contra a aplicação real (Tomcat embarcado + H2 in-memory) usando o cliente HTTP nativo do JDK 21.

```bash
# Opt-in para não atrasar mvn verify rotineiro
mvn -Dperf.enabled=true -Dtest=CargaSistemaPerformanceTest test

# Customizando volume e concorrência
mvn -Dperf.enabled=true -Dperf.votantes=20000 -Dperf.concorrencia=64 \
    -Dtest=CargaSistemaPerformanceTest test
```

Reporta na console:
- Throughput (req/s)
- Latência p50, p95, p99 (ms)
- Tempo da apuração final
- `totalVotos` apurados
- Erros HTTP (com status + exemplo de body)

**Asserções:** erro HTTP ≤ 1 % e `totalVotos` apurados = sucessos do envio (nenhum voto perdido).

Baseline medido: throughput **3 000+ req/s** com **p99 < 50 ms** para 10 000 votos em H2 in-memory; apuração com 10 000 votos em **65–95 ms**. Detalhes e tabela de baseline em [`tarefas-bonus.md` §Bônus 2](tarefas-bonus.md#baseline-medido-jdk-21--h2-in-memory) e em [`specs/004-performance/plan.md`](../specs/004-performance/plan.md#4-resultados-de-baseline).

## Outros testes ausentes (fora de escopo da Spec 001)

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
