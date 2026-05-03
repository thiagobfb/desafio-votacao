# ADR-003: Maven como build tool

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

O projeto precisa de uma ferramenta de build que: (a) seja universal entre avaliadores Java; (b) integre nativamente com Spring Boot; (c) tenha sintaxe legível por quem vai inspecionar o código.

## Decisão

**Maven 3.9+** com `pom.xml` único usando `spring-boot-starter-parent`. Plugins:

- `spring-boot-maven-plugin` — repackage e `spring-boot:run`.
- `maven-compiler-plugin` — `annotationProcessorPaths` para Lombok (override de versão).

## Consequências

**Prós:**
- `pom.xml` é declarativo e reconhecido por **qualquer IDE Java** sem configuração extra.
- Convenções padronizadas — qualquer dev Java sabe rodar `mvn verify`.
- Parent BOM do Spring Boot gerencia versões transitivas.

**Trade-offs:**
- Mais verboso que `build.gradle.kts`.
- DSL XML é menos expressiva para builds complexos (não é o caso aqui).

## Alternativas consideradas

- **Gradle Kotlin DSL:** mais conciso, suporte a build cache. Trade-off: menor universalidade entre avaliadores Java; o wrapper exigiria download de Gradle no primeiro `./gradlew`.
- **Gradle Groovy DSL:** mesma família do Kotlin com sintaxe legacy.
