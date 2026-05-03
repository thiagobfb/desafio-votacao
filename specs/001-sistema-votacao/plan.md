# Plan 001 — Sistema de Votação Cooperativista

**Refere-se a:** [spec.md](./spec.md)
**Status:** Draft
**Data:** 2026-04-30
**Constitution:** v1

> Este documento responde **COMO** vamos implementar o que está descrito na `spec.md`. Cada decisão técnica abaixo carrega justificativa e trade-off explícito. Se uma escolha conflitar com o `spec.md` ou `constitution.md`, prevalece o documento superior.

---

## 1. Stack tecnológica

| Camada | Escolha | Versão | Justificativa |
|---|---|---|---|
| Linguagem | Java | **21 LTS** | Versão LTS mais recente; suporte a `records`, pattern matching, virtual threads — úteis para clareza e Bônus 2. |
| Framework | Spring Boot | **3.3.x** | Exigência do desafio; 3.x é a linha ativa. |
| Build | **Maven** | 3.9+ | Universal, pom.xml é legível por avaliadores; Gradle traria ganho marginal. |
| Persistência | Spring Data JPA + Hibernate | 6.x | Reduz boilerplate; suficiente para o domínio simples. |
| Banco — default/dev | **H2 file mode** | 2.x | Atende RNF-1 (persistência durável; `jdbc:h2:file:`). Zero setup para o avaliador. |
| Banco — test | H2 in-memory | 2.x | Isolamento e velocidade. |
| Banco — opcional/prod-like | PostgreSQL via Docker | 16 | Profile `postgres` disponível, mas **não exigido** para rodar o desafio. |
| Migrations | **Flyway** | core | Versionamento explícito de schema; alinhado com Constitution §IV (persistência explícita). |
| Validação | Jakarta Validation | 3.x | `@NotBlank`, `@Size`, `@NotNull` — built-in com Spring. |
| Documentação API | **springdoc-openapi** | 2.x | Gera OpenAPI 3 + Swagger UI automático (RNF-5). |
| Testes | JUnit 5, Mockito, AssertJ, Spring Boot Test | — | Stack padrão. |
| Logs | SLF4J + Logback (logback-spring.xml) | — | Default Spring; configuração mínima de pattern estruturado. |
| **Lombok** | `@Getter`, `@Setter`, `@NoArgsConstructor` em entidades | 1.18.38 | Reduz boilerplate em entidades JPA. Versão override do parent (1.18.34) para suportar JDK 25 do ambiente local. `<optional>true</optional>` + exclude no boot jar. |

**Não usados (com justificativa):**
- **Liquibase** — Flyway é mais simples para schema linear.
- **Testcontainers** — Não obrigatório nesta spec; H2 cobre o domínio. Pode entrar na Spec 004 (performance) ou 002 (integração CPF) se necessário.
- **MapStruct** — Mapeamento manual em `Mapper` simples basta; tamanho do domínio não justifica.

---

## 2. Arquitetura

### 2.1 Estilo

**Camadas em N=3** (Controller → Service → Repository), com domínio anêmico-pragmático: regras vão no Service, entidades JPA carregam apenas estado e validações triviais.

**Não:** Hexagonal, Onion, DDD tático com Aggregates e Value Objects elaborados. Constitution §I.

### 2.2 Estrutura de pacotes

```
br.com.desafio.votacao
├── VotacaoApplication.java
├── pauta
│   ├── api
│   │   ├── PautaController.java
│   │   └── dto/                # records: CriarPautaRequest, PautaResponse, etc
│   ├── domain
│   │   ├── Pauta.java          # @Entity
│   │   └── EstadoPauta.java    # enum
│   ├── repository
│   │   └── PautaRepository.java
│   └── service
│       └── PautaService.java
├── sessao
│   ├── api/...
│   ├── domain/Sessao.java
│   ├── repository/SessaoRepository.java
│   └── service/SessaoService.java
├── voto
│   ├── api/...
│   ├── domain/{Voto.java, Escolha.java}
│   ├── repository/VotoRepository.java
│   └── service/VotoService.java
├── resultado
│   ├── api/ResultadoController.java
│   ├── dto/ResultadoResponse.java
│   └── service/ResultadoService.java
├── shared
│   ├── exception/      # NegocioException, NaoEncontradoException, GlobalExceptionHandler
│   ├── config/         # OpenApiConfig, ClockConfig
│   └── util/
└── (sem layer "model" global — domínio fica perto do uso)
```

**Por feature, não por camada técnica.** Pacotes agrupam o que muda junto. Trade-off: levemente mais arquivos por pasta, mas localidade de mudança é o ganho.

### 2.3 Concorrência (RNF-2)

Uniqueness de voto é resolvida pela **constraint de banco** `UNIQUE(pauta_id, associado_id)`. O service tenta inserir; em `DataIntegrityViolationException` traduzimos para `VotoDuplicadoException` → HTTP 409.

**Por quê não lock pessimista/otimista?** O banco já é a única fonte de verdade da unicidade — duplicar a checagem em código adiciona complexidade sem ganho. Um SELECT-then-INSERT teria condição de corrida.

### 2.4 Tempo (RN-2)

Toda lógica de "está aberta?" usa um `Clock` injetado. Em produção: `Clock.system(ZoneOffset.of("-03:00"))` — Horário de Brasília (UTC-3, offset fixo). Isso permite testar expiração sem `Thread.sleep`. Bean único em `ClockConfig`.

**Trade-off do offset fixo vs `America/Sao_Paulo`:** Brasil não observa DST desde 2019 e os dois se comportam igual hoje. Mantendo `-03:00` fixo, o sistema permanece previsível mesmo se DST voltar. Se a regra de negócio passar a depender de zona civil, trocar para `ZoneId.of("America/Sao_Paulo")`.

---

## 3. Modelo físico (esquema do banco)

### Tabela `pauta`
| Coluna | Tipo | Constraints |
|---|---|---|
| id | BIGINT | PK, IDENTITY |
| titulo | VARCHAR(200) | NOT NULL |
| descricao | VARCHAR(2000) | NULL |
| criada_em | TIMESTAMP | NOT NULL |

### Tabela `sessao`
| Coluna | Tipo | Constraints |
|---|---|---|
| id | BIGINT | PK, IDENTITY |
| pauta_id | BIGINT | NOT NULL, **UNIQUE**, FK → pauta(id) |
| aberta_em | TIMESTAMP | NOT NULL |
| fecha_em | TIMESTAMP | NOT NULL, CHECK (fecha_em > aberta_em) |

`UNIQUE(pauta_id)` materializa **RN-1** (uma sessão por pauta) no banco.

### Tabela `voto`
| Coluna | Tipo | Constraints |
|---|---|---|
| id | BIGINT | PK, IDENTITY |
| pauta_id | BIGINT | NOT NULL, FK → pauta(id) |
| associado_id | VARCHAR(64) | NOT NULL |
| escolha | VARCHAR(3) | NOT NULL, CHECK IN ('SIM','NAO') |
| registrado_em | TIMESTAMP | NOT NULL |

`UNIQUE(pauta_id, associado_id)` materializa **RN-3**.
Índice `idx_voto_pauta` em `pauta_id` para acelerar apuração (Spec 004 pode adicionar mais).

**Identificadores:** `BIGINT GENERATED ALWAYS AS IDENTITY` (SQL padrão, suportado em H2 e PostgreSQL 10+). Mapeado em JPA com `@GeneratedValue(strategy = IDENTITY)` em `Long id`. Razão: simplicidade e legibilidade — adequado ao escopo do desafio. Trade-off conhecido: IDs sequenciais são previsíveis externamente; aceitável aqui pois autenticação está fora de escopo.

**`associado_id`** permanece `VARCHAR(64)`: é identificador externo informado pelo cliente, não chave de entidade do nosso sistema.

---

## 4. Contratos REST

Versionamento por **prefixo de URI** (`/api/v1/...`) — decisão preliminar; estratégia completa fica na **Spec 003**.

### 4.1 Endpoints

| Método | Caminho | Descrição | Status sucesso |
|---|---|---|---|
| `POST` | `/api/v1/pautas` | RF-1 — Cadastrar pauta | 201 Created |
| `GET`  | `/api/v1/pautas` | RF-5 — Listar pautas (paginado) | 200 OK |
| `GET`  | `/api/v1/pautas/{id}` | Detalhe de pauta | 200 / 404 |
| `POST` | `/api/v1/pautas/{id}/sessoes` | RF-2 — Abrir sessão | 201 / 404 / 409 |
| `POST` | `/api/v1/pautas/{id}/votos` | RF-3 — Votar | 201 / 400 / 404 / 409 |
| `GET`  | `/api/v1/pautas/{id}/resultado` | RF-4 — Apurar | 200 / 404 |

### 4.2 Exemplos

**POST /api/v1/pautas**
```json
// request
{ "titulo": "Aprovação do balanço 2026", "descricao": "..." }

// 201 response
{
  "id": 1,
  "titulo": "Aprovação do balanço 2026",
  "descricao": "...",
  "criadaEm": "2026-04-30T15:00:00-03:00",
  "estado": "SEM_SESSAO"
}
```

**POST /api/v1/pautas/{id}/sessoes**
```json
// request (duracaoMinutos opcional, default = 1)
{ "duracaoMinutos": 5 }

// 201
{
  "sessaoId": 1,
  "pautaId": 1,
  "abertaEm": "2026-04-30T15:05:00-03:00",
  "fechaEm":  "2026-04-30T15:10:00-03:00"
}
```

**POST /api/v1/pautas/{id}/votos**
```json
// request
{ "associadoId": "A123", "voto": "SIM" }

// 201
{ "votoId": 42, "registradoEm": "2026-04-30T15:06:12-03:00" }
```

**GET /api/v1/pautas/{id}/resultado**
```json
{
  "pautaId": 1,
  "estado": "ENCERRADA",
  "totalSim": 12,
  "totalNao": 7,
  "totalVotos": 19,
  "resultado": "APROVADA"
}
```

### 4.3 Formato de erro padronizado

Inspirado no padrão usado em `guitar-gpt`. Forma simples — `status` + `message` + `errors[]` (detalhes de validação) + `timestamp`.

```json
// 404 / 409 simples
{
  "status": 409,
  "message": "Associado A1 já votou na pauta 1",
  "errors": [],
  "timestamp": "2026-05-01T15:06:12.123"
}

// 400 com Bean Validation
{
  "status": 400,
  "message": "Falha de validação",
  "errors": ["titulo: must not be blank"],
  "timestamp": "2026-05-01T15:06:12.123"
}

// 400 com enum inválido (InvalidFormatException ou MethodArgumentTypeMismatchException)
{
  "status": 400,
  "message": "Valor inválido para 'voto'. Aceitos: [SIM, NAO]",
  "errors": [],
  "timestamp": "2026-05-01T15:06:12.123"
}
```

**Decisão:** sem campo `code` separado. Cliente identifica pelo HTTP status; mensagem traz contexto humano. Trade-off conhecido: dificulta i18n no cliente — aceitável pelo escopo.

---

## 5. Mapeamento de exceções

| Exceção interna | HTTP | Mensagem |
|---|---|---|
| `RecursoNaoEncontradoException` | 404 | da própria exception |
| `SessaoJaExisteException` / `SessaoEncerradaException` / `SessaoNaoAbertaException` / `VotoDuplicadoException` | 409 | da própria exception |
| `MethodArgumentNotValidException` (Bean Validation) | 400 | `"Falha de validação"` + `errors[]` com `campo: motivo` |
| `HttpMessageNotReadableException` com `InvalidFormatException` em enum | 400 | `"Valor inválido para 'X'. Aceitos: [...]"` |
| `HttpMessageNotReadableException` (outros) | 400 | `"Corpo da requisição inválido"` |
| `MethodArgumentTypeMismatchException` (path/query inválidos, especial p/ enum) | 400 | `"Valor inválido para 'X'..."` |
| `IllegalArgumentException` | 400 | da própria exception |
| Qualquer outra `Exception` | 500 | `"Erro interno do servidor"` (sem stack trace; logada server-side) |

Centralizado em `GlobalExceptionHandler` (`@RestControllerAdvice`). Estilo alinhado ao projeto `guitar-gpt`:
- ResponseEntity inline (sem helper).
- `LocalDateTime.now()` no construtor de conveniência do `ErroResponse` (não injeta `Clock` — exceção à regra do Constitution §IV de injeção de Clock; trade-off: timestamp do erro usa zona default da JVM em vez do UTC-3 explícito).
- Hierarquia de exceções: `NegocioException` (abstract) como ponto único; subclasses específicas para tipagem em testes.

---

## 6. Logs

Padrão SLF4J com chave-valor. Pontos de log:

- `INFO` — pauta criada, sessão aberta, sessão expirada (lazy: detectada na consulta), voto aceito.
- `WARN` — voto rejeitado (duplicado / sessão fechada), tentativa de abrir 2ª sessão.
- `ERROR` — exceções não mapeadas.

Pattern Logback inclui `traceId` (vazio inicialmente; reservado para Spec 002+ se aparecer integração distribuída).

---

## 7. Estratégia de testes

| Tipo | Ferramenta | Cobertura alvo |
|---|---|---|
| Unitário | JUnit 5 + Mockito | Services (regras de negócio: RN-1..RN-5). |
| Slice web | `@WebMvcTest` | Controllers (status codes, validação, formato de resposta/erro). |
| Integração | `@SpringBootTest` + H2 in-memory | Fluxo ponta-a-ponta de cada RF; constraints de banco. |
| Tempo | `Clock` mockável | Expiração de sessão sem `sleep`. |

**Mínimo aceitável (Constitution §III):**
- Cada caso de aceite em §6 da spec tem pelo menos um teste correspondente.
- Cada regra RN tem teste que falharia se a regra fosse violada.

---

## 8. Profiles e configuração

| Profile | Uso | DB |
|---|---|---|
| `default` | Avaliador roda `mvn spring-boot:run` | H2 file (`./data/votacao`) |
| `test`    | Build / CI                              | H2 in-memory |
| `postgres` | Opcional, via docker-compose            | PostgreSQL 16 |

Configurações expostas em `application.yml`:
- `votacao.sessao.duracao-default-minutos: 1` (RF-2)
- `votacao.sessao.duracao-maxima-minutos: 1440` (sanity cap)

---

## 9. Layout final do repositório (após implementação)

```
desafio-votacao/
├── pom.xml
├── README.md                     # como rodar, decisões de alto nível
├── LICENSE
├── specs/                        # SDD
├── src/
│   ├── main/java/br/com/desafio/votacao/...
│   ├── main/resources/
│   │   ├── application.yml
│   │   ├── application-test.yml
│   │   ├── application-postgres.yml
│   │   ├── logback-spring.xml
│   │   └── db/migration/         # Flyway: V1__init.sql, ...
│   └── test/java/...
├── data/                         # H2 file (gitignored)
└── docker-compose.yml            # opcional: postgres
```

---

## 10. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Race condition em voto duplicado | Constraint UNIQUE no banco (§2.3). |
| Race condition em abrir 2ª sessão | UNIQUE em `sessao.pauta_id` (§3). |
| Relógio do servidor incorreto desviando contagem de tempo | `Clock.systemUTC()` injetado; documentado no README. |
| H2 file mode corrompido em crash | Para o desafio é aceitável. Profile `postgres` disponível para cenários reais. |
| Performance em apuração com 100k+ votos | Endereçada na Spec 004 (índices, contagem agregada, possível materialização). |

---

## 11. Decisões pendentes / a confirmar

- **Java 21 OK?** Se o avaliador rodar JDK 17, ajustar para 17 LTS — custo ~zero.
- **Maven OK?** Alternativa Gradle disponível.
- **Banco default H2 file OK?** Alternativa: Postgres via docker-compose como default (mais "real" mas mais setup).

Default vai ser Java 21 + Maven + H2 file até feedback contrário.
