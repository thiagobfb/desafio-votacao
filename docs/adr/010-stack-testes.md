# ADR-010: Stack de testes (JUnit 5 + Mockito + AssertJ + Spring Boot Test)

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

Constitution §III exige cobertura mínima em três níveis: unitário (regras de negócio), slice web (controllers), integração (banco e fluxo ponta-a-ponta). Stack de teste deve estar disponível sem dependências adicionais.

## Decisão

`spring-boot-starter-test` (transitivo: JUnit 5, Mockito, AssertJ, Hamcrest, MockMvc, JsonPath).

| Tipo | Anotação Spring | Ferramenta principal |
|---|---|---|
| Unitário (service) | — | JUnit 5 + Mockito + AssertJ |
| Slice web | `@WebMvcTest` | MockMvc |
| Persistência | `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` | H2 in-memory + Flyway |
| Integração ponta-a-ponta | `@SpringBootTest` + `@TestConfiguration` (`MutableClock`) | MockMvc + H2 |

Overrides para JDK 25 local (ver [ADR-001](001-java-21-lts.md)): `byte-buddy 1.17.5` e `mockito 5.14.2`.

## Consequências

**Prós:**
- Tudo em um starter — sem dependências extras.
- Slice tests são rápidos (sobem só o necessário).
- AssertJ é mais ergonômico que Hamcrest para a maioria das asserções.

**Trade-offs:**
- Mockito + JDK 25 exige overrides (transitória — JDK 21 vanilla não precisa).
- `@SpringBootTest` é o teste mais lento; uso pontual em `FluxoCompletoIntegracaoTest`.

## Alternativas consideradas

- **TestNG:** menos integrado com Spring Boot.
- **Spock (Groovy):** nova linguagem só para testes — overhead.
- **Hamcrest puro:** menos legível em asserções de coleções.
