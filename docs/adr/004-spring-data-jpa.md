# ADR-004: Spring Data JPA + Hibernate como camada de persistência

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

O sistema persiste pautas, sessões e votos. O domínio é simples (3 tabelas), mas precisa garantir constraints (unicidade), transações e portabilidade entre H2 e PostgreSQL (ver [ADR-005](005-h2-default.md), [ADR-006](006-postgresql-opcional.md)).

## Decisão

**Spring Data JPA** sobre **Hibernate 6.x** (versão gerenciada pelo Spring Boot 3.3.x).

- Repositórios estendem `JpaRepository<T, Long>`.
- Queries derivadas de nome (`findByPautaId`, `countByPautaIdAndEscolha`).
- `@Transactional` em services para delimitar boundaries.
- `ddl-auto: validate` — Hibernate apenas confere o schema vs entidades; criação fica com Flyway (ver [ADR-007](007-flyway.md)).

## Consequências

**Prós:**
- Reduz boilerplate massivamente (repositórios = interfaces, sem implementação).
- Portabilidade SQL entre H2 e PostgreSQL via dialeto.
- Integração transacional declarativa.

**Trade-offs:**
- "Mágica" de proxies e lazy-loading; mitigada por [ADR-016](016-fks-long-raw.md) (FKs como `Long`, sem `@ManyToOne`).
- Entidades JPA ficam acopladas à API `jakarta.persistence` — aceitável no anêmico-pragmático (ver [ADR-014](014-dominio-anemico.md)).

## Alternativas consideradas

- **JDBC puro / JdbcTemplate:** verboso para CRUD; vantagem só aparece em queries críticas (não é o caso aqui).
- **MyBatis:** SQL explícito mas exige XML/anotações + arquivos de mapeamento.
- **R2DBC:** reativo, complica o modelo síncrono do controller; não exigido.
