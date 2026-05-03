# Tasks 003 — Versionamento de API

**Refere-se a:** [spec.md](./spec.md) · [plan.md](./plan.md)

## Fase 1 — Documentação (concluída)

- [x] **T-301** Spec/plan/tasks desta feature.
- [x] **T-302** ADR-020 atualizado de "preliminar" para "Aceito".
- [x] **T-303** ADR-023 — Política de deprecação de versão de API (cabeçalhos `Deprecation`/`Sunset`, prazos, `410 Gone`).
- [x] **T-304** Atualizar `docs/tarefas-bonus.md` (Bônus 3 ⚠️ → ✅).

## Fase 2 — Diferida (gatilho: surgir mudança breaking real)

- [ ] **T-310** Criar `feature/api/v2/` ao lado de `v1/` para o(s) endpoint(s) impactado(s).
- [ ] **T-311** `ApiVersionMdcFilter` populando MDC `apiVersion`; pattern do logback inclui `%X{apiVersion}`.
- [ ] **T-312** `Deprecation` + `Sunset` headers em respostas `v1` (interceptor configurável).
- [ ] **T-313** `springdoc-openapi` — `GroupedOpenApi` para gerar docs `v1` e `v2` separados.
- [ ] **T-314** Comunicação ao cliente: 3 meses antes do release de `v2`, anúncio em changelog/email; 6 meses entre release e `Sunset`.

## Critérios de aceite (Fase 1)

- [x] `/api/v1/` em vigor em todos os endpoints.
- [x] Estratégia documentada (spec + plan + ADR-020 + ADR-023).
- [x] Diferenças entre mudanças "aditivas" e "breaking" explicitadas com exemplos.
