# ADR-012: SLF4J + Logback para logs

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

Plan §6 define pontos de log estruturado: INFO em transições felizes (pauta criada, sessão aberta, voto aceito, sessão expirada detectada), WARN em rejeições (voto duplicado, sessão fechada, 2ª sessão), ERROR em exceções não mapeadas.

## Decisão

**SLF4J facade + Logback** (default do Spring Boot, sem dependências adicionais). Pattern em `logback-spring.xml`:

```
%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n
```

Logs no formato chave-valor:

```java
log.info("Voto registrado id={} pautaId={} associadoId={} escolha={}", ...);
```

`GlobalExceptionHandler` chama `log.error("Erro interno não mapeado", ex)` — única origem de stack trace em log.

## Consequências

**Prós:**
- Padrão do Spring Boot, sem deps extras.
- Pattern legível humanamente; pronto para `grep`/`awk` em ambiente local.
- Reservamos espaço para `traceId` no pattern para integração distribuída futura.

**Trade-offs:**
- Texto plano (não JSON estruturado para ELK). Adoção de logstash-encoder fica para Spec 004 se necessário.

## Alternativas consideradas

- **Log4j2:** sem ganho técnico significativo; troca de default do Spring custa configuração.
- **JSON encoder (logstash):** overkill para o escopo do desafio.
