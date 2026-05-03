# ADR-020: Versionamento de API por URI

- **Status:** Aceito (preliminar — Spec 003 detalhará)
- **Data:** 2026-04-30
- **Contexto:** Spec 001 + Tarefa Bônus 3

## Contexto

A API precisa evoluir sem quebrar clientes mobile já em campo. A estratégia preliminar deve ser visível e fácil de testar.

## Decisão

**Prefixo de URI:** todas as rotas começam com `/api/v1/`.

```
POST /api/v1/pautas
POST /api/v1/pautas/{id}/sessoes
POST /api/v1/pautas/{id}/votos
GET  /api/v1/pautas/{id}/resultado
```

Estratégia completa (quando criar `v2`, como suportar `v1` e `v2` em paralelo, deprecação) fica detalhada em **Spec 003 — Versionamento de API**.

## Consequências

**Prós:**
- Visível em logs, métricas, curl examples.
- Cacheável por path (CDN/proxy).
- Cliente sabe imediatamente qual versão está consumindo.
- Não exige negociação de header.

**Trade-offs:**
- Breaking change exige código duplicado ou roteamento condicional.
- URLs ficam mais longas.

## Alternativas consideradas

- **Header (`Accept-Version: v1`):** invisível em log/curl básico.
- **Media type (`application/vnd.votacao.v1+json`):** RESTful purista, opaco em prática.
- **Query param (`?v=1`):** anti-pattern (mistura recurso e contrato).
