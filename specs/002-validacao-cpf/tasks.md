# Tasks 002 — Validação externa de CPF

**Refere-se a:** [spec.md](./spec.md) · [plan.md](./plan.md)

---

## Fase 1 — Schema e modelo

- [x] **T-201** Migration `V2__renomeia_associado_id_para_cpf.sql` (drop constraint → rename column → add constraint).
- [x] **T-202** Renomear `Voto.associadoId → Voto.cpf` (entidade + construtor + Lombok).
- [x] **T-203** Renomear `VotoRepository.existsByPautaIdAndAssociadoId → existsByPautaIdAndCpf`.

## Fase 2 — Validador externo

- [x] **T-210** Criar enum `StatusValidacaoCpf` (`INVALIDO`, `UNABLE_TO_VOTE`, `ABLE_TO_VOTE`).
- [x] **T-211** Criar interface `CpfValidator` com método `validar(cpf)`.
- [x] **T-212** Implementar `FakeCpfValidator` (`@Component`) com algoritmo determinístico de DV1+DV2 (Receita Federal) para o formato; `Random` injetável só sorteia entre `ABLE_TO_VOTE` e `UNABLE_TO_VOTE` para CPF estruturalmente válido.
- [x] **T-213** Teste `FakeCpfValidatorTest` com casos parametrizados (CPF válido, DV1 errado, DV2 errado, comprimento incorreto, não-numérico, formatado, null, vazio) e seed para sorteio de habilitação.

## Fase 3 — Integração com VotoService

- [x] **T-220** Adicionar `CpfInvalidoException` e `AssociadoNaoPodeVotarException` em `shared/exception`.
- [x] **T-221** Atualizar `GlobalExceptionHandler` para mapear as duas novas exceções em 404 (mesmo handler de `RecursoNaoEncontradoException`).
- [x] **T-222** Injetar `CpfValidator` em `VotoService` e chamar `validar(cpf)` antes das demais checagens.
- [x] **T-223** Atualizar mensagens e logs de `VotoService` (associado → CPF). Atualizar `VotoDuplicadoException`.

## Fase 4 — API

- [x] **T-230** Renomear campo do `RegistrarVotoRequest` (`associadoId → cpf`).
- [x] **T-231** Atualizar `VotoController` para passar `req.cpf()`.

## Fase 5 — Testes existentes

- [x] **T-240** `VotoServiceTest` — adicionar mock `CpfValidator`, atualizar parâmetros, adicionar 2 testes para `INVALIDO` e `UNABLE_TO_VOTE`.
- [x] **T-241** `VotoControllerTest` — atualizar JSON `associadoId → cpf`, adicionar 2 testes (404 para CPF inválido / associado não habilitado).
- [x] **T-242** `PersistenciaIntegracaoTest` — atualizar Voto e nome do método `existsByPautaIdAndCpf...`.
- [x] **T-243** `FluxoCompletoIntegracaoTest` — atualizar JSON `cpf` e prover `@Primary CpfValidator` permissivo via `@TestConfiguration`.

## Fase 6 — Verificação e documentação

- [x] **T-250** `mvn verify` verde — **67 testes** (60 anteriores + 3 `FakeCpfValidatorTest` + 2 `VotoService` + 2 `VotoController`).
- [x] **T-251** Atualizar `docs/tarefas-bonus.md` (Bônus 1: ❌ → ✅).
- [x] **T-252** Atualizar `README.md` (`Próximas specs`).

---

## Dependências

```
T-201 ─▶ T-202 ─▶ T-203
                    │
                    ▼
T-210 ─▶ T-211 ─▶ T-212 ─▶ T-213
                    │
                    ▼
T-220 ─▶ T-221 ─▶ T-222 ─▶ T-223
                              │
                              ▼
T-230 ─▶ T-231
            │
            ▼
T-240 / T-241 / T-242 / T-243   (paralelo)
            │
            ▼
T-250 ─▶ T-251 ─▶ T-252
```
