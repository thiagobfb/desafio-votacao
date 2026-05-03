# ADR-011: Lombok apenas em entidades JPA

- **Status:** Aceito
- **Data:** 2026-05-01
- **Contexto:** Spec 001

## Contexto

Entidades JPA precisam de getters, setters e construtor sem-args (Hibernate). Escrever isso à mão polui as classes. DTOs também são pequenos, mas Java 16+ tem `record` que já resolve isso de forma idiomática.

## Decisão

- Lombok **só em `@Entity`**: `@Getter @Setter @NoArgsConstructor(access = PROTECTED)`.
- DTOs são **`record`** (sem Lombok).
- Lombok como `<optional>true</optional>` no pom — não vai para o jar final.
- Versão override `1.18.38` (parent 1.18.34 falha no JDK 25 — ver [ADR-001](001-java-21-lts.md)).

## Consequências

**Prós:**
- Entidades curtas (~15 linhas em vez de ~80).
- DTOs são imutáveis por construção (`record`).
- Sem Lombok no runtime do jar.

**Trade-offs:**
- IDEs precisam do Lombok plugin (ver [`docs/ide-setup.md`](../ide-setup.md)).
- Setters em entidade abrem espaço para mutação fora do construtor — mitigado por construtores domínio-específicos (`new Pauta(titulo, descricao, criadaEm)`).

## Alternativas consideradas

- **Lombok em todos os tipos:** redundante para `record`.
- **Zero Lombok:** entidades viram boilerplate puro.
- **Java records como entidades JPA:** Hibernate ainda não tem suporte completo (precisa de `@Embeddable` ou hack).
