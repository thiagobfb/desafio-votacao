# ADR-005: H2 file mode como banco padrão

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

O desafio impõe **persistência durável após restart** (RNF-1) **sem dependência externa obrigatória** — o avaliador deve rodar `mvn spring-boot:run` e ter o sistema funcionando.

## Decisão

**H2 file mode** como banco do profile `default`:

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/votacao;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1
```

- Arquivo em `./data/votacao.mv.db` (gitignored). Sobrevive a restart.
- `AUTO_SERVER=TRUE` permite múltiplas conexões (app + console em outro terminal).
- Console disponível em `/h2-console` para inspeção em dev.

## Consequências

**Prós:**
- Zero setup para o avaliador.
- Durabilidade real (RNF-1 atendido).
- Dialeto SQL próximo ao PostgreSQL — mesma migration funciona em ambos.

**Trade-offs:**
- H2 não é prod-ready em alto volume ou concorrência pesada.
- "Production-like" com Postgres exige profile alternativo (ver [ADR-006](006-postgresql-opcional.md)).

## Alternativas consideradas

- **H2 in-memory:** não atende RNF-1 (perde estado em restart). Ainda assim usado no profile `test` por velocidade.
- **PostgreSQL como default:** mais real, mas exige docker-compose/instalação prévia — barreira para avaliação rápida.
- **SQLite:** menos suporte JPA/Hibernate maduro.
