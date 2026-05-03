# Plan 002 — Validação externa de CPF

**Refere-se a:** [spec.md](./spec.md)
**Status:** Aceito
**Data:** 2026-05-01

## 1. Decisões de design

| # | Decisão | Justificativa |
|---|---|---|
| 1 | Nova feature em `cpf/` com `domain/` (interface + enum) e `service/` (impl fake) | Mesmo padrão das outras features (pauta/, voto/, ...), mantém localidade. |
| 2 | `CpfValidator` é uma **interface**; impl `FakeCpfValidator` é o único `@Component` registrado | Substituível por cliente HTTP real numa próxima spec sem tocar `VotoService`. |
| 3 | Campo da API e do schema renomeado de `associadoId` → `cpf` | Nome semanticamente correto para esta feature. Migration Flyway V2 evita perda de dados existentes. |
| 4 | `INVALIDO` e `UNABLE_TO_VOTE` viram **404** com mensagens distintas | Segue literalmente o exemplo do enunciado (`"retornar 404 no client tb"`). |
| 5 | Validação de CPF acontece **antes** das demais validações (pauta/sessão) | Fail-fast: invalidação por CPF é a checagem mais barata e específica desta feature. |
| 6 | `Random` é injetável no construtor de `FakeCpfValidator` | Permite testes determinísticos com seed; default (`SecureRandom`) cobre produção. |
| 7 | Em testes de integração ponta-a-ponta, um `CpfValidator` "permissivo" é provido via `@TestConfiguration` (`@Primary`) | Evita falhas aleatórias atrapalhando asserções determinísticas — mesmo padrão usado para `MutableClock`. |

## 2. Modelo

### 2.1 Interface

```java
// br.com.desafio.votacao.cpf.domain.CpfValidator
public interface CpfValidator {
    StatusValidacaoCpf validar(String cpf);
}
```

### 2.2 Enum

```java
public enum StatusValidacaoCpf {
    INVALIDO,         // CPF não encontrado / inválido
    UNABLE_TO_VOTE,   // CPF válido, mas associado não pode votar agora
    ABLE_TO_VOTE      // CPF válido, associado pode votar
}
```

### 2.3 Impl fake

A validação é dividida em duas etapas:

1. **Formato (determinístico)** — algoritmo dos dígitos verificadores DV1 e DV2 (Receita Federal):
   ```
   Soma1 = d1*10 + d2*9 + d3*8 + d4*7 + d5*6 + d6*5 + d7*4 + d8*3 + d9*2
   Resto1 = Soma1 % 11
   DV1 = (Resto1 < 2) ? 0 : 11 - Resto1
   ↳ exige d10 == DV1

   Soma2 = d1*11 + d2*10 + d3*9 + d4*8 + d5*7 + d6*6 + d7*5 + d8*4 + d9*3 + DV1*2
   Resto2 = Soma2 % 11
   DV2 = (Resto2 < 2) ? 0 : 11 - Resto2
   ↳ exige d11 == DV2
   ```
   CPFs que falham nessa checagem retornam **`INVALIDO`** (sem usar `Random`).

2. **Habilitação (aleatória)** — CPF estruturalmente válido recebe aleatoriamente
   `ABLE_TO_VOTE` ou `UNABLE_TO_VOTE`, conforme o enunciado:
   *"Essa operação retorna resultados aleatórios, portanto um mesmo CPF pode funcionar em
   um teste e não funcionar no outro."*

```java
@Component
public class FakeCpfValidator implements CpfValidator {
    private final Random random;

    public FakeCpfValidator() { this(new SecureRandom()); }

    FakeCpfValidator(Random random) { this.random = random; } // p/ testes

    @Override
    public StatusValidacaoCpf validar(String cpf) {
        if (!formatoValido(cpf)) {
            return StatusValidacaoCpf.INVALIDO;
        }
        return random.nextBoolean()
                ? StatusValidacaoCpf.ABLE_TO_VOTE
                : StatusValidacaoCpf.UNABLE_TO_VOTE;
    }

    private boolean formatoValido(String cpf) { /* algoritmo DV1 + DV2 */ }
}
```

Aceita CPF formatado (`111.444.777-35`) e nu (`11144477735`) — caracteres não numéricos
são removidos antes de aplicar o algoritmo.

## 3. Mudanças no schema

### 3.1 Migration `V2__renomeia_associado_id_para_cpf.sql`

```sql
ALTER TABLE voto DROP CONSTRAINT uk_voto_pauta_associado;
ALTER TABLE voto RENAME COLUMN associado_id TO cpf;
ALTER TABLE voto ADD CONSTRAINT uk_voto_pauta_cpf UNIQUE (pauta_id, cpf);
```

Funciona em H2 e PostgreSQL (sintaxe SQL padrão).

### 3.2 Renames no Java

| Antes | Depois |
|---|---|
| `Voto.associadoId` | `Voto.cpf` |
| `VotoRepository.existsByPautaIdAndAssociadoId` | `VotoRepository.existsByPautaIdAndCpf` |
| `RegistrarVotoRequest.associadoId` | `RegistrarVotoRequest.cpf` |
| `VotoService.registrar(pautaId, associadoId, escolha)` | `VotoService.registrar(pautaId, cpf, escolha)` |
| Mensagem `VotoDuplicadoException` | "CPF X já votou na pauta Y" |

## 4. Exceções

| Nova exceção | HTTP | Mensagem padrão |
|---|---|---|
| `CpfInvalidoException` | 404 | `"CPF X inválido ou não encontrado"` |
| `AssociadoNaoPodeVotarException` | 404 | `"Associado com CPF X não está habilitado a votar no momento"` |

Ambas estendem `NegocioException`. `GlobalExceptionHandler` agrupa as duas com `RecursoNaoEncontradoException` no handler de 404.

## 5. Logs (extensão do plan §6 da Spec 001)

- **WARN** `Voto rejeitado: CPF inválido pautaId={} cpf={}`
- **WARN** `Voto rejeitado: associado não habilitado pautaId={} cpf={}`
- **DEBUG** (`FakeCpfValidator`): `Validação aleatória de CPF cpf={} resultado={}`

## 6. Testes

### Novos
- `FakeCpfValidatorTest` — com `Random` mockado/seedado, garante que os 3 estados aparecem.
- `VotoServiceTest` — 2 novos cenários: `INVALIDO` → `CpfInvalidoException`; `UNABLE_TO_VOTE` → `AssociadoNaoPodeVotarException`.
- `VotoControllerTest` — 2 novos cenários: 404 com mensagem para CPF inválido e para associado não habilitado.

### Atualizados
- Toda referência a `associadoId` em testes → `cpf`. Valores literais (`"A1"`, `"A2"`...) podem permanecer — o validador é mockado/permissivo.
- `VotoServiceTest` ganha mock de `CpfValidator` retornando `ABLE_TO_VOTE` por padrão nos cenários "happy".
- `FluxoCompletoIntegracaoTest` ganha um bean `@Primary CpfValidator` que sempre retorna `ABLE_TO_VOTE` (`@TestConfiguration`).

## 7. Sem-mudanças

- Endpoint `/api/v1/pautas/{id}/votos` continua sob `v1` ([ADR-020](../../docs/adr/020-versionamento-uri.md)) — mudança de campo é tratada como evolução compatível dentro da mesma versão *para fins do desafio*; em um cenário real exigiria `v2`.
- Spec 001 (estados, fluxos, RNs) permanece intacta.

## 8. Riscos

| Risco | Mitigação |
|---|---|
| Migration V2 falhar em base existente | V2 usa `ALTER TABLE ... RENAME COLUMN` (H2 + Postgres). Em caso extremo, avaliador apaga `./data/votacao.mv.db` e reinicia. |
| Avaliador rodar manualmente e cair na resposta aleatória | Documentado no README — comportamento esperado da Tarefa Bônus 1. |
| Testes flaky pela aleatoriedade | Toda surface de teste mocka `CpfValidator` ou usa `@Primary` permissivo. `FakeCpfValidatorTest` usa `Random` seedado. |
