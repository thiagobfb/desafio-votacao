# ADR-008: Jakarta Bean Validation

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

DTOs de request precisam validar título não-vazio, tamanhos máximos, enum obrigatório etc. Validação deve ocorrer **na borda** (controller), com mensagens uniformes.

## Decisão

Jakarta Bean Validation 3.x (`spring-boot-starter-validation`) em DTOs `record`:

```java
public record CriarPautaRequest(
    @NotBlank @Size(max = 200) String titulo,
    @Size(max = 2000) String descricao
) {}
```

- `@Valid` no `@RequestBody` ativa a validação.
- Falha lança `MethodArgumentNotValidException`, capturada em `GlobalExceptionHandler` (ver [ADR-019](019-tratamento-excecoes.md)) → HTTP 400 com `errors[]`.

## Consequências

**Prós:**
- Declarativo e padronizado.
- Mensagens uniformes via handler global.
- Funciona naturalmente com `record`.

**Trade-offs:**
- Mensagens default em inglês (ex: `"must not be blank"`). Aceitável no escopo; i18n fica para spec futura.

## Alternativas consideradas

- **Validação manual no service:** verboso e dispersa regras.
- **Validador customizado por DTO:** boilerplate sem ganho.
