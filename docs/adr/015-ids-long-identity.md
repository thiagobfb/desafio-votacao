# ADR-015: IDs `Long` IDENTITY (não UUID)

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

Pautas, sessões e votos precisam de identificadores. UUID é mais opaco e neutro em sistemas distribuídos; `BIGINT IDENTITY` é mais simples e legível em URLs e logs. O desafio é um processo seletivo — privilegiamos legibilidade.

## Decisão

`Long id` com `@GeneratedValue(strategy = IDENTITY)`. SQL: `BIGINT GENERATED ALWAYS AS IDENTITY` (padrão SQL, suportado por H2 e PostgreSQL ≥ 10).

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

## Consequências

**Prós:**
- URLs legíveis: `GET /api/v1/pautas/1/resultado`.
- Logs e debugging mais fáceis (`pautaId=1` em vez de `pautaId=550e8400-...`).
- Storage mais compacto (`BIGINT` 8 bytes vs UUID 16 bytes em representação binária).

**Trade-offs:**
- Previsibilidade externa: enumerar IDs é trivial. **Aceitável** porque autenticação está fora de escopo do desafio.
- Não é único entre instâncias — não é problema (sistema é centralizado nesta spec).

## Alternativas consideradas

- **UUID v4:** opaco, distribuído. Strings em URL ficam pesadas; ganho não vale para escopo atual.
- **UUID v7 (time-ordered):** mesmas vantagens do UUID com ordem temporal — interessante para Spec 004.
- **NanoID:** menor que UUID, mas mistura simbólica em URL atrapalha legibilidade.
