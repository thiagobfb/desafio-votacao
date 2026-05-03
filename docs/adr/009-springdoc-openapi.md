# ADR-009: springdoc-openapi para documentação interativa

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

RNF-5 exige documentação interativa da API (Swagger UI). Spring 3.x descontinuou Springfox; springdoc-openapi é o substituto de fato.

## Decisão

`springdoc-openapi-starter-webmvc-ui` **2.6.0** (versão estável compatível com Spring Boot 3.3.x).

- `OpenApiConfig` com `@OpenAPIDefinition` (title, version, description).
- `@Tag` em cada controller para agrupar endpoints na UI.
- Smoke test em `FluxoCompletoIntegracaoTest.openApiDocsDisponiveis` valida `/v3/api-docs`.

URLs:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Consequências

**Prós:**
- Spec OpenAPI 3 gerada automaticamente do código + DTOs.
- Avaliador testa endpoints sem ferramenta externa.
- Smoke test garante que a doc continua disponível em mudanças futuras.

**Trade-offs:**
- Anotações de doc poluem controllers se forem detalhadas (mantemos só `@Tag`).

## Alternativas consideradas

- **Springfox:** sem suporte para Spring 3.x.
- **Escrever `openapi.yaml` manual:** drift entre doc e código.
