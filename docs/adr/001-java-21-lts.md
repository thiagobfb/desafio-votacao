# ADR-001: Java 21 LTS como linguagem

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001 — Sistema de Votação

## Contexto

O desafio exige Java + Spring Boot. A escolha de versão da JVM influencia features disponíveis (records, pattern matching, virtual threads), tempo de suporte oficial e compatibilidade com Spring Boot 3.x.

O ambiente local de desenvolvimento usa **JDK 25**, mas o avaliador deve conseguir rodar com qualquer JDK 21+. A toolchain precisa compilar bytecode compatível com a versão mais antiga aceita e ainda assim rodar no JDK do desenvolvedor.

## Decisão

- Compilar para **Java 21 LTS** (`<java.version>21</java.version>` no `pom.xml`).
- Permitir build com JDK 25 local através de overrides de versões em libs sensíveis a internals do `javac`/bytecode:
  - `lombok 1.18.38` — o gerenciado pelo parent (1.18.34) falha com `TypeTag :: UNKNOWN` no JDK 25.
  - `byte-buddy 1.17.5` + `mockito 5.14.2` — suporte ao class-file format do JDK 25.

## Consequências

**Prós:**
- LTS com janela de suporte longa; avaliador roda com JDK 21+ vanilla sem ajustes.
- Acesso a `record`, pattern matching para `instanceof`, sealed types e virtual threads.
- Compatível com Spring Boot 3.3.x.

**Trade-offs:**
- Os overrides de versão são desnecessários em JDK 21 vanilla — saneamento futuro quando o ambiente local migrar.
- Virtual threads ainda não habilitados (decisão para Spec 004 — performance).

## Alternativas consideradas

- **Java 17 LTS:** mais conservador, mas perde records aprimorados, pattern matching de `switch` e virtual threads. Custo de migração futura > ganho hoje.
- **Java 25 (não-LTS):** fora de escopo para projeto avaliado por terceiros.
