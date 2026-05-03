# Tarefas bônus — status de implementação

O `README.md` lista três tarefas bônus do desafio. Aqui está o **estado real** de cada uma neste código.

| Bônus | Tema | Status |
|---|---|---|
| 1 | Validação externa de CPF | ❌ **Não implementado** |
| 2 | Performance | ⚠️ **Endereçado parcialmente em design**, sem implementação medida |
| 3 | Versionamento de API | ✅ **Parcialmente implementado** (URI prefix em vigor) |

A escolha de não implementar 1 e 2 é deliberada: o foco da Spec 001 era construir um **sistema de votação correto, simples e bem testado**. Tarefas bônus ganham specs próprias e ficam registradas como **placeholders** (`Spec 002`, `Spec 003`, `Spec 004`) para referência.

---

## Bônus 1 — Validação externa de CPF

**Status:** Não implementado.

### O que existiria se implementado (proposta para Spec 002)

- Interface `CpfValidator` com método `StatusValidacao validar(String cpf)` retornando `ABLE_TO_VOTE` / `UNABLE_TO_VOTE` / inválido.
- Implementação `FakeCpfValidator` (conforme desafio: retorno aleatório).
- Mapeamento HTTP:
  - CPF inválido por formato → **404** (NotFound) no client e no nosso endpoint.
  - CPF válido + `UNABLE_TO_VOTE` → **422** (Unprocessable Entity) ou **409** com mensagem clara.
- Hook em `VotoService.registrar()` antes da inserção.
- Testes unitários com fake **fixo** (não-aleatório) — determinismo.
- Toggle por configuração (`votacao.cpf.validador=fake|http|disabled`) para o avaliador testar com/sem.

### Por que não foi feito

A Spec 001 já entrega **todos os requisitos funcionais obrigatórios** (RF-1 a RF-5) e **regras de negócio** (RN-1 a RN-5) do enunciado original. SDD privilegia entregar specs completas em vez de fragmentos de várias.

### Onde encaixaria

Nova feature `cpf/` (paralela a `pauta/`, `voto/`...) com o mesmo padrão `api / domain / service / repository`. Injeção em `VotoService`.

---

## Bônus 2 — Performance

**Status:** Endereçado **em design**, **não medido**.

### O que existe hoje no código

- Índice `idx_voto_pauta` em `voto(pauta_id)` (Flyway `V1__init.sql`) para acelerar `countByPautaIdAndEscolha`.
- Apuração com `count(*)` agregado **no banco** — não traz a lista de votos para a JVM.
- `@Transactional(readOnly = true)` em queries de leitura.
- FKs como `Long` raw evitam N+1 inadvertido ([ADR-016](adr/016-fks-long-raw.md)).
- Constraints UNIQUE no banco evitam round-trips de checagem ([ADR-018](adr/018-concorrencia-unique.md)).

### O que **não** existe

- Nenhum **benchmark** ou **load test** medido (sem JMH, k6, Gatling, JMeter).
- Sem **caching** (Caffeine/Redis) na contagem.
- Sem **virtual threads** habilitados (Java 21+ permitiria).
- Sem materialização de view ou contagem incremental.
- Sem otimizações específicas para o cenário "centenas de milhares de votos" mencionado no enunciado.

### Trabalho proposto (Spec 004 — placeholder)

- Cenário de teste com 100k+ votos.
- Baseline de latência **p95 / p99** para os 4 endpoints.
- Profiling com async-profiler / JFR.
- Decisão fundamentada de cache vs materialização vs nada.
- Migração Postgres + connection pool tuning (HikariCP).

---

## Bônus 3 — Versionamento de API

**Status:** ✅ **Parcialmente implementado.**

### O que existe hoje

- **Prefixo de URI `/api/v1/`** em todos os endpoints. Decisão registrada em [ADR-020](adr/020-versionamento-uri.md).
- `@RequestMapping("/api/v1/...")` em todos os controllers.
- README e Swagger refletem `v1`.

### O que falta para uma "estratégia completa"

- Critérios formais para criar `v2` (que mudanças contam como "breaking"?).
- Política de deprecação (quanto tempo `v1` co-existe com `v2`? cabeçalho `Deprecation` / `Sunset`?).
- Mecanismo de roteamento entre versões (controllers separados? roteamento condicional?).
- Documentação OpenAPI por versão.

### Resposta à pergunta "Como você versionaria?" (do desafio)

> Eu uso **prefixo de URI** (`/api/v1/`) como caminho preferencial: é visível em logs, curl e CDN; cliente sabe imediatamente qual contrato consome; cacheável por path. Quebras viram `/api/v2/` com período de coexistência (mínimo 6 meses, com header `Deprecation: true; sunset=...` em `v1`). Mudanças aditivas (campos opcionais novos) ficam na mesma versão. Alternativas (header `Accept-Version`, media type `application/vnd.X.v1+json`) são RESTfully puras mas pioram observabilidade no dia a dia. Detalhamento completo fica em **Spec 003 — Versionamento de API**.

---

## Resumo executável

Quando o avaliador rodar o sistema, ele encontrará:

- ✅ Endpoints todos sob `/api/v1/...`
- ❌ Validação de CPF: ausente — qualquer string vai como `associadoId`.
- ⚠️ Performance: índices presentes, mas sem evidência empírica.
