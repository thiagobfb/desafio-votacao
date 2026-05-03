# ADR-017: `Clock` injetado em UTC-3 (offset fixo, não `America/Sao_Paulo`)

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

RN-2 ("sessão fica aberta por X minutos") depende fortemente de `now()`. Hardcodear `LocalDateTime.now()` impede testar expiração sem `Thread.sleep`. Além disso, o sistema é brasileiro — UTC bruto é inconveniente para logs e `criadaEm` exibido ao usuário.

## Decisão

Bean único em `ClockConfig`:

```java
public static final ZoneOffset HORARIO_BRASILIA = ZoneOffset.of("-03:00");

@Bean
public Clock clock() {
    return Clock.system(HORARIO_BRASILIA);
}
```

Services dependem de `Clock` injetado: `LocalDateTime.now(clock)`. Testes de integração injetam `MutableClock` (extensão de `Clock` com `definir(Instant)` e `avancar(Duration)`).

## Consequências

**Prós:**
- Testabilidade: integração ponta-a-ponta avança o relógio em 6 minutos sem dormir.
- Previsibilidade: offset fixo nunca muda.
- Brasil não observa DST desde 2019 — `ZoneOffset.of("-03:00")` e `ZoneId.of("America/Sao_Paulo")` se comportam igual hoje.

**Trade-offs:**
- Se o Brasil voltar a observar DST, é preciso trocar para `ZoneId.of("America/Sao_Paulo")` (mudança em 1 linha de `ClockConfig`).

## Alternativas consideradas

- **`Clock.systemUTC()`:** alinhado com servidores cloud, mas exige conversão em todo log/response.
- **`ZoneId.of("America/Sao_Paulo")`:** observa DST se voltar, mas dependente de `tzdata` atualizado na JVM.
