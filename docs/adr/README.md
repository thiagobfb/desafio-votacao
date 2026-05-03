# ADRs — Architectural Decision Records

Cada arquivo documenta uma decisão técnica seguindo um formato MADR enxuto: **contexto → decisão → consequências (prós e trade-offs) → alternativas consideradas**.

ADRs são imutáveis: se uma decisão for revertida, criamos um novo ADR que **superseda** o anterior, sem editar o original.

## Índice

### Stack tecnológico
- [001 — Java 21 LTS](001-java-21-lts.md)
- [002 — Spring Boot 3.3.5](002-spring-boot.md)
- [003 — Maven](003-maven.md)
- [004 — Spring Data JPA + Hibernate](004-spring-data-jpa.md)
- [005 — H2 file mode como banco padrão](005-h2-default.md)
- [006 — PostgreSQL como profile opcional](006-postgresql-opcional.md)
- [007 — Flyway para migrations](007-flyway.md)
- [008 — Jakarta Bean Validation](008-bean-validation.md)
- [009 — springdoc-openapi (Swagger UI)](009-springdoc-openapi.md)
- [010 — Stack de testes (JUnit 5 + Mockito + AssertJ)](010-stack-testes.md)
- [011 — Lombok apenas em entidades](011-lombok-em-entidades.md)
- [012 — SLF4J + Logback](012-logging.md)

### Arquitetura e padrões
- [013 — Camadas N=3 + organização por feature](013-camadas-por-feature.md)
- [014 — Domínio anêmico-pragmático](014-dominio-anemico.md)
- [015 — IDs `Long` IDENTITY (não UUID)](015-ids-long-identity.md)
- [016 — FKs como `Long` raw (sem `@ManyToOne`)](016-fks-long-raw.md)
- [017 — `Clock` UTC-3 fixo](017-clock-utc-3.md)
- [018 — Concorrência via UNIQUE constraint](018-concorrencia-unique.md)
- [019 — Tratamento de exceções via `@RestControllerAdvice`](019-tratamento-excecoes.md)
- [020 — Versionamento de API por URI](020-versionamento-uri.md)
- [021 — Spec Driven Development](021-sdd.md)
- [022 — Trade-offs de performance e observabilidade](022-performance.md)
- [023 — Política de deprecação de versão de API](023-deprecacao-versao-api.md)

## Status

| Status | Significado |
|---|---|
| **Aceito** | Decisão em vigor |
| Substituído | Substituído por ADR mais recente |
| Deprecado | Decisão revogada, sem substituto direto |

Todas as 23 decisões atuais estão **Aceitas**.
