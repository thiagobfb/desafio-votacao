# Specs — Desafio Votação

Este diretório contém as **especificações** que dirigem o desenvolvimento do projeto, seguindo o modelo **Spec Driven Development** (estilo [Spec Kit](https://github.com/github/spec-kit)).

## Estrutura

```
specs/
├── constitution.md         # Princípios invioláveis (lei do projeto)
├── README.md               # Este arquivo
└── NNN-slug/               # Uma pasta por feature, numerada
    ├── spec.md             # O QUE e POR QUÊ — sem decisões técnicas
    ├── plan.md             # COMO — stack, contratos, estrutura
    └── tasks.md            # Passos executáveis, ordenados
```

## Fluxo de trabalho

```
┌──────────┐    ┌──────────┐    ┌───────────┐    ┌───────────────┐
│ spec.md  │ ─▶ │ plan.md  │ ─▶ │ tasks.md  │ ─▶ │ implementação │
└──────────┘    └──────────┘    └───────────┘    └───────────────┘
   (O QUÊ)        (COMO)        (passos)            (código + testes)
```

Cada fase é revisada antes da próxima começar. **Nunca** pular para o código sem `tasks.md` aprovado.

## Specs atuais

| ID  | Slug                      | Status | Resumo |
|-----|---------------------------|--------|--------|
| 001 | sistema-votacao           | Draft  | Núcleo: pautas, sessões, votos, apuração |
| 002 | validacao-cpf             | TODO   | Bônus 1 — Facade fake de CPF |
| 003 | versionamento-api         | TODO   | Bônus 3 — estratégia de versionamento |
| 004 | performance-alto-volume   | TODO   | Bônus 2 — centenas de milhares de votos |

## Como propor uma nova spec

1. Crie `specs/NNN-slug/spec.md` (próximo número livre, slug em kebab-case).
2. Use o modelo de seções da spec 001 como referência.
3. Mantenha **somente** O QUÊ e POR QUÊ no `spec.md` — decisões técnicas vão para `plan.md`.
4. Adicione a entrada na tabela acima.
