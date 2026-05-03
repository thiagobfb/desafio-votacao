# ADR-006: PostgreSQL como profile opcional

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

H2 file mode é prático para avaliação ([ADR-005](005-h2-default.md)) mas não representa um cenário "prod-like". Ter um caminho fácil para Postgres mostra que o sistema é portável e prepara terreno para Spec 004 (performance).

## Decisão

Driver `org.postgresql:postgresql` incluído como `runtime` no `pom.xml`. Profile `postgres` em `application-postgres.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/votacao
    username: votacao
    password: votacao
```

Ativação: `SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run`.

## Consequências

**Prós:**
- Mostra portabilidade sem custo no fluxo principal.
- Mesma migration Flyway roda nos dois bancos (`BIGINT GENERATED ALWAYS AS IDENTITY` é SQL padrão).
- Testes manuais "prod-like" com 5 minutos de docker-compose.

**Trade-offs:**
- ~+700KB no jar (driver Postgres).
- Configuração de container/credenciais fica fora do repo (responsabilidade do usuário).

## Alternativas consideradas

- **Não incluir Postgres:** perde a porta de fuga.
- **Testcontainers:** elegante, mas não exigido pela Spec 001 — escopo Spec 004.
