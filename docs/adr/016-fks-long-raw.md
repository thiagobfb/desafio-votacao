# ADR-016: FKs como `Long` raw (sem `@ManyToOne`)

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

`Sessao` e `Voto` referenciam `Pauta`. JPA oferece `@ManyToOne` com proxies/lazy-loading, mas isso traz custos: `LazyInitializationException` fora de transação, risco de N+1 queries, e proxies que mascaram a entidade real em logs e debug.

## Decisão

FKs ficam como **`Long pautaId` direto** na entidade. Sem `@ManyToOne`. Composição de objetos é feita pelo service via repositórios.

```java
@Entity
public class Sessao {
    @Id @GeneratedValue(strategy = IDENTITY) private Long id;
    private Long pautaId;
    private LocalDateTime abertaEm;
    private LocalDateTime fechaEm;
}
```

DDL preserva `FOREIGN KEY (pauta_id) REFERENCES pauta(id)` no banco — integridade referencial sem proxy do JPA.

## Consequências

**Prós:**
- Sem `LazyInitializationException`.
- Sem N+1 query inadvertido.
- Entidade é POJO honesto — sem proxies do Hibernate aparecerem em debug.
- Integridade referencial continua no banco.

**Trade-offs:**
- Service compõe queries manualmente (`pautaRepository.findById(sessao.getPautaId())` em vez de `sessao.getPauta()`).
- Perde o açúcar do navigation property em queries JPQL.

## Alternativas consideradas

- **`@ManyToOne(fetch = LAZY)`:** clássico, mas exige cuidado com escopo de transação.
- **`@ManyToOne(fetch = EAGER)`:** N+1 implícito em qualquer listagem.
