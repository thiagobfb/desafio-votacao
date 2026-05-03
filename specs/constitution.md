# Constitution — Projeto Desafio Votação

> Princípios invioláveis que governam todas as decisões deste projeto.
> Specs, plans e tasks DEVEM ser consistentes com este documento.
> Alterações aqui exigem justificativa explícita.

---

## I. Simplicidade acima de tudo

**Regra:** A solução mais simples que resolve o problema vence. Frameworks, camadas, padrões e abstrações só entram se houver dor concreta justificando.

**Não fazer:**
- DDD pesado (Aggregates, Value Objects elaborados, Domain Events) sem necessidade.
- Hexagonal/Onion completa quando 3 camadas (controller → service → repository) bastam.
- Microsserviços, CQRS, Event Sourcing.
- Abstrações para "futuro hipotético".

**Critério de avaliação explícito do desafio:** _"Simplicidade no design da solução (evitar over engineering)"_.

---

## II. Spec antes de código

**Regra:** Toda feature relevante começa por uma spec em `specs/NNN-slug/spec.md`. Spec descreve **O QUE** e **POR QUÊ**, nunca **COMO**.

**Fluxo:**
1. `spec.md` — requisitos funcionais, regras de negócio, critérios de aceite.
2. `plan.md` — decisões técnicas, contratos, estrutura de pastas, escolhas justificadas.
3. `tasks.md` — passos executáveis, ordenados por dependência, cada um testável.
4. Implementação — só após `tasks.md` aprovado.

Pular fase exige justificativa registrada na própria spec.

---

## III. Testes são parte da definição de pronto

**Regra:** Código sem teste automatizado não está pronto. Cada caso de uso (controller/service público) tem ao menos um teste — unidade ou integração — que falharia se a regra fosse violada.

**Mínimo aceitável:**
- Camada de serviço: testes unitários cobrindo regras de negócio (ex: voto duplicado, sessão expirada).
- Camada web: testes de slice (`@WebMvcTest`) ou integração (`@SpringBootTest`) cobrindo contratos REST.
- Persistência: pelo menos um teste de integração com banco real (Testcontainers ou H2 em modo compatível).

Não há mock para o que pode ser testado de verdade barato.

---

## IV. Persistência durável e explícita

**Regra:** Pautas, sessões e votos sobrevivem a restart. Banco em memória só é aceitável em ambiente de teste; profile `default` aponta para banco persistente.

**Why:** Requisito explícito do README — "as pautas e os votos sejam persistidos e que não sejam perdidos com o restart da aplicação".

---

## V. Contratos REST claros e versionáveis

**Regra:** API segue convenções REST padrão (substantivos plurais, verbos HTTP, status codes corretos). Estratégia de versionamento decidida no `plan.md` da primeira feature e mantida.

**Erros:** respostas de erro têm formato consistente (`{ timestamp, status, error, message, path }` ou equivalente padronizado). Nunca vazar stack traces ao cliente.

---

## VI. Observabilidade mínima

**Regra:** A aplicação loga eventos relevantes (abertura de sessão, voto recebido, voto rejeitado por regra, encerramento de sessão). Logs estruturados (chave-valor) preferíveis a strings concatenadas.

**Não logar:** dados sensíveis (CPF completo em logs vai mascarado).

---

## VII. Commits contam a história

**Regra:** Commits pequenos, mensagens no imperativo, escopo claro. Cada commit deixa o repositório em estado compilável e com testes passando.

**Padrão sugerido:** Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).

---

## VIII. Documentação onde o leitor procura

**Regra:**
- README do projeto: como rodar, como testar, decisões de arquitetura em alto nível.
- Specs em `specs/`: o porquê de cada feature.
- Javadoc apenas em APIs públicas não-óbvias.
- Endpoints REST documentados via OpenAPI (springdoc) — gerado automaticamente.

Comentário em código só quando o "porquê" não é evidente.

---

## Aplicabilidade

Quando uma decisão de design conflitar com este documento, o documento vence. Quando o documento estiver errado, atualize-o no mesmo PR que viola — nunca silenciosamente.
