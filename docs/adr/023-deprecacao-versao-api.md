# ADR-023: Política de deprecação de versão de API

- **Status:** Aceito
- **Data:** 2026-05-02
- **Contexto:** Spec 003 — Versionamento + Tarefa Bônus 3
- **Estende:** [ADR-020](020-versionamento-uri.md)

## Contexto

[ADR-020](020-versionamento-uri.md) fixa o **mecanismo** de versionamento (prefixo de URI, `/api/vN/`). Esta ADR define a **política operacional**: como e quando deprecar uma versão; como sinalizar isso ao cliente; e quais convenções de cabeçalho HTTP usar.

## Decisão

### Classificação de mudanças

| Tipo | Quem decide | Bumpa versão? |
|---|---|---|
| Adicionar campo opcional | API designer | ❌ não |
| Adicionar endpoint | API designer | ❌ não |
| Adicionar status code para caso de erro **novo** | API designer | ❌ não |
| Renomear / remover / mudar tipo de campo | requer revisão | ✅ sim → `vN+1` |
| Mudar semântica de endpoint existente | requer revisão | ✅ sim → `vN+1` |
| Mudar URL de endpoint | requer revisão | ✅ sim (ou alias temporário) |

### Linha do tempo de deprecação

```
T-3m       T0           T+6m         T+7m
 │          │             │            │
 │          │             │            │
 ▼          ▼             ▼            ▼
Anúncio   Release       Sunset      Remoção
de v2     de v2         de v1       definitiva
        + Deprecation   v1 → 410     de v1
        em respostas    Gone
        v1
```

- **T−3m** — Anúncio prévio (changelog, email, banner em Swagger).
- **T0** — `vN+1` em produção. Respostas `vN` ganham headers `Deprecation: true` + `Sunset: <data>`.
- **T+6m** — Sunset. `vN` passa a responder `410 Gone` por 30 dias.
- **T+7m** — `vN` removida do código.

### Cabeçalhos HTTP

Toda response de versão deprecada inclui:

```
Deprecation: true
Sunset: Wed, 02 Nov 2026 00:00:00 GMT
Link: <https://api.exemplo.com/api/v2/pautas>; rel="successor-version"
```

- `Deprecation` segue [draft-ietf-httpapi-deprecation-header](https://datatracker.ietf.org/doc/draft-ietf-httpapi-deprecation-header/).
- `Sunset` segue [RFC 8594](https://datatracker.ietf.org/doc/html/rfc8594).
- `Link` aponta para o sucessor (se houver mapeamento direto).

### Implementação prevista (quando `v2` existir)

`ApiVersionDeprecationInterceptor` (`HandlerInterceptor`) injeta os headers em respostas que casam `/api/v1/`. Lista de versões deprecadas e respectivas `Sunset` em `application.yml`:

```yaml
votacao:
  api:
    deprecated-versions:
      - prefix: /api/v1/
        sunset: 2026-11-02
        successor: /api/v2/
```

### Observabilidade por versão

`ApiVersionMdcFilter` popula MDC `apiVersion` (extraído do path). Pattern do logback inclui `%X{apiVersion}` para correlação.

Métricas Micrometer já vêm tagueadas pelo path; podemos adicionar `apiVersion` como tag custom em `WebMvcTagsContributor`.

## Consequências

**Prós:**
- Cliente vê o aviso no header da própria resposta — não depende de ler changelog.
- Período definido (6 meses + 30 dias de `410`) dá previsibilidade.
- Padrões IETF (Deprecation/Sunset) — clientes sofisticados sabem ler.

**Trade-offs:**
- Manter `v1` por 6 meses paralelamente a `v2` exige disciplina (não dar refactor que quebre `v1`).
- Headers add ~80 bytes por response — desprezível.

## Alternativas consideradas

- **Não definir Sunset** (deixar `v1` para sempre) — quebra a evolução; o esforço de manter cresce com o tempo.
- **Sunset agressivo (1 mês)** — pressão excessiva sobre clientes mobile que demoram a atualizar.
- **Custom header `X-Deprecated-Until`** em vez de `Sunset` IETF — reinventa a roda; clientes não conhecem.
