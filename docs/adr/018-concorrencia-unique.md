# ADR-018: Concorrência via constraint UNIQUE do banco

- **Status:** Aceito
- **Data:** 2026-04-30
- **Contexto:** Spec 001

## Contexto

RN-1 — uma sessão por pauta. RN-3 — um voto por (pauta, associado). Ambas precisam ser corretas sob concorrência (RNF-2). Um padrão SELECT-then-INSERT em service tem race condition entre threads/transações.

## Decisão

**Constraints UNIQUE no banco** são a única fonte de verdade:

```sql
CONSTRAINT uk_sessao_pauta UNIQUE (pauta_id),
CONSTRAINT uk_voto_pauta_associado UNIQUE (pauta_id, associado_id),
```

Service tenta inserir e captura a violação:

```java
try {
    Voto salvo = votoRepository.saveAndFlush(voto);
    return salvo;
} catch (DataIntegrityViolationException e) {
    throw new VotoDuplicadoException(pautaId, associadoId);
}
```

`saveAndFlush` força a constraint a estourar **dentro do try** (sem isso, o flush ocorre no commit, fora do bloco).

## Consequências

**Prós:**
- Banco é fonte única de verdade — concorrência entre nodes futuros funciona naturalmente.
- Nenhuma race condition possível.
- Service fica simples — sem lock.

**Trade-offs:**
- Acoplado ao schema: a constraint precisa existir; Flyway é a fonte (ver [ADR-007](007-flyway.md)).
- Mensagem de erro de constraint é genérica até o handler traduzir para `VotoDuplicadoException` / `SessaoJaExisteException`.

## Alternativas consideradas

- **Lock pessimista (`SELECT ... FOR UPDATE`):** lentidão e deadlock potencial.
- **Lock otimista (`@Version`):** previne perda em UPDATE concorrente, mas não previne INSERT duplicado de registros novos.
- **Checagem em código (`existsBy...` antes de salvar):** clássica race — duas threads passam no check, ambas inserem.
