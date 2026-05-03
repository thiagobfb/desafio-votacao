# ADR-013: Camadas N=3 + organização por feature

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

A arquitetura precisa equilibrar simplicidade (Constitution §I — "evitar over-engineering") com testabilidade e localidade de mudança. O domínio é pequeno (4 features: pauta, sessão, voto, resultado).

## Decisão

**Três camadas:** Controller → Service → Repository. Domínio anêmico-pragmático (ver [ADR-014](014-dominio-anemico.md)).

**Organização por feature**, não por camada técnica:

```
br.com.desafio.votacao
├── pauta/
│   ├── api/         # Controller + DTOs
│   ├── domain/      # Entidade + enums
│   ├── repository/
│   └── service/
├── sessao/    ... (idem)
├── voto/      ... (idem)
├── resultado/ ... (idem)
└── shared/
    ├── config/      # ClockConfig, OpenApiConfig
    └── exception/   # NegocioException + handler global
```

## Consequências

**Prós:**
- Código que muda junto fica junto — alterar a feature "voto" toca um pacote.
- Cada feature pode crescer ou ser extraída para módulo independente.
- Camadas são finas e óbvias.

**Trade-offs:**
- Helpers cross-feature ficam em `shared/` (ex: `EstadoPautaResolver` poderia argumentavelmente estar em `pauta/` ou `sessao/`; ficou em `pauta/service/` por afinidade de uso).

## Alternativas consideradas

- **Hexagonal/Onion:** overkill para domínio com 4 entidades.
- **Pacote por camada (`controller/`, `service/`, `repository/`):** dispersa mudanças da mesma feature em pacotes diferentes.
- **DDD tático com Aggregates:** escala não justifica.
