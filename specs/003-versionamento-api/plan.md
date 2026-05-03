# Plan 003 — Versionamento de API

**Refere-se a:** [spec.md](./spec.md)
**Status:** Aceito
**Data:** 2026-05-02

## 1. Decisões de design

| # | Decisão | Justificativa |
|---|---|---|
| 1 | Versão por **prefixo de URI** (`/api/vN/`) | Visível em logs/curl/CDN; cacheável por path; cliente sabe imediatamente qual contrato consome. ADR-020. |
| 2 | Mudanças aditivas **não** bumpam versão | Cliente antigo ignora campo novo; bumpar a cada adição polui rotas e força clientes a migrar sem ganho. |
| 3 | Mudanças breaking forçam `vN+1` em paralelo | `vN` deprecado mas funcional por ≥ 6 meses. Headers `Deprecation` + `Sunset` em respostas `vN`. ADR-023. |
| 4 | Roteamento por controllers em pacotes versionados (`feature/api/vN/`) | Compilador garante separação; refactor de uma versão não toca outra. |
| 5 | Services e repositórios são compartilhados quando o domínio não muda; quando muda, services dedicados por versão | Evita "if version == 2" espalhado pelo código. |
| 6 | OpenAPI com docs separados por versão (`/v3/api-docs/v1`, `/v3/api-docs/v2`) | Padrão springdoc — `GroupedOpenApi` filtra por path. |

## 2. Mecanismo de coexistência (quando `v2` existir)

```
src/main/java/br/com/desafio/votacao/pauta/api/
├── v1/
│   ├── PautaControllerV1.java       @RequestMapping("/api/v1/pautas")
│   └── dto/                         # CriarPautaRequest, PautaResponse v1
└── v2/
    ├── PautaControllerV2.java       @RequestMapping("/api/v2/pautas")
    └── dto/                         # CriarPautaRequest v2 (campos diferentes)
```

Se a regra de negócio é a mesma, ambos os controllers chamam `PautaService` e converte DTO ↔ entidade. Se a regra muda, `PautaServiceV2` separado.

## 3. Headers de deprecação

Quando `v2` for lançada, todo endpoint `v1` adiciona via interceptor:

```
HTTP/1.1 200 OK
Deprecation: true
Sunset: Wed, 02 Nov 2026 00:00:00 GMT
Link: <https://exemplo.com/api/v2/pautas>; rel="successor-version"
```

`Deprecation` ([draft-ietf-httpapi-deprecation-header](https://datatracker.ietf.org/doc/draft-ietf-httpapi-deprecation-header/)) e `Sunset` ([RFC 8594](https://datatracker.ietf.org/doc/html/rfc8594)) são padrões IETF.

## 4. Observabilidade

Quando houver mais de uma versão, adicionar filtro:

```java
@Component
public class ApiVersionMdcFilter extends OncePerRequestFilter {
    private static final Pattern VERSION = Pattern.compile("^/api/(v\\d+)/");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        Matcher m = VERSION.matcher(req.getRequestURI());
        if (m.find()) MDC.put("apiVersion", m.group(1));
        try { chain.doFilter(req, res); }
        finally { MDC.remove("apiVersion"); }
    }
}
```

Pattern do logback-spring.xml inclui `%X{apiVersion}` para ficar visível em todo log da request.

## 5. Política de incidentes

- Anúncio de nova versão: **3 meses** antes da liberação para clientes acompanharem.
- `Deprecation` headers ativados no dia 1 da `vN+1`.
- `Sunset` aponta para 6 meses depois.
- Após `Sunset`, `vN` retorna `410 Gone` por mais 30 dias antes de ser removida totalmente.

## 6. O que NÃO mudaríamos

- A interface (REST sobre HTTP/JSON) — versionamento é por **forma de URI**, não por protocolo.
- O OpenAPI 3.x continua sendo a fonte para Swagger UI.
- O Tomcat embarcado e a porta 8080 são neutros.

## 7. Trabalho deferido

- Criar `v2` exige uma mudança breaking real motivada por feedback do cliente — não criamos `v2` "para mostrar". A spec está pronta para o dia em que isso ocorrer.
- O filtro `ApiVersionMdcFilter` será introduzido junto com a primeira release multi-versão (gatilho concreto justifica o código).
