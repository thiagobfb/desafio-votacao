# Spec 003 — Estratégia de versionamento de API

**Refere-se à Tarefa Bônus 3 do desafio.**
**Status:** Aceita
**Data:** 2026-05-02
**Constitution:** v1

## Resumo

A API do sistema é versionada por **prefixo de URI** (`/api/v1/...`). Esta spec consolida a estratégia: como classificar mudanças (compatíveis vs. breaking), quando criar uma nova versão, política de deprecação e como suportar múltiplas versões em paralelo.

## Pergunta original do desafio

> "Como você versionaria a API da sua aplicação? Que estratégia usar?"

## Resposta (resumo executivo)

1. **Prefixo de URI** (`/api/vN/...`). Visível em logs, métricas, curl, CDN; cacheável por path; ergonômico para o cliente. Detalhes em [ADR-020](../../docs/adr/020-versionamento-uri.md).
2. **Mudanças aditivas** (campos novos opcionais, novos endpoints, novos status para casos antes não previstos) **ficam na mesma versão** — não bumpamos `v1` por isso. Cliente antigo ignora o novo campo.
3. **Mudanças breaking** (renomear campo, remover campo, alterar semântica, quebrar contrato de erro) **vão para `vN+1`**. `vN` permanece servindo até o fim do período de deprecação.
4. **Período de deprecação** mínimo de **6 meses** após anúncio de `vN+1`. `vN` continua respondendo, com headers `Deprecation: true` e `Sunset: <data>`.
5. **Roteamento**: cada versão tem seus próprios `@RestController`s no pacote da feature (`pauta/api/v1/`, `pauta/api/v2/`). Services e repositórios são compartilhados quando possível; quando o domínio quebra, services dedicados por versão evitam contaminação.

Detalhamento: [`plan.md`](./plan.md), [ADR-020](../../docs/adr/020-versionamento-uri.md), [ADR-023](../../docs/adr/023-deprecacao-versao-api.md).

## O que é "breaking"

Critérios objetivos para forçar `vN+1`:

| Mudança | Breaking? |
|---|---|
| Adicionar campo opcional ao response | ❌ não |
| Adicionar campo opcional ao request | ❌ não |
| Adicionar novo endpoint | ❌ não |
| Adicionar novo status code para um caso de erro **novo** | ❌ não |
| Renomear campo no request/response | ✅ sim |
| Remover campo | ✅ sim |
| Tornar campo opcional → obrigatório | ✅ sim |
| Mudar tipo (string → integer, etc) | ✅ sim |
| Mudar semântica (ex.: 200 → 204; mensagem de erro com nova lógica) | ✅ sim |
| Mudar URL de um endpoint | ✅ sim (ou criar alias) |

## Caso real neste projeto

Em **Spec 002** o campo `associadoId` virou `cpf` no request e response do `/votos`. Tecnicamente é uma quebra de contrato. Foi tratada como mudança dentro de `v1` **só por ser desafio acadêmico**; em produção, exigiria `v2` ou um período de coexistência aceitando ambos os campos.

## Não-funcionais

- **RNF-3.1.** Cliente que consome `v1` deve continuar funcional ao menos 6 meses após anúncio de `v2`.
- **RNF-3.2.** Toda response de versão deprecada deve incluir headers padrão IETF (`Deprecation`, `Sunset`).
- **RNF-3.3.** A versão da rota deve aparecer em logs estruturados (MDC `apiVersion`) — facilita troubleshooting por versão.
- **RNF-3.4.** A documentação OpenAPI deve refletir a versão ativa; `v1` e `v2` em paralelo geram dois documentos separados.

## Escopo

**Dentro:**
- Documentar a estratégia (esta spec + ADR-023).
- Manter `/api/v1/` como prefixo único enquanto não houver `v2`.

**Fora (futuro / quando necessário):**
- Implementar `v2` de algum endpoint (gatilho: surgir uma mudança breaking real).
- Filter/interceptor que injete MDC `apiVersion` (gatilho: precisarmos correlacionar logs por versão).
- Documentação OpenAPI separada por versão (gatilho: existir mais de uma versão ativa).

## Critérios de aceite

- [x] Estratégia documentada nesta spec + ADR-020 + ADR-023.
- [x] Endpoints atuais sob `/api/v1/...`.
- [x] `tarefas-bonus.md` reflete o status.
