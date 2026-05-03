# ADR-002: Spring Boot 3.3.5 como framework

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

O desafio exige **Spring Boot**. A linha 3.x é a ativa e exige Java 17+ (alinhada com [ADR-001](001-java-21-lts.md)). 3.3.x é a release minor mais recente estável no momento da decisão.

## Decisão

Spring Boot **3.3.5** via parent POM:

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.3.5</version>
</parent>
```

Starters utilizados:
- `spring-boot-starter-web` — REST controllers + Jackson + Tomcat embarcado.
- `spring-boot-starter-data-jpa` — Spring Data + Hibernate (ver [ADR-004](004-spring-data-jpa.md)).
- `spring-boot-starter-validation` — Jakarta Bean Validation (ver [ADR-008](008-bean-validation.md)).
- `spring-boot-starter-test` — JUnit 5 + Mockito + AssertJ + MockMvc (ver [ADR-010](010-stack-testes.md)).

## Consequências

**Prós:**
- Auto-config + parent reduz manutenção do POM (`spring-boot-dependencies` BOM gerencia versões transitivas).
- Ferramentas de teste (`MockMvc`, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`) já vêm no starter de teste.
- Suporte ativo de bugfix e segurança.

**Trade-offs:**
- "Mágica" do auto-configure exige ler logs com atenção em casos exóticos.
- Acoplado a Jakarta EE 10 (`jakarta.persistence`, `jakarta.validation`) — quem migra de `javax.*` paga o custo único.

## Alternativas consideradas

- **Spring Framework puro:** exige boot manual; ganho de transparência não compensa o custo no escopo do desafio.
- **Quarkus / Micronaut:** explicitamente fora do escopo (Spring Boot é exigido).
