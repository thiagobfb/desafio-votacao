# Spec 002 — Validação externa de CPF

**Refere-se à Tarefa Bônus 1 do desafio.**
**Status:** Aceita
**Data:** 2026-05-01
**Constitution:** v1

## Resumo

O associado passa a ser identificado por **CPF**, e o registro de voto é precedido por uma chamada a um **serviço fake de validação de CPF** que combina:

- **Validação determinística de formato** — algoritmo dos dígitos verificadores DV1 e DV2 (Receita Federal). CPF malformado, com DVs errados, comprimento incorreto, etc. → `INVALIDO`.
- **Habilitação aleatória** — CPF estruturalmente válido sorteia entre `ABLE_TO_VOTE` e `UNABLE_TO_VOTE`, conforme o enunciado ("um mesmo CPF pode funcionar em um teste e não funcionar no outro").

## Leitura do enunciado (decisão explícita)

O texto do desafio tem dois bullets que falam em "aleatório":

1. *"Criar uma Facade/Client Fake que retorna **aleatoriamente** se um CPF recebido é válido ou não."*
2. *"Caso o CPF seja válido, a API retornará se o usuário pode (ABLE_TO_VOTE) ou não pode (UNABLE_TO_VOTE) executar a operação. **Essa operação retorna resultados aleatórios**, portanto um mesmo CPF pode funcionar em um teste e não funcionar no outro."*

Há **duas leituras possíveis** do bullet 1:

- **Leitura A — literal:** a *validade em si* é aleatória; a Facade retorna `INVALIDO` ou não com probabilidade aleatória, independente do conteúdo do CPF.
- **Leitura B — adotada aqui:** a Facade aplica **validação real de CPF** (algoritmo DV1+DV2); o "aleatoriamente" do bullet 1 refere-se ao mesmo `Random` que decide habilitação no bullet 2 — ou seja, a **única** dimensão aleatória do sistema é `ABLE_TO_VOTE` vs `UNABLE_TO_VOTE`.

**Por que a Leitura B:** o terceiro item do enunciado diz *"Você pode usar **geradores de CPF** para gerar **CPFs válidos**"*. Essa dica só faz sentido se o validador considerar o formato real — caso contrário, qualquer string de 11 dígitos seria equivalente. A consistência interna do enunciado (com a dica do gerador) puxa a interpretação para validação determinística + habilitação aleatória.

Trade-off: leitor estrito do bullet 1 pode achar que "validade aleatória" foi reduzida demais. Em contrapartida, a Leitura B é mais útil na prática (testes manuais com CPFs válidos não falham por sorteio na primeira etapa) e dá significado à dica do gerador.

## Requisitos funcionais

- **RF-2.1.** O endpoint `POST /api/v1/pautas/{id}/votos` aceita o campo `cpf` (substitui `associadoId`).
- **RF-2.2.** Antes de qualquer outra validação de domínio, o serviço chama o validador externo de CPF.
- **RF-2.3.** Se o validador retornar `INVALIDO`, a API responde **HTTP 404** com mensagem identificando o CPF.
- **RF-2.4.** Se o validador retornar `UNABLE_TO_VOTE`, a API responde **HTTP 404** indicando que o associado não está habilitado.
- **RF-2.5.** Se o validador retornar `ABLE_TO_VOTE`, o fluxo de voto continua normalmente (regras da Spec 001).

> O retorno 404 nos dois casos de erro segue o exemplo do enunciado (`"// CPF Nao Ok para votar - retornar 404 no client tb"`).

## Regras de negócio

- **RN-2.1.** A unicidade de voto (`UNIQUE(pauta_id, cpf)`) é mantida — herda RN-3 da Spec 001.
- **RN-2.2.** A validação de CPF acontece **fora da transação de banco** (não há lock nem alteração de schema durante a chamada externa).
- **RN-2.3.** O serviço fake é configurado por componente Spring; não há chamada HTTP real (Tarefa Bônus 1 explicita "Facade/Client Fake").

## Não-funcionais

- **RNF-2.1.** A interface `CpfValidator` deve permitir trocar a implementação fake por uma real (ex.: cliente HTTP) na Spec futura, sem mexer nos serviços que dependem dela.
- **RNF-2.2.** Em testes que precisam de comportamento determinístico, é possível injetar uma implementação alternativa via `@Primary` (`@TestConfiguration`).

## Fluxo

```
Cliente
  │ POST /api/v1/pautas/{id}/votos { "cpf": "12345678901", "voto": "SIM" }
  ▼
[VotoController] @Valid
  ▼
[VotoService.registrar]
  │ cpfValidator.validar(cpf)
  │  ├─ INVALIDO        → CpfInvalidoException             → 404
  │  ├─ UNABLE_TO_VOTE  → AssociadoNaoPodeVotarException   → 404
  │  └─ ABLE_TO_VOTE    → continua
  │
  │ pautaService.buscarObrigatorio(pautaId)               → 404
  │ sessaoService.buscarPorPautaId(pautaId)               → 409
  │ sessao.estaAbertaEm(now)                              → 409
  │ votoRepository.saveAndFlush                           → 409 (duplicado)
  ▼
201 + { votoId, registradoEm }
```

## Escopo

**Dentro:**
- Validar CPF (formato real via DV1+DV2) antes de registrar voto.
- Sorteio de habilitação `ABLE_TO_VOTE` / `UNABLE_TO_VOTE` para CPFs estruturalmente válidos.
- Renomear o campo `associadoId` para `cpf` em API, entidade, schema do banco, repositório e exceções.
- Migration Flyway para renomear coluna e constraint sem perda de dados.
- Testes determinísticos do algoritmo de DV + cobertura de habilitação aleatória.

**Fora:**
- Cliente HTTP real para um serviço externo (ficaria em Spec 002.1 ou 005).
- Cache de respostas do validador.
- Rejeição explícita de CPFs com todos os dígitos iguais (`11111111111`, `22222222222`, ...) — passam o algoritmo matemático e o enunciado não pede esse filtro adicional.

## Critérios de aceite

- [x] `mvn verify` permanece verde.
- [x] Todos os testes existentes da Spec 001 continuam passando após o rename `associadoId → cpf`.
- [x] Testes cobrem: algoritmo determinístico de DV (CPF válido puro e formatado, DV1 errado, DV2 errado, comprimento, não-numérico, null, vazio), distribuição entre `ABLE_TO_VOTE` e `UNABLE_TO_VOTE`, `VotoService` rejeitando `INVALIDO` e `UNABLE_TO_VOTE`, controller respondendo 404 nos dois casos.
- [x] Migration `V2` executa em H2 e PostgreSQL sem perda de dados.
