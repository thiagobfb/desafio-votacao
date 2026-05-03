# Spec 001 — Sistema de Votação Cooperativista

**Status:** Draft
**Autor:** Equipe desafio-votacao
**Data:** 2026-04-30
**Constitution:** v1 (`specs/constitution.md`)

---

## 1. Contexto e motivação

No cooperativismo, cada associado possui um voto e as decisões são tomadas em assembleias. Hoje o processo é presencial ou ad hoc. Precisamos de um backend que permita aplicações cliente (mobile) gerenciarem e participarem dessas sessões de votação remotamente.

Esta spec cobre o **núcleo do sistema**: pautas, sessões, votos e apuração. Integrações externas e considerações de versionamento/performance estão escopadas como specs separadas (002, 003, 004) para manter o foco.

---

## 2. Objetivo

Entregar uma API REST que permita:

- **Cadastrar** pautas a serem votadas.
- **Abrir** sessões de votação sobre pautas, com duração configurável.
- **Registrar** votos de associados (Sim/Não), garantindo unicidade.
- **Apurar** o resultado de uma pauta após o encerramento da sessão.

---

## 3. Fora de escopo (desta spec)

- Autenticação/autorização (README diz: _"a segurança das interfaces pode ser abstraída"_).
- Validação externa de CPF (Spec 002).
- Estratégia de versionamento da API (Spec 003).
- Otimização para centenas de milhares de votos (Spec 004).
- Aplicação cliente mobile (não faz parte da avaliação).
- Notificações em tempo real (push, websocket).

---

## 4. Personas e atores

| Ator | Descrição |
|---|---|
| **Administrador da assembleia** | Cadastra pautas e abre sessões. Identidade não é validada nesta spec. |
| **Associado** | Vota em pautas. Identificado por um `associadoId` único (string opaca; CPF tratado na Spec 002). |
| **Observador / Cliente** | Consulta pautas e resultados. |

---

## 5. Glossário

- **Pauta** — Tópico submetido à votação. Tem identificador, título e descrição.
- **Sessão de votação** — Janela de tempo em que uma pauta aceita votos. Uma pauta pode ter múltiplas sessões ao longo do tempo? **Decisão:** _Não nesta versão — uma sessão por pauta_ (ver §10 RN-1).
- **Voto** — Manifestação binária (`SIM`/`NAO`) de um associado em uma pauta cuja sessão está aberta.
- **Resultado** — Contagem final (totalSim, totalNao, total, vencedor, empate?) calculada após encerramento.

---

## 6. Requisitos funcionais

### RF-1 — Cadastrar pauta
**Como** administrador, **quero** cadastrar uma nova pauta, **para que** ela possa ser submetida à votação.

**Critérios de aceite:**
- Aceita `titulo` (obrigatório, 1–200 caracteres) e `descricao` (opcional, até 2000 caracteres).
- Retorna identificador único e os dados persistidos.
- Pauta recém-cadastrada está no estado `SEM_SESSAO` (sem sessão aberta).

### RF-2 — Abrir sessão de votação
**Como** administrador, **quero** abrir uma sessão sobre uma pauta existente, **para que** associados possam votar.

**Critérios de aceite:**
- Recebe `pautaId` e `duracaoMinutos` (opcional; default = 1).
- Rejeita se a pauta já tiver sessão aberta ou encerrada (RN-1).
- Rejeita se `duracaoMinutos <= 0`.
- Retorna `sessaoId`, `abertaEm`, `fechaEm`.
- Estado da pauta passa para `SESSAO_ABERTA`.

### RF-3 — Votar em uma pauta
**Como** associado, **quero** registrar meu voto Sim/Não em uma pauta com sessão aberta.

**Critérios de aceite:**
- Recebe `pautaId` (ou `sessaoId`), `associadoId`, `voto` ∈ {`SIM`, `NAO`}.
- Aceita apenas se a sessão da pauta estiver aberta no instante da requisição (RN-2).
- Rejeita voto duplicado do mesmo `associadoId` na mesma pauta (RN-3).
- Retorna confirmação com timestamp do voto.

### RF-4 — Apurar resultado
**Como** observador, **quero** consultar o resultado de uma pauta.

**Critérios de aceite:**
- Retorna `totalSim`, `totalNao`, `totalVotos`, e `resultado` ∈ {`APROVADA`, `REJEITADA`, `EMPATE`, `EM_ANDAMENTO`, `SEM_SESSAO`}.
- Enquanto a sessão está aberta, retorna `EM_ANDAMENTO` com contagem parcial.
- Após encerramento, resultado é determinístico e estável.

### RF-5 — Listar pautas (suporte ao cliente)
**Como** cliente, **quero** listar pautas para escolher onde votar.

**Critérios de aceite:**
- Endpoint paginado retornando pautas com seu estado atual.
- Filtragem por estado (`SEM_SESSAO`, `SESSAO_ABERTA`, `ENCERRADA`) é desejável mas não obrigatória nesta spec.

---

## 7. Requisitos não-funcionais

| ID | Requisito |
|---|---|
| RNF-1 | Persistência durável (sobrevive a restart) — Constitution §IV. |
| RNF-2 | Concorrência: dois votos simultâneos do mesmo associado na mesma pauta resultam em **um único registro** (o segundo é rejeitado). |
| RNF-3 | Tempo de resposta P95 < 300 ms para operações unitárias em carga típica de teste. |
| RNF-4 | Logs estruturados em todas as transições de estado e rejeições por regra de negócio. |
| RNF-5 | Documentação automática da API (OpenAPI) acessível em rota conhecida. |

---

## 8. Fluxos principais

### Fluxo feliz — Da pauta ao resultado

```
1. POST /pautas                       → cria pauta P1
2. POST /pautas/P1/sessoes {min:5}    → abre sessão S1 com fecha_em = now + 5min
3. POST /pautas/P1/votos {assoc:A, voto:SIM}  → aceita
4. POST /pautas/P1/votos {assoc:B, voto:NAO}  → aceita
5. POST /pautas/P1/votos {assoc:A, voto:NAO}  → rejeita (RN-3)
6. (5 minutos depois) GET /pautas/P1/resultado → APROVADA / REJEITADA / EMPATE
```

### Fluxos de erro relevantes
- Votar em pauta sem sessão → 409 Conflict.
- Votar em sessão encerrada → 409 Conflict.
- Votar com `voto` inválido → 400 Bad Request.
- Pauta inexistente → 404 Not Found.
- Voto duplicado → 409 Conflict.

---

## 9. Modelo de dados conceitual

> _Detalhamento físico (tabelas, índices, tipos) fica no `plan.md`._

```
Pauta
  - id
  - titulo
  - descricao
  - criadaEm

Sessao
  - id
  - pautaId  (1:1 nesta versão)
  - abertaEm
  - fechaEm

Voto
  - id
  - pautaId
  - associadoId
  - escolha  (SIM | NAO)
  - registradoEm
  - UNIQUE(pautaId, associadoId)
```

---

## 10. Regras de negócio

| ID | Regra |
|---|---|
| RN-1 | Uma pauta tem **no máximo uma sessão** ao longo de sua vida. Reabertura não é permitida nesta versão. |
| RN-2 | Sessão é considerada aberta sse `abertaEm <= agora < fechaEm`. O encerramento é por tempo, sem ação manual. |
| RN-3 | Cada `associadoId` vota **no máximo uma vez** por pauta. A unicidade é garantida no banco (constraint), não apenas em código. |
| RN-4 | Resultado é calculado on-demand a partir dos votos persistidos — sem materialização prematura. |
| RN-5 | Empate (`totalSim == totalNao`) é resultado válido e distinto de aprovação/rejeição. |

---

## 11. Critérios de pronto (Definition of Done)

- [ ] Todos RF-1 a RF-5 implementados com testes automatizados cobrindo cenários felizes e de erro.
- [ ] Constraints de unicidade aplicadas no banco (não só no código).
- [ ] Logs estruturados em todas transições.
- [ ] Documentação OpenAPI publicada.
- [ ] README do repositório descreve como rodar, testar e principais decisões.
- [ ] Plan.md e tasks.md desta spec marcados como concluídos.

---

## 12. Questões em aberto

- **Q1:** Reabrir sessão é necessário? _Decisão atual: não (RN-1)._ Validar com avaliador se sair na entrega final.
- **Q2:** Resultado pós-encerramento deve ser materializado/cacheado? _Decisão atual: não (RN-4) — calcular on-demand é simples e suficiente para o cenário base; performance entra na Spec 004._

---

## 13. Specs relacionadas

- **Spec 002** — Validação externa de CPF (Tarefa Bônus 1).
- **Spec 003** — Estratégia de versionamento de API (Tarefa Bônus 3).
- **Spec 004** — Performance e suporte a alto volume de votos (Tarefa Bônus 2).
