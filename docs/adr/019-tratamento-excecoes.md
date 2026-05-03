# ADR-019: Tratamento de exceções via `@RestControllerAdvice` (estilo guitar-gpt)

- **Status:** Aceito
- **Data:** 2026-05-01
- **Contexto:** Spec 001

## Contexto

A API precisa de respostas de erro **uniformes** (mesma forma para 400, 404, 409, 500), **sem leak de stack trace** e com tratamento especial para enums inválidos (`{"voto": "MAYBE"}` deve responder algo melhor que "Could not parse").

A decisão pré-existente em `guitar-gpt` foi tomada como referência — estilo minimalista validado em outro projeto.

## Decisão

**Hierarquia:**

```
NegocioException (abstract)
├── RecursoNaoEncontradoException     → 404
├── SessaoJaExisteException           → 409
├── SessaoEncerradaException          → 409
├── SessaoNaoAbertaException          → 409
└── VotoDuplicadoException            → 409
```

**`ErroResponse` minimalista:**

```java
public record ErroResponse(int status, String message, List<String> errors, LocalDateTime timestamp) { ... }
```

`GlobalExceptionHandler` (`@RestControllerAdvice`) mapeia:

| Exceção | HTTP | Mensagem |
|---|---|---|
| `RecursoNaoEncontradoException` | 404 | da própria exception |
| `Sessao*Exception` / `VotoDuplicadoException` | 409 | da própria exception |
| `MethodArgumentNotValidException` | 400 | "Falha de validação" + `errors[]` |
| `HttpMessageNotReadableException` (enum) | 400 | "Valor inválido para 'X'. Aceitos: [...]" |
| `MethodArgumentTypeMismatchException` (path enum) | 400 | mesma mensagem |
| `IllegalArgumentException` | 400 | da própria exception |
| `Exception` (qualquer outra) | 500 | "Erro interno do servidor" + `log.error` com stack |

## Consequências

**Prós:**
- Respostas previsíveis para o cliente.
- Stack trace só vai para `log.error` — nunca para a resposta HTTP.
- Enum inválido vira mensagem útil (`Aceitos: [SIM, NAO]`).

**Trade-offs:**
- `timestamp` usa `LocalDateTime.now()` (zona default da JVM) em vez do `Clock` injetado — alinhado com guitar-gpt; trade-off conhecido.

## Alternativas consideradas

- **`ResponseStatusException` por exceção:** acopla controller a HTTP.
- **Handler por controller:** duplicação.
- **RFC 7807 Problem Details:** mais formal, mas custa campos extras pouco usados em UIs simples.
