# ADR-014: Domínio anêmico-pragmático

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

Constitution §I prioriza simplicidade. Domínio rico (rich entity) traria valor em sistemas com regras complexas, invariantes profundas ou lógica que muda mais que a tecnologia — não é o caso aqui.

## Decisão

- Entidades JPA carregam **estado + uma validação trivial** ligada a invariantes próximas dos dados. Exemplo: `Sessao.estaAbertaEm(LocalDateTime momento)` — pertence à `Sessao` porque depende apenas dos seus campos.
- **Regras de negócio vão para o Service**: abrir sessão, validar voto, apurar resultado.
- **Validações de DTO** vão para Bean Validation no record (ver [ADR-008](008-bean-validation.md)).

## Consequências

**Prós:**
- Refactor é simples — alterar regra é tocar um service e seus testes.
- Entidade fica próxima ao schema, sem indireção desnecessária.
- Curva de aprendizado baixa para novos contribuintes.

**Trade-offs:**
- Lógica espalhada se o domínio crescer; mitigamos isso isolando bem por feature ([ADR-013](013-camadas-por-feature.md)).
- Não captura invariantes via tipos — depende de testes para garantir RNs.

## Alternativas consideradas

- **Rich domain (DDD):** Aggregate Root, Value Objects, Domain Events. Custo desproporcional ao escopo.
- **Domain Model pattern com "service-less":** lógica em entidades pode crescer demais para seguir testando como unidade isolada.
