# ADR-007: Flyway para migrations

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

Constitution §IV exige **persistência explícita**. Schema deve ser versionado, reproduzível e independente da geração automática do Hibernate.

## Decisão

**Flyway core** com migrations em `src/main/resources/db/migration/`:

- `V1__init.sql` — cria tabelas `pauta`, `sessao`, `voto` com `BIGINT GENERATED ALWAYS AS IDENTITY` PKs, UNIQUE/CHECK constraints.
- Hibernate configurado com `ddl-auto: validate` — apenas confere o schema, nunca cria/altera.

Para evoluções futuras: novo arquivo `VN__descricao.sql` (nunca editar uma migration aplicada).

## Consequências

**Prós:**
- Schema reproduzível em H2 e PostgreSQL com o mesmo SQL.
- `ddl-auto: validate` falha rápido se uma entidade JPA divergir do schema.
- Migrations ficam versionadas no Git, auditáveis.

**Trade-offs:**
- Cada mudança de schema vira nova migration — disciplina necessária.
- Migrations em `src/main/resources` viram parte do jar (não é problema; Flyway lê via classpath).

## Alternativas consideradas

- **Liquibase:** mais flexível (XML/YAML/JSON), mas verboso para schema linear.
- **`ddl-auto: update`:** proibido por Constitution §IV — gera SQL imprevisível, perde rastreabilidade.
- **SQL ad-hoc no startup:** sem versionamento, sem rollback, sem checksum.
